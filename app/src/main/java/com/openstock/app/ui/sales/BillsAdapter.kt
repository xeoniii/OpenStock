package com.openstock.app.ui.sales

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.openstock.app.databinding.ItemBillBinding
import java.io.File

class BillsAdapter(
    private val onOpen: (File) -> Unit,
    private val onShare: (File) -> Unit,
    private val onDelete: (File) -> Unit
) : ListAdapter<File, BillsAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemBillBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(file: File) {
            binding.tvFileName.text = file.name
            binding.tvFileSize.text = "${file.length() / 1024} KB"
            binding.root.setOnClickListener { onOpen(file) }
            binding.btnShare.setOnClickListener { onShare(file) }
            binding.btnDelete.setOnClickListener { onDelete(file) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemBillBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<File>() {
            override fun areItemsTheSame(oldItem: File, newItem: File) = oldItem.absolutePath == newItem.absolutePath
            override fun areContentsTheSame(oldItem: File, newItem: File) = oldItem.lastModified() == newItem.lastModified()
        }
    }
}
