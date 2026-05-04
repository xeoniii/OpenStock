package com.openstock.app.ui.sales

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.openstock.app.R
import com.openstock.app.data.dao.InventoryRaw
import com.openstock.app.databinding.ItemGalleryProductBinding
import java.io.File

data class GalleryItem(
    val inventory: InventoryRaw,
    val selectedQuantity: Double
)

class GalleryAdapter(
    private val onPlusClick: (InventoryRaw) -> Unit,
    private val onMinusClick: (InventoryRaw) -> Unit
) : ListAdapter<GalleryItem, GalleryAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemGalleryProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GalleryItem) {
            val raw = item.inventory
            binding.tvProductName.text = raw.productName
            binding.tvProductDescription.text = raw.productDescription
            binding.tvPrice.text = "AED %.2f".format(raw.retailPrice)
            binding.tvStock.text = "In Stock: ${formatQty(raw.quantityInStock)}"
            
            val selectedQty = item.selectedQuantity
            binding.tvSelectedQuantity.text = formatQty(selectedQty)
            binding.layoutQuantity.alpha = if (selectedQty > 0) 1.0f else 0.5f

            if (raw.imagePath != null) {
                val file = File(raw.imagePath)
                if (file.exists()) {
                    Glide.with(binding.root.context)
                        .load(file)
                        .centerCrop()
                        .into(binding.ivProduct)
                    binding.ivProduct.alpha = 1.0f
                } else {
                    binding.ivProduct.setImageResource(R.drawable.ic_products)
                    binding.ivProduct.alpha = 0.3f
                }
            } else {
                binding.ivProduct.setImageResource(R.drawable.ic_products)
                binding.ivProduct.alpha = 0.3f
            }

            binding.btnPlus.setOnClickListener { onPlusClick(raw) }
            binding.btnMinus.setOnClickListener { onMinusClick(raw) }
        }

        private fun formatQty(qty: Double) = if (qty % 1.0 == 0.0) qty.toInt().toString() else qty.toString()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemGalleryProductBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<GalleryItem>() {
            override fun areItemsTheSame(a: GalleryItem, b: GalleryItem) = a.inventory.id == b.inventory.id
            override fun areContentsTheSame(a: GalleryItem, b: GalleryItem) = a == b
        }
    }
}
