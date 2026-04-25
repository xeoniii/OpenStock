package com.openstock.app.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.openstock.app.ui.inventory.InventoryFragment
import com.openstock.app.ui.products.ProductsFragment
import com.openstock.app.ui.sales.SalesFragment

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    
    var isSalesMode: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    override fun getItemCount(): Int = if (isSalesMode) 2 else 3

    override fun createFragment(position: Int): Fragment {
        return if (isSalesMode) {
            when (position) {
                0 -> InventoryFragment()
                1 -> SalesFragment()
                else -> throw IllegalArgumentException("Invalid position")
            }
        } else {
            when (position) {
                0 -> ProductsFragment()
                1 -> InventoryFragment()
                2 -> SalesFragment()
                else -> throw IllegalArgumentException("Invalid position")
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return if (isSalesMode) {
            // Use different IDs for Sales mode fragments to avoid recycling issues
            when (position) {
                0 -> 101L // Inventory
                1 -> 102L // Sales
                else -> super.getItemId(position)
            }
        } else {
            when (position) {
                0 -> 0L // Products
                1 -> 1L // Inventory
                2 -> 2L // Sales
                else -> super.getItemId(position)
            }
        }
    }

    override fun containsItem(itemId: Long): Boolean {
        return if (isSalesMode) {
            itemId in 101L..102L
        } else {
            itemId in 0L..2L
        }
    }
}
