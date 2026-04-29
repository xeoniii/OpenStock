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

class GalleryAdapter : ListAdapter<InventoryRaw, GalleryAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemGalleryProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: InventoryRaw) {
            binding.tvProductName.text = item.productName
            binding.tvPrice.text = "AED %.2f".format(item.retailPrice)
            binding.tvStock.text = "In Stock: ${formatQty(item.quantityInStock)}"

            if (item.imagePath != null) {
                val file = File(item.imagePath)
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
        }

        private fun formatQty(qty: Double) = if (qty % 1.0 == 0.0) qty.toInt().toString() else qty.toString()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemGalleryProductBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<InventoryRaw>() {
            override fun areItemsTheSame(a: InventoryRaw, b: InventoryRaw) = a.id == b.id
            override fun areContentsTheSame(a: InventoryRaw, b: InventoryRaw) = a == b
        }
    }
}
