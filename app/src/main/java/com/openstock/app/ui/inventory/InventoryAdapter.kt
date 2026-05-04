package com.openstock.app.ui.inventory

import android.view.*
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.openstock.app.R
import com.openstock.app.data.dao.InventoryRaw
import com.openstock.app.databinding.ItemInventoryBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class InventoryAdapter(
    private val onEdit: (InventoryRaw) -> Unit,
    private val onDelete: (InventoryRaw) -> Unit,
    private val isSalesModeProvider: () -> Boolean = { false },
    private val onItemClick: ((InventoryRaw) -> Unit)? = null
) : ListAdapter<InventoryRaw, InventoryAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemInventoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: InventoryRaw) {
            val isSalesMode = isSalesModeProvider()
            binding.tvProductName.text = item.productName
            
            if (isSalesMode) {
                binding.tvQuantity.text = "In Stock: ${formatQty(item.quantityInStock)}"
                binding.tvQuantity.textSize = 18f
                binding.tvPrice.text = "Price: AED %.2f".format(item.retailPrice)
                binding.tvLastUpdated.visibility = View.GONE
            } else {
                binding.tvQuantity.text = formatQty(item.quantityInStock)
                binding.tvQuantity.textSize = 22f
                binding.tvPrice.text = "AED %.2f".format(item.retailPrice)
                binding.tvLastUpdated.visibility = View.VISIBLE
                binding.tvLastUpdated.text = "Updated: ${dateFormatter.format(Date(item.lastUpdated))}"
            }

            if (item.imagePath != null) {
                val file = File(item.imagePath)
                if (file.exists()) {
                    Glide.with(binding.root.context)
                        .load(file)
                        .centerCrop()
                        .into(binding.ivProduct)
                } else {
                    binding.ivProduct.setImageResource(R.drawable.ic_products)
                }
            } else {
                binding.ivProduct.setImageResource(R.drawable.ic_products)
            }
            
            // Remove three dots in sales mode
            binding.btnMore.visibility = if (isSalesMode) View.GONE else View.VISIBLE
            binding.btnMore.setOnClickListener { showMenu(it, item) }
            
            binding.root.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }

        private fun formatQty(qty: Double) = if (qty % 1.0 == 0.0) qty.toInt().toString() else qty.toString()
    }

    private fun showMenu(view: View, item: InventoryRaw) {
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
        ViewHolder(ItemInventoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val dateFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        val DIFF = object : DiffUtil.ItemCallback<InventoryRaw>() {
            override fun areItemsTheSame(a: InventoryRaw, b: InventoryRaw) = a.id == b.id
            override fun areContentsTheSame(a: InventoryRaw, b: InventoryRaw) = a == b
        }
    }
}
