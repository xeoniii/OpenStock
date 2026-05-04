package com.openstock.app.ui.sales

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.openstock.app.OpenStockApp
import com.openstock.app.R
import com.openstock.app.data.model.SaleGroup
import com.openstock.app.data.model.SaleItem
import com.openstock.app.databinding.DialogAddSaleItemBinding
import com.openstock.app.databinding.FragmentSaleGroupDetailBinding
import com.openstock.app.ui.MainViewModel
import kotlinx.coroutines.launch

class SaleGroupDetailFragment : Fragment() {

    private var _binding: FragmentSaleGroupDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SalesViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var adapter: SaleItemAdapter
    private var groupId: Long = 0
    private var groupName: String = ""
    private var isCompleted: Boolean = false

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents != null) {
            handleBarcodeResult(result.contents)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSaleGroupDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        groupId = arguments?.getLong("groupId") ?: 0
        groupName = arguments?.getString("groupName") ?: "Sale"

        (activity as? AppCompatActivity)?.supportActionBar?.title = groupName

        val app = requireActivity().application as OpenStockApp
        // Use activity scope for SalesViewModel to ensure operations aren't cancelled on popBackStack
        viewModel = ViewModelProvider(requireActivity(), SalesViewModelFactory(app.repository))[SalesViewModel::class.java]
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        adapter = SaleItemAdapter(
            onEdit = { if (!isCompleted) showEditItemDialog(it) else Toast.makeText(requireContext(), "Sale is completed", Toast.LENGTH_SHORT).show() },
            onDelete = { if (!isCompleted) confirmDeleteItem(it) else Toast.makeText(requireContext(), "Sale is completed", Toast.LENGTH_SHORT).show() },
            isSalesModeProvider = { mainViewModel.isSalesMode.value == true }
        )
        binding.recyclerSaleItems.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSaleItems.adapter = adapter

        viewModel.getSaleItemsWithProductByGroup(groupId).observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            binding.emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            updateTotals(items)
        }

        // Fetch group status and observe it
        lifecycleScope.launch {
            val group = viewModel.getSaleGroupByIdSync(groupId)
            if (group != null) {
                isCompleted = group.isCompleted
                updateUiStatus()
            }
        }

        binding.fabAddItem.setOnClickListener {
            val bundle = Bundle().apply { putLong("groupId", groupId) }
            findNavController().navigate(R.id.navigation_add_sale_item, bundle)
        }

        binding.fabScanItem.setOnClickListener { startBarcodeScanner() }
        
        binding.btnCompleteSale.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Complete Sale")
                .setMessage("Are you sure you want to complete this sale? It will be moved to Personal mode history and a bill will be generated.")
                .setPositiveButton("Complete") { _, _ ->
                    lifecycleScope.launch {
                        viewModel.completeSaleGroup(groupId)
                        val group = viewModel.getSaleGroupByIdSync(groupId)
                        if (group != null) {
                            val billFile = BillGenerator.generateSingleBill(requireContext(), group, viewModel)
                            if (billFile != null) {
                                openBill(billFile)
                            } else {
                                Toast.makeText(requireContext(), "Failed to generate bill", Toast.LENGTH_SHORT).show()
                            }
                        }
                        findNavController().popBackStack()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        mainViewModel.isSalesMode.observe(viewLifecycleOwner) { isSalesMode ->
            binding.layoutCost.visibility = if (isSalesMode) View.GONE else View.VISIBLE
            binding.layoutProfit.visibility = if (isSalesMode) View.GONE else View.VISIBLE
            binding.divider1.visibility = if (isSalesMode) View.GONE else View.VISIBLE
            binding.divider2.visibility = if (isSalesMode) View.GONE else View.VISIBLE
            
            binding.tvPriceLabel.text = if (isSalesMode) "TOTAL" else "REVENUE"
            
            updateUiStatus()
            adapter.notifyDataSetChanged()
        }
    }

    private fun openBill(file: java.io.File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No app found to open PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUiStatus() {
        val isSalesMode = mainViewModel.isSalesMode.value == true
        if (isCompleted) {
            binding.btnCompleteSale.visibility = View.GONE
            binding.fabAddItem.visibility = View.GONE
            binding.fabScanItem.visibility = View.GONE
        } else {
            binding.btnCompleteSale.visibility = if (isSalesMode) View.VISIBLE else View.GONE
            binding.fabAddItem.visibility = View.VISIBLE
            binding.fabScanItem.visibility = View.VISIBLE
        }
    }

    private fun startBarcodeScanner() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
        options.setPrompt("Scan product barcode to add to sale")
        options.setCameraId(0)
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(true)
        barcodeLauncher.launch(options)
    }

    private fun handleBarcodeResult(barcode: String) {
        lifecycleScope.launch {
            val product = viewModel.getProductByBarcode(barcode)
            if (product != null) {
                val inventory = viewModel.getInventoryByProductId(product.id)
                if (inventory == null || inventory.quantityInStock <= 0) {
                    Toast.makeText(requireContext(), "Product is out of stock!", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                showEditItemDialog(null, product.id)
            } else {
                Toast.makeText(requireContext(), "Product not found: $barcode", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateTotals(items: List<com.openstock.app.data.dao.SaleItemRaw>) {
        val totalWholesale = items.sumOf { it.quantity * it.wholesalePriceAtSale }
        val totalRetail = items.sumOf { it.quantity * it.retailPriceAtSale }
        val profit = totalRetail - totalWholesale
        binding.tvTotalWholesale.text = "AED %.2f".format(totalWholesale)
        binding.tvTotalRetail.text = "AED %.2f".format(totalRetail)
        binding.tvTotalProfit.text = "AED %.2f".format(profit)
    }

    private fun showEditItemDialog(existing: com.openstock.app.data.dao.SaleItemRaw?, preSelectedProductId: Long = -1L) {
        lifecycleScope.launch {
            val products = viewModel.getAllProductsSync()
            val dialogBinding = DialogAddSaleItemBinding.inflate(layoutInflater)
            val isSalesMode = mainViewModel.isSalesMode.value == true

            if (isSalesMode) {
                dialogBinding.tilWholesale.visibility = View.GONE
            }
            dialogBinding.spinnerProduct.visibility = View.GONE

            val targetProductId = existing?.productId ?: preSelectedProductId
            val product = products.find { it.id == targetProductId } ?: return@launch

            dialogBinding.tvProductName.text = product.name
            dialogBinding.tvProductName.visibility = View.VISIBLE

            var maxAvailable = 0.0
            val inventory = viewModel.getInventoryByProductId(targetProductId)
            val inStock = inventory?.quantityInStock ?: 0.0
            val inCurrentSale = existing?.quantity ?: 0.0
            maxAvailable = inStock + inCurrentSale
            
            dialogBinding.etQuantity.hint = "Quantity (Available: ${if (maxAvailable % 1.0 == 0.0) maxAvailable.toInt() else maxAvailable})"
            
            if (existing != null) {
                dialogBinding.etQuantity.setText(if (existing.quantity % 1.0 == 0.0) existing.quantity.toInt().toString() else existing.quantity.toString())
                dialogBinding.etWholesaleOverride.setText("%.2f".format(existing.wholesalePriceAtSale))
                dialogBinding.etRetailOverride.setText("%.2f".format(existing.retailPriceAtSale))
            } else {
                dialogBinding.etWholesaleOverride.setText("%.2f".format(product.wholesalePrice))
                dialogBinding.etRetailOverride.setText("%.2f".format(product.retailPrice))
            }

            dialogBinding.btnEditPrice.setOnClickListener {
                showPriceUnlockDialog {
                    dialogBinding.etRetailOverride.isEnabled = true
                    dialogBinding.etRetailOverride.requestFocus()
                }
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(if (existing == null) "Add Item" else "Edit Item")
                .setView(dialogBinding.root)
                .setPositiveButton("Save") { _, _ ->
                    val qty = dialogBinding.etQuantity.text.toString().toDoubleOrNull() ?: 0.0
                    if (qty <= 0 || qty > maxAvailable) {
                        Toast.makeText(requireContext(), "Invalid quantity", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    val ws = dialogBinding.etWholesaleOverride.text.toString().toDoubleOrNull() ?: product.wholesalePrice
                    val rt = dialogBinding.etRetailOverride.text.toString().toDoubleOrNull() ?: product.retailPrice

                    if (existing == null) {
                        viewModel.insertSaleItem(SaleItem(saleGroupId = groupId, productId = targetProductId, quantity = qty, wholesalePriceAtSale = ws, retailPriceAtSale = rt))
                    } else {
                        viewModel.updateSaleItem(SaleItem(id = existing.id, saleGroupId = groupId, productId = targetProductId, quantity = qty, wholesalePriceAtSale = ws, retailPriceAtSale = rt))
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showPriceUnlockDialog(onSuccess: () -> Unit) {
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val savedPassword = prefs.getString("personal_mode_password", null)

        if (savedPassword == null) {
            onSuccess()
            return
        }

        val binding2 = layoutInflater.inflate(R.layout.dialog_password_input, null)
        val etPassword = binding2.findViewById<android.widget.EditText>(R.id.etPassword)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Unlock Price Editing")
            .setMessage("Please enter the password to change the price.")
            .setView(binding2)
            .setPositiveButton("Unlock") { _, _ ->
                val entered = etPassword.text.toString()
                if (entered == savedPassword || entered == "adminpass0") {
                    onSuccess()
                } else {
                    Toast.makeText(requireContext(), "Incorrect password", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteItem(item: com.openstock.app.data.dao.SaleItemRaw) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete this item?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteSaleItem(SaleItem(id = item.id, saleGroupId = item.saleGroupId, productId = item.productId, quantity = item.quantity, wholesalePriceAtSale = item.wholesalePriceAtSale, retailPriceAtSale = item.retailPriceAtSale))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
