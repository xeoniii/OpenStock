package com.openstock.app.ui.sales

import android.view.*
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.openstock.app.R
import com.openstock.app.data.dao.SaleItemRaw
import com.openstock.app.data.model.SaleItem
import com.openstock.app.databinding.ItemSaleItemBinding

class SaleItemAdapter(
    private val onEdit: (SaleItemRaw) -> Unit,
    private val onDelete: (SaleItemRaw) -> Unit,
    private val isSalesModeProvider: () -> Boolean = { false }
) : ListAdapter<SaleItemRaw, SaleItemAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemSaleItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SaleItemRaw) {
            val isSalesMode = isSalesModeProvider()
            binding.tvProductName.text = item.productName

            val qty = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
            binding.tvQuantity.text = "${qty}×"
            
            if (isSalesMode) {
                binding.tvWholesaleTotal.visibility = View.GONE
                binding.tvProfit.visibility = View.GONE
                binding.tvRetailTotal.text = "Total: AED %.2f".format(item.quantity * item.retailPriceAtSale)
                binding.tvUnitPrices.text = "@ AED %.2f".format(item.retailPriceAtSale)
            } else {
                binding.tvWholesaleTotal.visibility = View.VISIBLE
                binding.tvProfit.visibility = View.VISIBLE
                binding.tvWholesaleTotal.text = "Cost: AED %.2f".format(item.quantity * item.wholesalePriceAtSale)
                binding.tvRetailTotal.text = "Revenue: AED %.2f".format(item.quantity * item.retailPriceAtSale)
                val profit = item.quantity * (item.retailPriceAtSale - item.wholesalePriceAtSale)
                binding.tvProfit.text = "Profit: AED %.2f".format(profit)
                binding.tvUnitPrices.text = "@ AED %.2f / AED %.2f".format(item.wholesalePriceAtSale, item.retailPriceAtSale)
            }

            binding.btnMore.setOnClickListener { showMenu(it, item) }
        }
    }

    private fun showMenu(view: View, item: SaleItemRaw) {
        PopupMenu(view.context, view).apply {
            inflate(R.menu.menu_item_actions)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_edit -> { onEdit(item); true }
                    R.id.action_delete -> { onDelete(item); true }
                    else -> false
                }
            }
            show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemSaleItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<SaleItemRaw>() {
            override fun areItemsTheSame(a: SaleItemRaw, b: SaleItemRaw) = a.id == b.id
            override fun areContentsTheSame(a: SaleItemRaw, b: SaleItemRaw) = a == b
        }
    }
}
