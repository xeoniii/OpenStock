package com.openstock.app.ui.products

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.openstock.app.R
import com.openstock.app.data.model.Product
import com.openstock.app.databinding.DialogAddEditProductBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

class AddEditProductDialog : DialogFragment() {

    private var _binding: DialogAddEditProductBinding? = null
    private val binding get() = _binding!!
    var onSave: ((Product) -> Unit)? = null
    var validator: (suspend (Product) -> String?)? = null
    var barcodeExistHandler: (suspend (String) -> Product?)? = null

    private var product: Product? = null
    private var initialBarcode: String = ""

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val barcode = result.contents
            lifecycleScope.launch {
                val existing = barcodeExistHandler?.invoke(barcode)
                if (existing != null && (product == null || existing.id != product?.id)) {
                    // Switch to editing the existing product
                    product = existing
                    updateUi()
                } else {
                    binding.etBarcode.setText(barcode)
                }
            }
        }
    }

    companion object {
        fun newInstance(product: Product? = null, barcodeValue: String = ""): AddEditProductDialog {
            val dialog = AddEditProductDialog()
            dialog.product = product
            dialog.initialBarcode = barcodeValue
            return dialog
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddEditProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        
        updateUi()

        if (initialBarcode.isNotEmpty()) {
            binding.etBarcode.setText(initialBarcode)
        }

        binding.btnScanBarcode.setOnClickListener {
            val options = ScanOptions()
            options.setPrompt("Scan barcode")
            barcodeLauncher.launch(options)
        }

        binding.btnSave.setOnClickListener { save() }
        binding.btnCancel.setOnClickListener { dismiss() }
    }

    private fun updateUi() {
        binding.tvTitle.text = if (product == null) "Add Product" else "Edit Product"
        product?.let {
            binding.etName.setText(it.name)
            binding.etBarcode.setText(it.barcode)
            binding.etWholesalePrice.setText(it.wholesalePrice.toString())
            binding.etRetailPrice.setText(it.retailPrice.toString())
            binding.etDescription.setText(it.description)
            binding.etUnit.setText(it.unit)
        }
    }

    private fun save() {
        val name = binding.etName.text.toString().trim()
        val wholesaleStr = binding.etWholesalePrice.text.toString().trim()
        val retailStr = binding.etRetailPrice.text.toString().trim()

        if (name.isEmpty()) { 
            binding.etName.error = "Required"
            return 
        }
        if (wholesaleStr.isEmpty()) { 
            binding.etWholesalePrice.error = "Required"
            return 
        }
        if (retailStr.isEmpty()) { 
            binding.etRetailPrice.error = "Required"
            return 
        }

        val wholesale = wholesaleStr.toDoubleOrNull() ?: run {
            binding.etWholesalePrice.error = "Invalid number"
            return
        }
        val retail = retailStr.toDoubleOrNull() ?: run {
            binding.etRetailPrice.error = "Invalid number"
            return
        }

        val p = Product(
            name = name,
            barcode = binding.etBarcode.text.toString().trim(),
            wholesalePrice = wholesale,
            retailPrice = retail,
            description = binding.etDescription.text.toString().trim(),
            unit = binding.etUnit.text.toString().trim().ifEmpty { "pcs" }
        )
        
        lifecycleScope.launch {
            val error = validator?.invoke(p)
            if (error != null) {
                binding.etBarcode.error = error
                return@launch
            }

            if (onSave != null) {
                onSave?.invoke(p)
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Error: Save listener lost. Please try again.", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
