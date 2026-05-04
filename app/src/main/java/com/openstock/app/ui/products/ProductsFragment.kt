package com.openstock.app.ui.products

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
import com.openstock.app.data.model.Product
import com.openstock.app.databinding.FragmentProductsBinding
import com.openstock.app.ui.MainViewModel
import kotlinx.coroutines.launch

class ProductsFragment : Fragment() {

    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProductsViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var adapter: ProductsAdapter

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        val barcode = result.contents
        if (barcode != null) {
            lifecycleScope.launch {
                val existing = viewModel.getProductByBarcode(barcode)
                if (existing != null) {
                    showAddEditDialog(existing)
                } else {
                    showAddEditDialog(null, barcode)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as OpenStockApp
        viewModel = ViewModelProvider(this, ProductsViewModelFactory(app.repository))[ProductsViewModel::class.java]
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        adapter = ProductsAdapter(
            onEdit = { showAddEditDialog(it) },
            onDelete = { confirmDelete(it) }
        )
        binding.recyclerProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerProducts.adapter = adapter

        viewModel.filteredProducts.observe(viewLifecycleOwner) { products ->
            adapter.submitList(products)
            binding.emptyState.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
        }

        mainViewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            viewModel.setSearchQuery(query ?: "")
        }

        binding.fabAddProduct.setOnClickListener { showAddEditDialog(null) }
        binding.fabScanBarcode.setOnClickListener { startBarcodeScanner() }
    }

    private fun startBarcodeScanner() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
        options.setPrompt("Scan product barcode")
        options.setCameraId(0)
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(true)
        options.setOrientationLocked(false)
        barcodeLauncher.launch(options)
    }

    private fun showAddEditDialog(product: Product?, barcodeValue: String = "") {
        val dialog = AddEditProductDialog.newInstance(product, barcodeValue)
        dialog.barcodeExistHandler = { barcode ->
            viewModel.getProductByBarcode(barcode)
        }
        dialog.validator = { p ->
            if (p.barcode.isNotEmpty()) {
                val existing = viewModel.getProductByBarcode(p.barcode)
                if (existing != null && (product == null || existing.id != product.id)) {
                    "Barcode already exists for product: ${existing.name}"
                } else null
            } else null
        }
        dialog.onSave = { newProduct ->
            if (product == null) {
                viewModel.insertProduct(newProduct)
            } else {
                viewModel.updateProduct(newProduct.copy(id = product.id))
            }
        }
        dialog.show(childFragmentManager, "AddEditProduct")
    }

    private fun confirmDelete(product: Product) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete \"${product.name}\"?")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteProduct(product) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
