package com.openstock.app.ui.sales

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.openstock.app.OpenStockApp
import com.openstock.app.databinding.FragmentBillsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class BillsFragment : Fragment() {

    private var _binding: FragmentBillsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: BillsAdapter
    private lateinit var salesViewModel: SalesViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBillsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as OpenStockApp
        salesViewModel = ViewModelProvider(requireActivity(), SalesViewModelFactory(app.repository))[SalesViewModel::class.java]

        adapter = BillsAdapter(
            onOpen = { openFile(it) },
            onShare = { shareFile(it) },
            onDelete = { confirmDelete(it) }
        )
        binding.recyclerBills.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerBills.adapter = adapter

        refreshBills()

        binding.btnGenerateTop.setOnClickListener {
            generateNewBills()
        }
    }

    private fun generateNewBills() {
        viewLifecycleOwner.lifecycleScope.launch {
            val completedGroups = salesViewModel.getCompletedUnverifiedSaleGroups()
            if (completedGroups.isEmpty()) {
                Toast.makeText(requireContext(), "No new completed sales to generate bills for", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val count = BillGenerator.generateBills(requireContext(), completedGroups, salesViewModel)
            if (count > 0) {
                Toast.makeText(requireContext(), "Generated $count new bills", Toast.LENGTH_SHORT).show()
                refreshBills()
            } else {
                Toast.makeText(requireContext(), "No new bills generated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun refreshBills() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val billsFolder = File(downloadsDir, "OpenStock_Bills")
            
            val files = if (billsFolder.exists() && billsFolder.isDirectory) {
                billsFolder.listFiles { file -> file.extension.lowercase() == "pdf" }
                    ?.sortedByDescending { it.lastModified() } ?: emptyList()
            } else {
                emptyList()
            }

            withContext(Dispatchers.Main) {
                adapter.submitList(files)
                binding.emptyState.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun openFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No app found to open PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share Bill"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error sharing file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDelete(file: File) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Bill")
            .setMessage("Are you sure you want to delete ${file.name}?")
            .setPositiveButton("Delete") { _, _ ->
                if (file.delete()) {
                    Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                    refreshBills()
                } else {
                    Toast.makeText(requireContext(), "Failed to delete", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
