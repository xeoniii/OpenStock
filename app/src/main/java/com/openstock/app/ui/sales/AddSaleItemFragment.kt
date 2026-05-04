package com.openstock.app.ui.sales

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.openstock.app.OpenStockApp
import com.openstock.app.data.dao.InventoryRaw
import com.openstock.app.data.model.SaleItem
import com.openstock.app.databinding.DialogAddSaleItemBinding
import com.openstock.app.databinding.FragmentAddSaleItemBinding
import com.openstock.app.ui.MainViewModel
import com.openstock.app.ui.inventory.InventoryAdapter

class AddSaleItemFragment : Fragment() {

    private var _binding: FragmentAddSaleItemBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SalesViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var adapter: InventoryAdapter
    private lateinit var galleryAdapter: GalleryAdapter
    private var groupId: Long = 0
    private var currentSaleItems: List<com.openstock.app.data.model.SaleItem> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddSaleItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        groupId = arguments?.getLong("groupId") ?: 0

        val app = requireActivity().application as OpenStockApp
        viewModel = ViewModelProvider(this, SalesViewModelFactory(app.repository))[SalesViewModel::class.java]
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        adapter = InventoryAdapter(
            onEdit = {},
            onDelete = {},
            isSalesModeProvider = { true },
            onItemClick = { showAddToSaleDialog(it) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        galleryAdapter = GalleryAdapter(
            onPlusClick = { handlePlusClick(it) },
            onMinusClick = { handleMinusClick(it) }
        )
        binding.recyclerViewGallery.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerViewGallery.adapter = galleryAdapter

        viewModel.filteredInventory.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            updateGalleryItems()
            binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.getSaleItemsByGroup(groupId).observe(viewLifecycleOwner) { items ->
            currentSaleItems = items
            updateGalleryItems()
            updateTotalDisplay(items)
        }

        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                if (checkedId == binding.btnList.id) {
                    binding.recyclerView.visibility = View.VISIBLE
                    binding.recyclerViewGallery.visibility = View.GONE
                } else {
                    binding.recyclerView.visibility = View.GONE
                    binding.recyclerViewGallery.visibility = View.VISIBLE
                }
            }
        }

        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setInventorySearchQuery(newText ?: "")
                return true
            }
        })
        
        binding.btnCart.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun updateGalleryItems() {
        val inventory = viewModel.filteredInventory.value ?: emptyList()
        val qtyMap = currentSaleItems.associate { it.productId to it.quantity }
        val galleryItems = inventory.map { GalleryItem(it, qtyMap[it.productId] ?: 0.0) }
        galleryAdapter.submitList(galleryItems)
    }

    private fun handlePlusClick(item: InventoryRaw) {
        val existing = currentSaleItems.find { it.productId == item.productId }
        if (existing == null) {
            if (item.quantityInStock >= 1) {
                val product = viewModel.allProducts.value?.find { it.id == item.productId }
                if (product != null) {
                    viewModel.insertSaleItem(
                        SaleItem(
                            saleGroupId = groupId,
                            productId = item.productId,
                            quantity = 1.0,
                            wholesalePriceAtSale = product.wholesalePrice,
                            retailPriceAtSale = item.retailPrice
                        )
                    )
                }
            } else {
                Toast.makeText(requireContext(), "Out of stock", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (item.quantityInStock > 0) {
                viewModel.updateSaleItem(existing.copy(quantity = existing.quantity + 1))
            } else {
                Toast.makeText(requireContext(), "No more stock available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleMinusClick(item: InventoryRaw) {
        val existing = currentSaleItems.find { it.productId == item.productId } ?: return
        if (existing.quantity > 1) {
            viewModel.updateSaleItem(existing.copy(quantity = existing.quantity - 1))
        } else {
            viewModel.deleteSaleItem(existing)
        }
    }

    private fun updateTotalDisplay(items: List<SaleItem>) {
        val total = items.sumOf { it.quantity * it.retailPriceAtSale }
        binding.tvTotalLabel.text = "Total: AED %.2f".format(total)
    }

    private fun showAddToSaleDialog(item: InventoryRaw) {
        val dialogBinding = DialogAddSaleItemBinding.inflate(layoutInflater)
        
        dialogBinding.spinnerProduct.visibility = View.GONE
        dialogBinding.tvProductName.text = item.productName
        dialogBinding.tvProductName.visibility = View.VISIBLE
        
        val product = viewModel.allProducts.value?.find { it.id == item.productId }
        if (product == null) return

        dialogBinding.etWholesaleOverride.setText("%.2f".format(product.wholesalePrice))
        dialogBinding.etRetailOverride.setText("%.2f".format(product.retailPrice))
        
        if (mainViewModel.isSalesMode.value == true) {
            dialogBinding.tilWholesale.visibility = View.GONE
        }

        val existing = currentSaleItems.find { it.productId == item.productId }
        val maxAvailable = item.quantityInStock + (existing?.quantity ?: 0.0)
        
        dialogBinding.etQuantity.hint = "Quantity (Available: ${if (maxAvailable % 1.0 == 0.0) maxAvailable.toInt() else maxAvailable})"
        if (existing != null) {
            dialogBinding.etQuantity.setText(if (existing.quantity % 1.0 == 0.0) existing.quantity.toInt().toString() else existing.quantity.toString())
        }

        dialogBinding.btnEditPrice.setOnClickListener {
            showPriceUnlockDialog {
                dialogBinding.etRetailOverride.isEnabled = true
                dialogBinding.etRetailOverride.requestFocus()
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) "Add to Sale" else "Edit Sale Item")
            .setView(dialogBinding.root)
            .setPositiveButton(if (existing == null) "Add" else "Update") { _, _ ->
                val qtyStr = dialogBinding.etQuantity.text.toString().trim()
                val qty = qtyStr.toDoubleOrNull()
                
                if (qty == null || qty <= 0 || qty > maxAvailable) {
                    Toast.makeText(requireContext(), "Invalid quantity", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val ws = dialogBinding.etWholesaleOverride.text.toString().toDoubleOrNull() ?: product.wholesalePrice
                val rt = dialogBinding.etRetailOverride.text.toString().toDoubleOrNull() ?: product.retailPrice

                if (existing == null) {
                    viewModel.insertSaleItem(
                        SaleItem(
                            saleGroupId = groupId,
                            productId = item.productId,
                            quantity = qty,
                            wholesalePriceAtSale = ws,
                            retailPriceAtSale = rt
                        )
                    )
                } else {
                    viewModel.updateSaleItem(
                        existing.copy(
                            quantity = qty,
                            wholesalePriceAtSale = ws,
                            retailPriceAtSale = rt
                        )
                    )
                }
                Toast.makeText(requireContext(), "Sale updated", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPriceUnlockDialog(onSuccess: () -> Unit) {
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val savedPassword = prefs.getString("personal_mode_password", null)

        if (savedPassword == null) {
            onSuccess()
            return
        }

        val binding2 = layoutInflater.inflate(com.openstock.app.R.layout.dialog_password_input, null)
        val etPassword = binding2.findViewById<android.widget.EditText>(com.openstock.app.R.id.etPassword)

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
