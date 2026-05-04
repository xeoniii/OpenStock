package com.openstock.app.ui.inventory

import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.openstock.app.R
import com.openstock.app.data.dao.InventoryRaw
import com.openstock.app.data.model.Product
import com.openstock.app.databinding.DialogAddEditInventoryBinding

class AddEditInventoryDialog : DialogFragment() {

    private var _binding: DialogAddEditInventoryBinding? = null
    private val binding get() = _binding!!

    var onSave: ((Long, Double) -> Unit)? = null
    private var existing: InventoryRaw? = null
    private var products: List<Product> = emptyList()
    private var preSelectedProductId: Long = -1L
    private var allInventory: List<InventoryRaw> = emptyList()

    companion object {
        fun newInstance(existing: InventoryRaw?, products: List<Product>, preSelectedProductId: Long = -1L, allInventory: List<InventoryRaw> = emptyList()): AddEditInventoryDialog {
            val dialog = AddEditInventoryDialog()
            dialog.existing = existing
            dialog.products = products
            dialog.preSelectedProductId = preSelectedProductId
            dialog.allInventory = allInventory
            return dialog
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_OpenStock_Dialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddEditInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        binding.tvTitle.text = if (existing == null) "Add to Inventory" else "Edit Stock"

        val productNames = products.map { it.name }
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, productNames)
        binding.spinnerProduct.adapter = spinnerAdapter

        // Handle pre-selection logic
        val targetId = existing?.productId ?: preSelectedProductId
        
        if (targetId != -1L) {
            val idx = products.indexOfFirst { it.id == targetId }
            if (idx >= 0) {
                // Using post ensures the selection happens after the spinner is fully initialized
                binding.spinnerProduct.post {
                    binding.spinnerProduct.setSelection(idx)
                }
                
                // If it's an edit of existing inventory, lock the spinner
                if (existing != null) {
                    binding.spinnerProduct.isEnabled = false
                }
            }
        }

        binding.spinnerProduct.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (existing == null) {
                    val selectedProduct = products[position]
                    val inv = allInventory.find { it.productId == selectedProduct.id }
                    if (inv != null) {
                        binding.etQuantity.setText(
                            if (inv.quantityInStock % 1.0 == 0.0) inv.quantityInStock.toInt().toString()
                            else inv.quantityInStock.toString()
                        )
                        binding.tvTitle.text = "Edit Stock"
                    } else {
                        binding.etQuantity.setText("")
                        binding.tvTitle.text = "Add to Inventory"
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        existing?.let { inv ->
            binding.etQuantity.setText(
                if (inv.quantityInStock % 1.0 == 0.0) inv.quantityInStock.toInt().toString()
                else inv.quantityInStock.toString()
            )
        }

        binding.btnSave.setOnClickListener { save() }
        binding.btnCancel.setOnClickListener { dismiss() }
    }

    private fun save() {
        val qtyStr = binding.etQuantity.text.toString().trim()
        if (qtyStr.isEmpty()) { 
            binding.etQuantity.error = "Required"
            return 
        }
        val qty = qtyStr.toDoubleOrNull() ?: run { 
            binding.etQuantity.error = "Invalid number"
            return 
        }

        if (binding.spinnerProduct.selectedItemPosition >= 0) {
            val selectedProduct = products[binding.spinnerProduct.selectedItemPosition]
            onSave?.invoke(selectedProduct.id, qty)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
