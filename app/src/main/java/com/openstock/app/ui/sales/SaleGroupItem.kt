package com.openstock.app.ui.sales

import com.openstock.app.data.model.SaleGroupSummary

sealed class SaleGroupItem {
    data class Header(val date: String, val totalWholesale: Double, val totalRetail: Double) : SaleGroupItem()
    data class Summary(val summary: SaleGroupSummary) : SaleGroupItem()
}
