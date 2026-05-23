package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AppDatabase
import com.example.data.repository.CashierRepository
import com.example.ui.screens.MainCashierScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CashierViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge support for notch/navigation bar safe areas
        enableEdgeToEdge()

        // Core Room database setup and repository pattern
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = CashierRepository(
            menuDao = database.menuDao(),
            transactionDao = database.transactionDao()
        )

        // Instantiate CashierViewModel passing repository and context
        val viewModel = CashierViewModel(repository, applicationContext)

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

            // Dynamic theme controller utilizing customized warm schemes
            MyApplicationTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainCashierScreen(viewModel = viewModel)
                }
            }
        }
    }
}
