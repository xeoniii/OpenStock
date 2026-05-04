package com.openstock.app.ui.inventory

import android.os.Bundle
import android.view.*
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.openstock.app.OpenStockApp
import com.openstock.app.R
import com.openstock.app.data.dao.InventoryRaw
import com.openstock.app.data.model.InventoryItem
import com.openstock.app.databinding.FragmentInventoryBinding
import com.openstock.app.ui.MainViewModel
import kotlinx.coroutines.launch

class InventoryFragment : Fragment() {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: InventoryViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var adapter: InventoryAdapter

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents != null) {
            handleBarcodeResult(result.contents)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as OpenStockApp
        viewModel = ViewModelProvider(this, InventoryViewModelFactory(app.repository))[InventoryViewModel::class.java]
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        adapter = InventoryAdapter(
            onEdit = { if (mainViewModel.isSalesMode.value != true) showAddEditDialog(it) },
            onDelete = { if (mainViewModel.isSalesMode.value != true) confirmDelete(it) },
            isSalesModeProvider = { mainViewModel.isSalesMode.value == true }
        )
        binding.recyclerInventory.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerInventory.adapter = adapter

        viewModel.filteredInventory.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            binding.emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }

        mainViewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            viewModel.setSearchQuery(query ?: "")
        }

        mainViewModel.isSalesMode.observe(viewLifecycleOwner) { isSalesMode ->
            val visibility = if (isSalesMode) View.GONE else View.VISIBLE
            binding.fabAddInventory.visibility = visibility
            binding.fabScanInventory.visibility = visibility
            adapter.notifyDataSetChanged()
        }

        binding.fabAddInventory.setOnClickListener { showAddEditDialog(null) }
        binding.fabScanInventory.setOnClickListener { startBarcodeScanner() }
    }

    private fun startBarcodeScanner() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
        options.setPrompt("Scan product barcode to add to inventory")
        options.setCameraId(0)
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(true)
        barcodeLauncher.launch(options)
    }

    private fun handleBarcodeResult(barcode: String) {
        lifecycleScope.launch {
            val product = viewModel.getProductByBarcode(barcode)
            if (product != null) {
                val existing = viewModel.getInventoryByProductId(product.id)
                val inventoryRaw = if (existing != null) {
                    InventoryRaw(
                        existing.id,
                        existing.productId,
                        existing.quantityInStock,
                        existing.lastUpdated,
                        product.name,
                        product.description,
                        product.retailPrice,
                        product.imagePath
                    )
                } else null
                
                showAddEditDialog(inventoryRaw, product.id)
            } else {
                Toast.makeText(requireContext(), "Product not found: $barcode", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showAddEditDialog(existing: InventoryRaw?, preSelectedProductId: Long = -1L) {
        lifecycleScope.launch {
            val products = viewModel.getAllProductsSync()
            if (products.isEmpty()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("No Products")
                    .setMessage("Please add products first before managing inventory.")
                    .setPositiveButton("OK", null)
                    .show()
                return@launch
            }

            val dialog = AddEditInventoryDialog.newInstance(existing, products, preSelectedProductId, viewModel.filteredInventory.value ?: emptyList())
            dialog.onSave = { productId, quantity ->
                viewModel.upsertInventory(productId, quantity)
            }
            dialog.show(childFragmentManager, "AddEditInventory")
        }
    }

    private fun confirmDelete(item: InventoryRaw) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove from Inventory")
            .setMessage("Remove \"${item.productName}\" from inventory?")
            .setPositiveButton("Remove") { _, _ ->
                viewModel.deleteInventory(
                    InventoryItem(id = item.id, productId = item.productId, quantityInStock = item.quantityInStock)
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
