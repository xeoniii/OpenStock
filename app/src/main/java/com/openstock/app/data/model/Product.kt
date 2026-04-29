package com.openstock.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val barcode: String = "",
    val wholesalePrice: Double,
    val retailPrice: Double,
    val description: String = "",
    val unit: String = "pcs",
    val imagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
