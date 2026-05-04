package com.openstock.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sale_groups",
    indices = [
        Index("isCompleted"),
        Index("isVerified"),
        Index("createdAt")
    ]
)
data class SaleGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val isVerified: Boolean = false,
    val overrideTotalRetail: Double? = null
)

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleGroup::class,
            parentColumns = ["id"],
            childColumns = ["saleGroupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("saleGroupId"), Index("productId")]
)
data class SaleItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleGroupId: Long,
    val productId: Long,
    val quantity: Double,
    val wholesalePriceAtSale: Double,
    val retailPriceAtSale: Double
)

data class SaleGroupSummary(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val isCompleted: Boolean,
    val itemCount: Int,
    val totalWholesale: Double,
    val totalRetail: Double,
    val isVerified: Boolean = false,
    val overrideTotalRetail: Double? = null
) {
    val finalTotalRetail: Double get() = overrideTotalRetail ?: totalRetail
}

data class SaleItemWithProduct(
    val saleItem: SaleItem,
    val product: Product
)

data class SaleGroupWithItems(
    val saleGroup: SaleGroup,
    val items: List<SaleItemWithProduct>
) {
    val totalWholesale: Double get() = items.sumOf { it.saleItem.quantity * it.saleItem.wholesalePriceAtSale }
    val totalRetail: Double get() = saleGroup.overrideTotalRetail ?: items.sumOf { it.saleItem.quantity * it.saleItem.retailPriceAtSale }
    val totalProfit: Double get() = totalRetail - totalWholesale
}
