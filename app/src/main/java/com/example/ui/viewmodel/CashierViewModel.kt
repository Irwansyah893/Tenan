package com.example.ui.viewmodel

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.MenuItem
import com.example.data.model.Transaction
import com.example.data.repository.CashierRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CashierViewModel(
    private val repository: CashierRepository,
    private val context: Context
) : ViewModel() {

    // Persistent SharedPreferences for settings like custom Dark Mode
    private val prefs = context.getSharedPreferences("warung_kasir_prefs", Context.MODE_PRIVATE)

    // Dark mode state: loaded from user preference, defaults to system theme
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Loaded menu items
    val menuItems: StateFlow<List<MenuItem>> = repository.allMenuItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Transaction History logs
    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Selected items map (MenuItem -> Quantity count)
    private val _cart = MutableStateFlow<Map<MenuItem, Int>>(emptyMap())
    val cart: StateFlow<Map<MenuItem, Int>> = _cart.asStateFlow()

    // Paid amount text from cash checkout screen
    private val _paidAmountText = MutableStateFlow("")
    val paidAmountText: StateFlow<String> = _paidAmountText.asStateFlow()

    // Chosen payment method
    private val _paymentMethod = MutableStateFlow("CASH") // "CASH", "QRIS", "TRANSFER"
    val paymentMethod: StateFlow<String> = _paymentMethod.asStateFlow()

    // Active receipt of transaction if checkout just completed
    private val _lastCompletedTransaction = MutableStateFlow<Transaction?>(null)
    val lastCompletedTransaction: StateFlow<Transaction?> = _lastCompletedTransaction.asStateFlow()

    // Transaction status for QRIS: "PENDING", "SUCCESS", or null
    private val _qrisPaymentStatus = MutableStateFlow<String?>(null)
    val qrisPaymentStatus: StateFlow<String?> = _qrisPaymentStatus.asStateFlow()

    init {
        // Prepare default menus on launch
        viewModelScope.launch {
            repository.checkAndSeedDatabase()
        }
    }

    // Toggle and persist Dark Mode configurations
    fun toggleDarkMode() {
        val nextMode = !_isDarkMode.value
        _isDarkMode.value = nextMode
        prefs.edit().putBoolean("dark_mode", nextMode).apply()
    }

    // Add item quantity to cart
    fun addToCart(item: MenuItem) {
        val current = _cart.value.toMutableMap()
        current[item] = (current[item] ?: 0) + 1
        _cart.value = current
    }

    // Subtract item quantity from cart
    fun removeFromCart(item: MenuItem) {
        val current = _cart.value.toMutableMap()
        val count = current[item] ?: 0
        if (count > 1) {
            current[item] = count - 1
        } else {
            current.remove(item)
        }
        _cart.value = current
    }

    // Remove single line item completely from cart
    fun removeLineItem(item: MenuItem) {
        val current = _cart.value.toMutableMap()
        current.remove(item)
        _cart.value = current
    }

    // Empty entire active transaction
    fun clearCart() {
        _cart.value = emptyMap()
        _paidAmountText.value = ""
        _paymentMethod.value = "CASH"
        _qrisPaymentStatus.value = null
        _lastCompletedTransaction.value = null
    }

    // Check if the shopping cart is completely empty
    fun isCartEmpty(): Boolean = _cart.value.isEmpty()

    // Compute total sum of active basket items
    fun getCartTotal(): Double {
        return _cart.value.entries.sumOf { it.key.price * it.value }
    }

    // Update monetary pay in textual input
    fun setPaidAmountText(amount: String) {
        // filter out non-digit characters
        val formatted = amount.filter { it.isDigit() }
        _paidAmountText.value = formatted
    }

    // Change checkout method
    fun setPaymentMethod(method: String) {
        _paymentMethod.value = method
        if (method == "QRIS") {
            _qrisPaymentStatus.value = "PENDING"
        } else {
            _qrisPaymentStatus.value = null
        }
    }

    // Custom utility buttons to quick-add cash values (e.g. +Rp10k, +Rp50k, or Cash match)
    fun appendCashValue(value: Double) {
        val currentVal = _paidAmountText.value.toDoubleOrNull() ?: 0.0
        val targetVal = currentVal + value
        _paidAmountText.value = targetVal.toLong().toString()
    }

    // Set cash precisely matching the exact amount needed
    fun setExactCash() {
        _paidAmountText.value = getCartTotal().toLong().toString()
    }

    // Complete QRIS mock states
    fun completeQrisSuccess() {
        _qrisPaymentStatus.value = "SUCCESS"
    }

    // Core transaction checkout engine salvaging history
    fun processCheckout(onSuccess: (Transaction) -> Unit, onError: (String) -> Unit) {
        val total = getCartTotal()
        if (total <= 0.0) {
            onError("Keranjang belanja masih kosong!")
            return
        }

        val method = _paymentMethod.value
        var paid = total
        var change = 0.0

        if (method == "CASH") {
            val paidInput = _paidAmountText.value.toDoubleOrNull() ?: 0.0
            if (paidInput < total) {
                onError("Pembayaran kurang! Kurang Rp${(total - paidInput).toInt()}")
                return
            }
            paid = paidInput
            change = paidInput - total
        } else if (method == "QRIS") {
            if (_qrisPaymentStatus.value != "SUCCESS") {
                onError("Tunggu hingga pembayaran QRIS berhasil diverifikasi!")
                return
            }
        }

        // Format items list for database save
        val transactionItems = _cart.value.map {
            Transaction.TransactionItem(
                name = it.key.name,
                quantity = it.value,
                price = it.key.price
            )
        }
        val serialized = Transaction.serializeItems(transactionItems)

        val tx = Transaction(
            totalAmount = total,
            paidAmount = paid,
            changeAmount = change,
            paymentMethod = method,
            itemsSerialized = serialized
        )

        viewModelScope.launch {
            repository.insertTransaction(tx)
            _lastCompletedTransaction.value = tx

            // Audio validation: Play sound effect confirming positive sales checkout (Cash drawer beep sound)
            playAlertTone()

            // Reset checkout state fields
            _cart.value = emptyMap()
            _paidAmountText.value = ""
            _qrisPaymentStatus.value = null

            onSuccess(tx)
        }
    }

    // Execute customizable tone for quick checkout audio feedback
    private fun playAlertTone() {
        try {
            val toneG = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            toneG.startTone(ToneGenerator.TONE_PROP_BEEP, 130)
        } catch (e: Exception) {
            // Safe fallback if audio hardware is unavailable in VM environment
            e.printStackTrace()
        }
    }

    // Custom Menu Item actions (letting users add/edit menus to increase POS completeness)
    fun addNewMenuItem(name: String, price: Double, category: String) {
        viewModelScope.launch {
            repository.addMenuItem(MenuItem(name = name, price = price, category = category))
        }
    }

    fun deleteMenuItemAndRemove(item: MenuItem) {
        viewModelScope.launch {
            repository.deleteMenuItem(item)
        }
    }

    // Delete single transaction logs
    fun deleteTransactionLogs(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    // Wipe transaction registers clean
    fun clearTransactionLogs() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
