package com.openstock.app.ui.products

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.openstock.app.R
import com.openstock.app.data.model.Product
import com.openstock.app.databinding.DialogAddEditProductBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class AddEditProductDialog : DialogFragment() {

    private var _binding: DialogAddEditProductBinding? = null
    private val binding get() = _binding!!
    var onSave: ((Product) -> Unit)? = null
    var validator: (suspend (Product) -> String?)? = null
    var barcodeExistHandler: (suspend (String) -> Product?)? = null

    private var product: Product? = null
    private var initialBarcode: String = ""
    private var selectedImagePath: String? = null
    private var tempCameraUri: Uri? = null

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

    private val pickImageLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        uri?.let { saveImageToInternalStorage(it) }
    }

    private val takePictureLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempCameraUri?.let { saveImageToInternalStorage(it) }
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

        binding.btnChangePhoto.setOnClickListener {
            showPhotoSourceDialog()
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
            selectedImagePath = it.imagePath
            displayImage(it.imagePath)
        }
    }

    private fun showPhotoSourceDialog() {
        val options = arrayOf("Select File", "Take from Camera", "Download from Web")
        AlertDialog.Builder(requireContext())
            .setTitle("Add Product Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImageLauncher.launch("image/*")
                    1 -> openCamera()
                    2 -> showWebDownloadDialog()
                }
            }
            .show()
    }

    private fun openCamera() {
        val tempFile = File(requireContext().cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        tempCameraUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            tempFile
        )
        takePictureLauncher.launch(tempCameraUri)
    }

    private fun showWebDownloadDialog() {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "https://example.com/image.jpg"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Download from Web")
            .setMessage("Enter the URL of the product image:")
            .setView(input)
            .setPositiveButton("Download") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) {
                    downloadImageFromWeb(url)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun downloadImageFromWeb(url: String) {
        lifecycleScope.launch {
            val progressDialog = AlertDialog.Builder(requireContext())
                .setMessage("Downloading image...")
                .setCancelable(false)
                .show()

            val success = withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val fileName = "product_${System.currentTimeMillis()}.jpg"
                        val file = File(requireContext().filesDir, fileName)
                        val body = response.body
                        if (body != null) {
                            body.source().use { source ->
                                FileOutputStream(file).use { output ->
                                    source.inputStream().copyTo(output)
                                }
                            }
                            selectedImagePath = file.absolutePath
                            true
                        } else false
                    } else false
                } catch (e: Exception) {
                    false
                }
            }

            progressDialog.dismiss()
            if (success) {
                displayImage(selectedImagePath)
            } else {
                Toast.makeText(requireContext(), "Failed to download image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayImage(path: String?) {
        if (path != null) {
            val file = File(path)
            if (file.exists()) {
                Glide.with(this)
                    .load(file)
                    .centerCrop()
                    .into(binding.ivProductPhoto)
                binding.ivProductPhoto.alpha = 1.0f
            }
        }
    }

    private fun saveImageToInternalStorage(uri: android.net.Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val fileName = "product_${System.currentTimeMillis()}.jpg"
            val file = File(requireContext().filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            selectedImagePath = file.absolutePath
            displayImage(selectedImagePath)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show()
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
            id = product?.id ?: 0,
            name = name,
            barcode = binding.etBarcode.text.toString().trim(),
            wholesalePrice = wholesale,
            retailPrice = retail,
            description = binding.etDescription.text.toString().trim(),
            unit = binding.etUnit.text.toString().trim().ifEmpty { "pcs" },
            imagePath = selectedImagePath
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
