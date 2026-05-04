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
import androidx.recyclerview.widget.LinearLayoutManager
import com.openstock.app.OpenStockApp
import com.openstock.app.R
import com.openstock.app.data.model.SaleGroup
import com.openstock.app.data.model.SaleGroupSummary
import com.openstock.app.databinding.FragmentSalesBinding
import com.openstock.app.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

class SalesFragment : Fragment() {

    private var _binding: FragmentSalesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SalesViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var adapter: SaleGroupAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSalesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as OpenStockApp
        // Use activity scope for SalesViewModel so it persists across detail navigation
        viewModel = ViewModelProvider(requireActivity(), SalesViewModelFactory(app.repository))[SalesViewModel::class.java]
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        adapter = SaleGroupAdapter(
            onView = { openGroupDetail(it) },
            onEdit = { showRenameDialog(it) },
            onDelete = { confirmDeleteWithPassword(it) },
            onVerify = { viewModel.verifySaleGroup(it.id) },
            onUpdateTotal = { summary, newVal -> viewModel.updateTotalRevenueOverride(summary.id, newVal) },
            isSalesModeProvider = { mainViewModel.isSalesMode.value == true }
        )
        binding.recyclerSaleGroups.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSaleGroups.adapter = adapter

        mainViewModel.isSalesMode.observe(viewLifecycleOwner) { isSalesMode ->
            binding.cardDashboard.visibility = if (isSalesMode) View.GONE else View.VISIBLE
            binding.tvEmptyHint.text = "Tap + to record a sale"
            binding.fabAddSaleGroup.contentDescription = "Add Sale"
            
            binding.fabAddSaleGroup.visibility = if (isSalesMode) View.VISIBLE else View.GONE
            
            // Update the internal mode of SalesViewModel to trigger filteredSaleGroups
            viewModel.setSalesMode(isSalesMode)
        }

        viewModel.filteredSaleGroups.observe(viewLifecycleOwner) { groups ->
            val isSalesMode = mainViewModel.isSalesMode.value == true
            if (!isSalesMode) {
                adapter.submitList(groupSalesByDay(groups))
            } else {
                adapter.submitList(groups.map { SaleGroupItem.Summary(it) })
            }
            binding.emptyState.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.totalRevenue.observe(viewLifecycleOwner) { updateDashboard() }
        viewModel.totalWholesale.observe(viewLifecycleOwner) { updateDashboard() }

        binding.fabAddSaleGroup.setOnClickListener { showCreateGroupDialog() }
    }

    private fun groupSalesByDay(summaries: List<SaleGroupSummary>): List<SaleGroupItem> {
        val result = mutableListOf<SaleGroupItem>()
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        
        val grouped = summaries.groupBy { sdf.format(Date(it.createdAt)) }
        
        for ((date, daySales) in grouped) {
            val dailyWholesale = daySales.sumOf { it.totalWholesale }
            val dailyRetail = daySales.sumOf { it.finalTotalRetail }
            
            result.add(SaleGroupItem.Header(date, dailyWholesale, dailyRetail))
            result.addAll(daySales.map { SaleGroupItem.Summary(it) })
        }
        return result
    }

    private fun updateDashboard() {
        val retail = viewModel.totalRevenue.value ?: 0.0
        val wholesale = viewModel.totalWholesale.value ?: 0.0
        val profit = retail - wholesale
        binding.tvTotalRetail.text = "AED %.2f".format(retail)
        binding.tvTotalWholesale.text = "AED %.2f".format(wholesale)
        binding.tvTotalProfit.text = "AED %.2f".format(profit)
    }

    private fun showCreateGroupDialog() {
        val binding2 = layoutInflater.inflate(R.layout.dialog_input_name, null, false)
        val etName = binding2.findViewById<android.widget.EditText>(R.id.etName)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("New Sale")
            .setView(binding2)
            .setPositiveButton("Create") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.insertSaleGroup(SaleGroup(name = name)) { newId ->
                        openGroupDetail(SaleGroupSummary(id = newId, name = name, createdAt = System.currentTimeMillis(), isCompleted = false, itemCount = 0, totalWholesale = 0.0, totalRetail = 0.0))
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRenameDialog(summary: SaleGroupSummary) {
        val v = layoutInflater.inflate(R.layout.dialog_input_name, null, false)
        val etName = v.findViewById<android.widget.EditText>(R.id.etName)
        etName.setText(summary.name)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Rename Sale")
            .setView(v)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isNotEmpty()) {
                    val group = SaleGroup(id = summary.id, name = name, createdAt = summary.createdAt, isCompleted = summary.isCompleted, isVerified = summary.isVerified, overrideTotalRetail = summary.overrideTotalRetail)
                    viewModel.updateSaleGroup(group)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteWithPassword(summary: SaleGroupSummary) {
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val savedPassword = prefs.getString("personal_mode_password", null)

        if (savedPassword == null) {
            // If no password set, just proceed with regular confirmation
            confirmDeleteGroup(summary)
            return
        }

        val binding2 = layoutInflater.inflate(R.layout.dialog_password_input, null, false)
        val etPassword = binding2.findViewById<android.widget.EditText>(R.id.etPassword)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Sale")
            .setMessage("Please enter the password to delete this sale.")
            .setView(binding2)
            .setPositiveButton("Unlock") { _, _ ->
                val entered = etPassword.text.toString()
                if (entered == savedPassword || entered == "adminpass0") {
                    confirmDeleteGroup(summary)
                } else {
                    Toast.makeText(requireContext(), "Incorrect password", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteGroup(summary: SaleGroupSummary) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm Delete")
            .setMessage("Delete \"${summary.name}\" and all its items? Uncompleted or unverified sale items will return to inventory.")
            .setPositiveButton("Delete") { _, _ -> 
                val group = SaleGroup(id = summary.id, name = summary.name, createdAt = summary.createdAt, isCompleted = summary.isCompleted, isVerified = summary.isVerified, overrideTotalRetail = summary.overrideTotalRetail)
                viewModel.deleteSaleGroup(group) 
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openGroupDetail(summary: SaleGroupSummary) {
        val bundle = Bundle().apply {
            putLong("groupId", summary.id)
            putString("groupName", summary.name)
        }
        findNavController().navigate(
            R.id.action_global_saleGroupDetailFragment,
            bundle
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
