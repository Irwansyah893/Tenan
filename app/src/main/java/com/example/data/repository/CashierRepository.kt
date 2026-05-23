package com.example.data.repository

import com.example.data.local.MenuDao
import com.example.data.local.TransactionDao
import com.example.data.model.MenuItem
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow

class CashierRepository(
    private val menuDao: MenuDao,
    private val transactionDao: TransactionDao
) {
    val allMenuItems: Flow<List<MenuItem>> = menuDao.getAllMenuItems()
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    // Seeds standard menus (Soto, Jasuke, Roti Bakar) if first-time launching with an empty database
    suspend fun checkAndSeedDatabase() {
        val count = menuDao.getMenuItemCount()
        if (count == 0) {
            menuDao.insertMenuItem(MenuItem(id = 1, name = "Soto", price = 18000.0, category = "Makanan", isDefault = true))
            menuDao.insertMenuItem(MenuItem(id = 2, name = "Jasuke", price = 8000.0, category = "Cemilan", isDefault = true))
            menuDao.insertMenuItem(MenuItem(id = 3, name = "Roti Bakar", price = 23000.0, category = "Cemilan", isDefault = true))
        }
    }

    suspend fun addMenuItem(item: MenuItem) {
        menuDao.insertMenuItem(item)
    }

    suspend fun updateMenuItem(item: MenuItem) {
        menuDao.updateMenuItem(item)
    }

    suspend fun deleteMenuItem(item: MenuItem) {
        menuDao.deleteMenuItem(item)
    }

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun clearHistory() {
        transactionDao.clearHistory()
    }

    suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteTransactionById(id)
    }
}
