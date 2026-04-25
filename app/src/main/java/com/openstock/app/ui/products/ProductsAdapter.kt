package com.openstock.app.ui.products

import android.view.*
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.openstock.app.R
import com.openstock.app.data.model.Product
import com.openstock.app.databinding.ItemProductBinding

class ProductsAdapter(
    private val onEdit: (Product) -> Unit,
    private val onDelete: (Product) -> Unit
) : ListAdapter<Product, ProductsAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            binding.tvProductName.text = product.name
            binding.tvBarcode.text = if (product.barcode.isNotEmpty()) "📦 ${product.barcode}" else "No barcode"
            binding.tvWholesale.text = "Cost: AED %.2f".format(product.wholesalePrice)
            binding.tvRetail.text = "Price: AED %.2f".format(product.retailPrice)
            binding.tvUnit.text = product.unit
            val profit = product.retailPrice - product.wholesalePrice
            binding.tvProfit.text = "Profit: AED %.2f".format(profit)
            binding.btnMore.setOnClickListener { showMenu(it, product) }
        }
    }

    private fun showMenu(view: View, product: Product) {
        PopupMenu(view.context, view).apply {
            inflate(R.menu.menu_item_actions)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_edit -> { onEdit(product); true }
                    R.id.action_delete -> { onDelete(product); true }
                    else -> false
                }
            }
            show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(a: Product, b: Product) = a.id == b.id
            override fun areContentsTheSame(a: Product, b: Product) = a == b
        }
    }
}
