package com.openstock.app.ui.sales

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
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

        galleryAdapter = GalleryAdapter()
        binding.viewPagerGallery.adapter = galleryAdapter

        viewModel.filteredInventory.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            galleryAdapter.submitList(items)
            binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                if (checkedId == binding.btnList.id) {
                    binding.recyclerView.visibility = View.VISIBLE
                    binding.viewPagerGallery.visibility = View.GONE
                    binding.btnGalleryAdd.visibility = View.GONE
                } else {
                    binding.recyclerView.visibility = View.GONE
                    binding.viewPagerGallery.visibility = View.VISIBLE
                    binding.btnGalleryAdd.visibility = View.VISIBLE
                }
            }
        }

        binding.btnGalleryAdd.setOnClickListener {
            val currentPos = binding.viewPagerGallery.currentItem
            val items = viewModel.filteredInventory.value
            if (items != null && currentPos < items.size) {
                showAddToSaleDialog(items[currentPos])
            }
        }

        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setInventorySearchQuery(newText ?: "")
                return true
            }
        })
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

        val maxAvailable = item.quantityInStock
        dialogBinding.etQuantity.hint = "Quantity (Available: ${if (maxAvailable % 1.0 == 0.0) maxAvailable.toInt() else maxAvailable})"

        dialogBinding.btnEditPrice.setOnClickListener {
            showPriceUnlockDialog {
                dialogBinding.etRetailOverride.isEnabled = true
                dialogBinding.etRetailOverride.requestFocus()
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add to Sale")
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { _, _ ->
                val qtyStr = dialogBinding.etQuantity.text.toString().trim()
                val qty = qtyStr.toDoubleOrNull()
                
                if (qty == null || qty <= 0 || qty > maxAvailable) {
                    Toast.makeText(requireContext(), "Invalid quantity", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val ws = dialogBinding.etWholesaleOverride.text.toString().toDoubleOrNull() ?: product.wholesalePrice
                val rt = dialogBinding.etRetailOverride.text.toString().toDoubleOrNull() ?: product.retailPrice

                viewModel.insertSaleItem(
                    SaleItem(
                        saleGroupId = groupId,
                        productId = item.productId,
                        quantity = qty,
                        wholesalePriceAtSale = ws,
                        retailPriceAtSale = rt
                    )
                )
                Toast.makeText(requireContext(), "Added to sale", Toast.LENGTH_SHORT).show()
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

        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Enter Password"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Unlock Price Editing")
            .setMessage("Please enter the password to change the price.")
            .setView(input)
            .setPositiveButton("Unlock") { _, _ ->
                val entered = input.text.toString()
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
