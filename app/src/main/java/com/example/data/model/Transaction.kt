package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val totalAmount: Double,
    val paidAmount: Double,
    val changeAmount: Double,
    val paymentMethod: String, // "CASH", "QRIS", "TRANSFER"
    val itemsSerialized: String // Raw string format: "Soto;2;18000|Jasuke;1;8000"
) {
    // Helper data structure representing an item within the transaction
    data class TransactionItem(
        val name: String,
        val quantity: Int,
        val price: Double
    ) {
        val total: Double get() = quantity * price
    }

    // Convert the serialized raw string back into a structural list of purchased items
    fun getItemsList(): List<TransactionItem> {
        if (itemsSerialized.isBlank()) return emptyList()
        return itemsSerialized.split("|").mapNotNull { part ->
            val subParts = part.split(";")
            if (subParts.size >= 3) {
                TransactionItem(
                    name = subParts[0],
                    quantity = subParts[1].toIntOrNull() ?: 0,
                    price = subParts[2].toDoubleOrNull() ?: 0.0
                )
            } else {
                null
            }
        }
    }

    companion object {
        // Core serialization helper to turn a list of items into a flat string
        fun serializeItems(items: List<TransactionItem>): String {
            return items.filter { it.quantity > 0 }.joinToString("|") {
                "${it.name};${it.quantity};${it.price}"
            }
        }
    }
}
