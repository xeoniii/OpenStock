package com.openstock.app.ui.sales

import android.view.*
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.openstock.app.R
import com.openstock.app.data.model.SaleGroupSummary
import com.openstock.app.databinding.ItemSaleGroupBinding
import com.openstock.app.databinding.ItemSaleGroupHeaderBinding
import java.text.SimpleDateFormat
import java.util.*

import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SaleGroupAdapter(
    private val onView: (SaleGroupSummary) -> Unit,
    private val onEdit: (SaleGroupSummary) -> Unit,
    private val onDelete: (SaleGroupSummary) -> Unit,
    private val onVerify: (SaleGroupSummary) -> Unit,
    private val onUpdateTotal: (SaleGroupSummary, Double) -> Unit,
    private val isSalesModeProvider: () -> Boolean = { false }
) : ListAdapter<SaleGroupItem, RecyclerView.ViewHolder>(DIFF) {

    inner class HeaderViewHolder(private val binding: ItemSaleGroupHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: SaleGroupItem.Header) {
            val isSalesMode = isSalesModeProvider()
            if (isSalesMode) {
                binding.root.layoutParams = RecyclerView.LayoutParams(0, 0)
                binding.root.visibility = View.GONE
            } else {
                binding.root.layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                binding.root.visibility = View.VISIBLE
                binding.tvHeaderDate.text = header.date
                binding.tvDailySummary.text = "Cost: AED %.2f | Revenue: AED %.2f | Profit: AED %.2f"
                    .format(header.totalWholesale, header.totalRetail, header.totalRetail - header.totalWholesale)
            }
        }
    }

    inner class SummaryViewHolder(private val binding: ItemSaleGroupBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(summary: SaleGroupSummary) {
            val isSalesMode = isSalesModeProvider()
            binding.tvGroupName.text = if (summary.isVerified) "${summary.name} (Verified)" else summary.name
            binding.tvDate.text = dateFormatter.format(Date(summary.createdAt))

            val totalWholesale = summary.totalWholesale
            val totalRetail = summary.finalTotalRetail
            val profit = totalRetail - totalWholesale
            binding.tvItemCount.text = "${summary.itemCount} item(s)"
            
            if (isSalesMode) {
                binding.tvGroupWholesale.text = "Total: AED %.2f".format(totalRetail)
                binding.tvGroupRetail.visibility = View.GONE
                binding.tvGroupProfit.visibility = View.GONE
            } else {
                binding.tvGroupWholesale.text = "Cost: AED %.2f".format(totalWholesale)
                binding.tvGroupRetail.text = "Revenue: AED %.2f".format(totalRetail)
                binding.tvGroupProfit.text = "Profit: AED %.2f".format(profit)
                binding.tvGroupWholesale.visibility = View.VISIBLE
                binding.tvGroupRetail.visibility = View.VISIBLE
                binding.tvGroupProfit.visibility = View.VISIBLE
            }

            binding.root.setOnClickListener { onView(summary) }
            binding.btnMore.setOnClickListener { showMenu(it, summary) }
        }
    }

    private fun showMenu(view: View, summary: SaleGroupSummary) {
        PopupMenu(view.context, view).apply {
            val isSalesMode = isSalesModeProvider()
            inflate(R.menu.menu_sale_group_actions)
            
            val actionVerify = menu.findItem(R.id.action_verify)
            val actionUpdateTotal = menu.add(Menu.NONE, 999, Menu.NONE, "Change Total Price")
            
            if (isSalesMode) {
                menu.findItem(R.id.action_rename)?.title = "Rename Sale"
                menu.findItem(R.id.action_delete)?.title = "Delete Sale"
                actionVerify?.isVisible = false
                actionUpdateTotal.isVisible = false
            } else {
                actionVerify?.isVisible = summary.isCompleted && !summary.isVerified
                actionUpdateTotal.isVisible = summary.isCompleted && !summary.isVerified
            }
            
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_view -> { onView(summary); true }
                    R.id.action_rename -> { onEdit(summary); true }
                    R.id.action_delete -> { onDelete(summary); true }
                    R.id.action_verify -> { onVerify(summary); true }
                    999 -> { showUpdateTotalDialog(view.context, summary); true }
                    else -> false
                }
            }
            show()
        }
    }

    private fun showUpdateTotalDialog(context: android.content.Context, summary: SaleGroupSummary) {
        val inflater = LayoutInflater.from(context)
        val binding = com.openstock.app.databinding.DialogInputNameBinding.inflate(inflater)
        binding.etName.apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText("%.2f".format(summary.finalTotalRetail))
        }
        binding.tilName.hint = "Total Price (AED)"

        MaterialAlertDialogBuilder(context)
            .setTitle("Change Total Price")
            .setMessage("Override the calculated total revenue for this sale.")
            .setView(binding.root)
            .setPositiveButton("Update") { _, _ ->
                val newVal = binding.etName.text.toString().toDoubleOrNull()
                if (newVal != null) onUpdateTotal(summary, newVal)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SaleGroupItem.Header -> TYPE_HEADER
            is SaleGroupItem.Summary -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(ItemSaleGroupHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            SummaryViewHolder(ItemSaleGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is SaleGroupItem.Header -> (holder as HeaderViewHolder).bind(item)
            is SaleGroupItem.Summary -> (holder as SummaryViewHolder).bind(item.summary)
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
        private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val DIFF = object : DiffUtil.ItemCallback<SaleGroupItem>() {
            override fun areItemsTheSame(a: SaleGroupItem, b: SaleGroupItem): Boolean {
                if (a is SaleGroupItem.Header && b is SaleGroupItem.Header) return a.date == b.date
                if (a is SaleGroupItem.Summary && b is SaleGroupItem.Summary) return a.summary.id == b.summary.id
                return false
            }
            override fun areContentsTheSame(a: SaleGroupItem, b: SaleGroupItem): Boolean {
                return a == b
            }
        }
    }
}
