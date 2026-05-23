package com.example.ui.screens

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MenuItem
import com.example.data.model.Transaction
import com.example.ui.viewmodel.CashierViewModel
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// Formatter to convert Double to Indonesian Rupiah representation
fun formatRupiah(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    format.maximumFractionDigits = 0
    return format.format(amount).replace("Rp", "Rp ")
}

// Print trigger executing a safe HTML print job using Android components
fun printTransactionReceipt(context: Context, transaction: Transaction) {
    try {
        val htmlContent = generateReceiptHtml(transaction)
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val printAdapter = webView.createPrintDocumentAdapter("Warung_Kasir_Struk_${transaction.timestamp}")
                val jobName = "${context.packageName} - Struk ${transaction.id}"
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal memproses cetak: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// Receipt HTML file formatting as custom css-print layout
fun generateReceiptHtml(transaction: Transaction): String {
    val items = transaction.getItemsList()
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val dateStr = sdf.format(Date(transaction.timestamp))

    val itemsHtml = StringBuilder()
    items.forEach { item ->
        itemsHtml.append("""
            <tr>
                <td style="text-align: left; padding: 4px 0;">${item.name}</td>
                <td style="text-align: center; padding: 4px 0;">${item.quantity}</td>
                <td style="text-align: right; padding: 4px 0;">${item.price.toInt()}</td>
                <td style="text-align: right; padding: 4px 0;">${item.total.toInt()}</td>
            </tr>
        """.trimIndent())
    }

    return """
        <html>
        <head>
        <meta charset="utf-8">
        <style>
            @page { size: 58mm auto; margin: 0; }
            body { 
                font-family: 'Courier New', Courier, monospace; 
                font-size: 11px; 
                color: #000; 
                background: #FFF;
                width: 48mm; 
                margin: 0 auto; 
                padding: 4px;
                box-sizing: border-box;
            }
            .text-center { text-align: center; }
            .divider { border-top: 1px dotted #000; margin: 6px 0; }
            table { width: 100%; border-collapse: collapse; margin: 4px 0; }
            th { border-bottom: 1px dashed #000; padding: 4px 0; font-size: 11px; }
            .total-row { font-weight: bold; }
            .footer { margin-top: 12px; font-size: 9px; line-height: 1.2; text-align: center; }
        </style>
        </head>
        <body>
            <div class="text-center">
                <h3 style="margin: 0; font-size: 14px; font-weight: bold;">WARUNG KASIR</h3>
                <p style="margin: 2px 0; font-size: 9px;">Sajian Lezat &amp; Cepat</p>
                <p style="margin: 1px 0; font-size: 8px;">Jl. Selera Makanan No. 5</p>
                <div class="divider"></div>
            </div>
            
            <p style="margin: 2px 0; font-size: 9px;"><b>No:</b> TX-${transaction.timestamp}</p>
            <p style="margin: 2px 0; font-size: 9px;"><b>Waktu:</b> $dateStr</p>
            <p style="margin: 2px 0; font-size: 9px;"><b>Bayar:</b> ${transaction.paymentMethod}</p>
            
            <div class="divider"></div>
            
            <table>
                <thead>
                    <tr>
                        <th style="text-align: left; width: 40%;">Menu</th>
                        <th style="text-align: center; width: 15%;">Qty</th>
                        <th style="text-align: right; width: 20%;">Unit</th>
                        <th style="text-align: right; width: 25%;">Total</th>
                    </tr>
                </thead>
                <tbody>
                    $itemsHtml
                </tbody>
            </table>
            
            <div class="divider"></div>
            
            <table>
                <tr class="total-row">
                    <td colspan="3" style="text-align: left; padding: 2px 0;">Total Belanja:</td>
                    <td style="text-align: right; padding: 2px 0;">${transaction.totalAmount.toInt()}</td>
                </tr>
                <tr>
                    <td colspan="3" style="text-align: left; padding: 2px 0; font-weight: normal;">Uang Bayar:</td>
                    <td style="text-align: right; padding: 2px 0; font-weight: normal;">${transaction.paidAmount.toInt()}</td>
                </tr>
                <tr class="total-row" style="font-size: 12px;">
                    <td colspan="3" style="text-align: left; padding: 2px 0; border-top: 1px dashed #000;">KEMBALIAN:</td>
                    <td style="text-align: right; padding: 2px 0; border-top: 1px dashed #000;">${transaction.changeAmount.toInt()}</td>
                </tr>
            </table>
            
            <div class="divider"></div>
            
            <div class="footer">
                <p style="margin: 0; font-weight: bold;">Terima Kasih Banyak!</p>
                <p style="margin: 2px 0;">Semoga Menikmati Hidangan Kami</p>
                <p style="margin: 4px 0 0; font-size: 7px; color: #666;">Dicetak via Warung Kasir Android</p>
            </div>
        </body>
        </html>
    """.trimIndent()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainCashierScreen(viewModel: CashierViewModel) {
    val context = LocalContext.current
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val menuItems by viewModel.menuItems.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val currentPaymentMethod by viewModel.paymentMethod.collectAsStateWithLifecycle()
    val paidInputAmount by viewModel.paidAmountText.collectAsStateWithLifecycle()
    val currentLastTx by viewModel.lastCompletedTransaction.collectAsStateWithLifecycle()
    val qrisState by viewModel.qrisPaymentStatus.collectAsStateWithLifecycle()

    // Screen Tabs navigation state
    var selectedTab by remember { mutableStateOf(0) } // 0 = Kasir, 1 = Riwayat/Laporan, 2 = Atur Menu
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var menuSearchQuery by remember { mutableStateOf("") }

    // Realtime ticking clock system
    var currentTimeString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val sdf = SimpleDateFormat("EEEE, dd MMM yyyy • HH:mm:ss", Locale("id", "ID"))
            currentTimeString = sdf.format(Date())
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.shadow(8.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Store,
                                contentDescription = "Logo Warung",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(36.dp)
                                    .padding(end = 8.dp)
                            )
                            Column {
                                Text(
                                    text = "Warung Kasir",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Kelola Usaha Kuliner Ringan & Cepat",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Theme switch toggle
                        IconButton(
                            onClick = { viewModel.toggleDarkMode() },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = "Ganti Tema",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Realtime ticking display string
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = "Jam Realtime",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentTimeString.ifBlank { "Membuka aplikasi..." },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                tonalElevation = 8.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = "Mesin Kasir") },
                    label = { Text("Kasir", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Laporan Penjualan") },
                    label = { Text("Riwayat", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.MenuBook, contentDescription = "Daftar Menu Makanan") },
                    label = { Text("Atur Menu", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> CashierTabScreen(
                    viewModel = viewModel,
                    menuItems = menuItems,
                    cart = cart,
                    onStartCheckout = { showCheckoutDialog = true },
                    onReset = { viewModel.clearCart() }
                )
                1 -> ReportsTabScreen(
                    viewModel = viewModel,
                    transactions = transactions
                )
                2 -> MenuManagerTabScreen(
                    viewModel = viewModel,
                    menuItems = menuItems
                )
            }
        }
    }

    // Modal popup Checkout Form 
    if (showCheckoutDialog) {
        Dialog(onDismissRequest = { showCheckoutDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(12.dp),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = "Proses Pembayaran",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Total Tagihan:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatRupiah(viewModel.getCartTotal()),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Metode Pembayaran:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // Triple choice row: CASH, QRIS, TRANSFER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("CASH", "Tunai", Icons.Filled.LocalAtm),
                            Triple("QRIS", "QRIS", Icons.Filled.QrCodeScanner),
                            Triple("TRANSFER", "Transfer", Icons.Filled.AccountBalance)
                        ).forEach { (code, label, icon) ->
                            val active = currentPaymentMethod == code
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.setPaymentMethod(code) }
                                    .border(
                                        width = if (active) 2.dp else 1.dp,
                                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Context-based inputs
                    when (currentPaymentMethod) {
                        "CASH" -> {
                            Text(
                                text = "Jumlah Uang Diterima (Rp):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            OutlinedTextField(
                                value = paidInputAmount,
                                onValueChange = { viewModel.setPaidAmountText(it) },
                                placeholder = { Text("Masukkan nominal cash", fontSize = 13.sp) },
                                isError = if (paidInputAmount.isNotBlank()) (paidInputAmount.toDoubleOrNull() ?: 0.0) < viewModel.getCartTotal() else false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cash_received_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                trailingIcon = {
                                    if (paidInputAmount.isNotBlank()) {
                                        IconButton(onClick = { viewModel.setPaidAmountText("") }) {
                                            Icon(Icons.Filled.Clear, contentDescription = "Bersihkan input")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Fast cash access buttons ("Uang Pas", Rp10rb, Rp20rb, Rp50rb, Rp100rb)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.setExactCash() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Uang Pas", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                listOf(10000.0, 20000.0, 50000.0, 100000.0).forEach { amount ->
                                    val formattedVal = if (amount >= 1000) "${(amount / 1000).toInt()}k" else amount.toInt().toString()
                                    Button(
                                        onClick = { viewModel.appendCashValue(amount) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("+$formattedVal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Change evaluation
                            val totalFloat = viewModel.getCartTotal()
                            val paidFloat = paidInputAmount.toDoubleOrNull() ?: 0.0
                            val changeValue = paidFloat - totalFloat

                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (changeValue >= 0) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (changeValue >= 0) "Kembalian:" else "Kurang Bayar:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (changeValue >= 0) formatRupiah(changeValue) else formatRupiah(kotlin.math.abs(changeValue)),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (changeValue >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        "QRIS" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Draw a lovely mockup digital QR block
                                Box(
                                    modifier = Modifier
                                        .size(150.dp)
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Custom visual layout inside QR frame representing pixels
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val rowsCols = 15
                                        val squareSize = size.width / rowsCols
                                        val random = Random(2026)
                                        for (r in 0 until rowsCols) {
                                            for (c in 0 until rowsCols) {
                                                // Create structural patterns for code scanning mockup
                                                val shouldFill = when {
                                                    // Corners
                                                    (r < 4 && c < 4) || (r < 4 && c >= rowsCols - 4) || (r >= rowsCols - 4 && c < 4) -> true
                                                    r == 1 || c == 1 || r == rowsCols - 2 || c == rowsCols - 2 -> false
                                                    random.nextBoolean() -> true
                                                    else -> false
                                                }
                                                if (shouldFill) {
                                                    drawRect(
                                                        color = Color(0xFF1E1E1E),
                                                        topLeft = androidx.compose.ui.geometry.Offset(c * squareSize, r * squareSize),
                                                        size = androidx.compose.ui.geometry.Size(squareSize + 0.5f, squareSize + 0.5f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (qrisState == "PENDING") {
                                        val infiniteTransition = rememberInfiniteTransition(label = "Blink")
                                        val alpha by infiniteTransition.animateFloat(
                                            initialValue = 0.3f,
                                            targetValue = 1.0f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(800, easing = LinearEasing),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "BlinkAlpha"
                                        )

                                        Icon(
                                            imageVector = Icons.Filled.Pending,
                                            contentDescription = "Menunggu",
                                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = alpha),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Menunggu Pembayaran...",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = alpha)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Sukses",
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Pembayaran Diverifikasi!",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                }

                                if (qrisState == "PENDING") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { viewModel.completeQrisSuccess() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4CAF50),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Pembayaran Berhasil", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        "TRANSFER" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "Instruksi Transfer Rekening:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Bank Mandiri • 131-00-555555-4\na/n Warung Kasir Nusantara",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = { viewModel.completeQrisSuccess() }, // Utilizes verified mockup status
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                ) {
                                    Text("Validasi Transfer Berhasil", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Dialog Actions (Batal vs Bayar Sekarang)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCheckoutDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Batal", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.processCheckout(
                                    onSuccess = {
                                        showCheckoutDialog = false
                                        // Auto shows receipt thermal slip after checkout
                                    },
                                    onError = { error ->
                                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("process_checkout_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Selesai Bayar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal popup Receipt Thermal slip (Retro receipt paper)
    if (currentLastTx != null) {
        val tx = currentLastTx!!
        Dialog(onDismissRequest = { viewModel.clearCart() }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Transaksi Selesai!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Skewomorphic thermal paper layout
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF9)), // Retro thermal paper color
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                        ) {
                            Text(
                                text = "WARUNG KASIR",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Sajian Cepat Enak Bersahabat\nJl. Selera Makanan No. 5",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                lineHeight = 12.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Dotted horizontal line separator (visualised via canvas to look exactly like thermal lines)
                            DottedLine(color = Color.Black)

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Kasir ID: TX-${tx.timestamp}\nTanggal: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(tx.timestamp))}\nMetode: ${tx.paymentMethod}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black,
                                lineHeight = 12.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            DottedLine(color = Color.Black)
                            Spacer(modifier = Modifier.height(6.dp))

                            // Items grid row
                            tx.getItemsList().forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.name} x${item.quantity}",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Black,
                                        modifier = Modifier.weight(1.5f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatRupiah(item.total),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Black,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            DottedLine(color = Color.Black)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("TOTAL:", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text(formatRupiah(tx.totalAmount), fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("BAYAR:", fontSize = 10.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                                Text(formatRupiah(tx.paidAmount), fontSize = 10.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("KEMBALIAN:", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text(formatRupiah(tx.changeAmount), fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            DottedLine(color = Color.Black)
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "TERIMA KASIH SEBANYAK-BANYAKNYA!\nWarung Kasir - Nikmat & Responsif",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.DarkGray,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                lineHeight = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Native Print manager trigger
                        OutlinedButton(
                            onClick = { printTransactionReceipt(context, tx) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Print, contentDescription = "Cetak")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cetak Struk", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.clearCart() // Resets details, empties cart, and dismisses receipt popup
                            },
                            modifier = Modifier.weight(1.2f).testTag("close_receipt_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Transaksi Baru", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Draw dotted visual division inside thermal slip
@Composable
fun DottedLine(color: Color) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
            pathEffect = pathEffect,
            strokeWidth = 2f
        )
    }
}

// TAB 1: KASIR sales catalog screen
@Composable
fun CashierTabScreen(
    viewModel: CashierViewModel,
    menuItems: List<MenuItem>,
    cart: Map<MenuItem, Int>,
    onStartCheckout: () -> Unit,
    onReset: () -> Unit
) {
    var searchFilterText by remember { mutableStateOf("") }
    val filteredMenu = menuItems.filter { it.name.contains(searchFilterText, ignoreCase = true) }

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight()
                .padding(start = 12.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchFilterText,
                onValueChange = { searchFilterText = it },
                placeholder = { Text("Cari hidangan menu...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Cari") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Grid list of menu foods
            if (filteredMenu.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = "Menu Kosong",
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tidak ada kecocokan menu",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    items(filteredMenu) { item ->
                        val qtyInCart = cart[item] ?: 0
                        FoodMenuCard(
                            item = item,
                            qty = qtyInCart,
                            onAdd = { viewModel.addToCart(item) },
                            onSub = { viewModel.removeFromCart(item) }
                        )
                    }
                }
            }
        }

        // Right hand vertical pane containing the running shopping bill details
        Surface(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxHeight(),
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pesanan Aktif",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (cart.isNotEmpty()) {
                        IconButton(
                            onClick = onReset,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Reset keranjang",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cart lines list
                if (cart.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.ShoppingCart,
                                contentDescription = "Keranjang Kosong",
                                tint = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Keranjang Kosong",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(cart.entries.toList()) { entry ->
                            CartLineItemRow(
                                item = entry.key,
                                quantity = entry.value,
                                onAdd = { viewModel.addToCart(entry.key) },
                                onSub = { viewModel.removeFromCart(entry.key) },
                                onDelete = { viewModel.removeLineItem(entry.key) }
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                // Total display counter
                val basketSum = viewModel.getCartTotal()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text("Total:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = formatRupiah(basketSum),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onStartCheckout,
                    enabled = cart.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("checkout_start_button")
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Bayar")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bayar (Checkout)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// Single card list widget matching Orange and Cream accents
@Composable
fun FoodMenuCard(
    item: MenuItem,
    qty: Int,
    onAdd: () -> Unit,
    onSub: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (qty > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            // Food category badge icon indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = item.category,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // If chosen quantity indicators
                if (qty > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${qty}x",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = formatRupiah(item.price),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quantities modification widget row (Tombol Tambah/Kurang)
            if (qty == 0) {
                Button(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Pilih", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onSub,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Kurang", modifier = Modifier.size(14.dp))
                    }

                    Text(
                        text = qty.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Button(
                        onClick = onAdd,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Tambah", modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

// Side cart line item row representation
@Composable
fun CartLineItemRow(
    item: MenuItem,
    quantity: Int,
    onAdd: () -> Unit,
    onSub: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${quantity}x  •  ${formatRupiah(item.price)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Small +/- and delete buttons configuration
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = onSub, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Filled.Remove, contentDescription = "Kurang", modifier = Modifier.size(12.dp))
                }
                Text(
                    text = quantity.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onAdd, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = "Tambah", modifier = Modifier.size(12.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Hapus baris", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// TAB 2: REPORTS & LOGS dashboard screen
@Composable
fun ReportsTabScreen(
    viewModel: CashierViewModel,
    transactions: List<Transaction>
) {
    val context = LocalContext.current
    var activeHistoryReceipt by remember { mutableStateOf<Transaction?>(null) }
    var filterTodayOnly by remember { mutableStateOf(false) }

    // Date computation details
    val calendarToday = Calendar.getInstance()
    calendarToday.set(Calendar.HOUR_OF_DAY, 0)
    calendarToday.set(Calendar.MINUTE, 0)
    calendarToday.set(Calendar.SECOND, 0)
    calendarToday.set(Calendar.MILLISECOND, 0)
    val startOfToday = calendarToday.timeInMillis

    // Apply filtering options
    val displayedTransactions = if (filterTodayOnly) {
        transactions.filter { it.timestamp >= startOfToday }
    } else {
        transactions
    }

    // Dashboard harian statistics
    val todayIncome = transactions.filter { it.timestamp >= startOfToday }.sumOf { it.totalAmount }
    val todayTxCount = transactions.filter { it.timestamp >= startOfToday }.size
    val totalIncome = transactions.sumOf { it.totalAmount }

    // Menghitung menu paling laris (Popular rankings)
    val itemsMapSalesCounts = mutableMapOf<String, Int>()
    transactions.forEach { tx ->
        tx.getItemsList().forEach { element ->
            itemsMapSalesCounts[element.name] = (itemsMapSalesCounts[element.name] ?: 0) + element.quantity
        }
    }
    val bestSellerMenu = itemsMapSalesCounts.maxByOrNull { it.value }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Dashboard Stats Overview cards row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Today Income
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Filled.TrendingUp, contentDescription = "Omset Hari Ini", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Pemasukan Hari Ini", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    Text(
                        text = formatRupiah(todayIncome),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Card 2: Today TX Count
            Card(
                modifier = Modifier.weight(0.9f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Filled.Receipt, contentDescription = "Transaksi", tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Total Transaksi", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                    Text(
                        text = "$todayTxCount Struk",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Card 3: Best Seller
            Card(
                modifier = Modifier.weight(1.1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Filled.Star, contentDescription = "Unggulan", tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Menu Terlaris", fontSize = 10.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                    Text(
                        text = bestSellerMenu?.let { "${it.key} (${it.value})" }.orEmpty().ifBlank { "Belum ada" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // History logs header control rows
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Daftar Riwayat Penjualan",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${displayedTransactions.size} Transaksi",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Wipe database history button
            if (transactions.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearTransactionLogs() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "Hapus Riwayat", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hapus Semua", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Filtering toggle switches
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = filterTodayOnly,
                onCheckedChange = { filterTodayOnly = it }
            )
            Text(
                text = "Tampilkan penjualan hari ini saja (${SimpleDateFormat("dd MMM", Locale("id","ID")).format(Date())})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { filterTodayOnly = !filterTodayOnly }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Grid contents / empty lists
        if (displayedTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Inbox,
                        contentDescription = "Catatan Kosong",
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Riwayat pembelian kosong",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayedTransactions) { log ->
                    TransactionHistoryRow(
                        tx = log,
                        onViewDetails = { activeHistoryReceipt = log },
                        onDeleteLog = { viewModel.deleteTransactionLogs(log.id) }
                    )
                }
            }
        }
    }

    // Modal popup detailed Reprint Struk thermal log
    if (activeHistoryReceipt != null) {
        val tx = activeHistoryReceipt!!
        Dialog(onDismissRequest = { activeHistoryReceipt = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Review Struk Penjualan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Skewomorphic thermal paper layout
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF9)),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                        ) {
                            Text(
                                text = "WARUNG KASIR",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Sajian Cepat Enak Bersahabat\nJl. Selera Makanan No. 5",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                lineHeight = 12.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            DottedLine(color = Color.Black)

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Kasir ID: TX-${tx.timestamp}\nTanggal: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(tx.timestamp))}\nMetode: ${tx.paymentMethod}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black,
                                lineHeight = 12.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            DottedLine(color = Color.Black)
                            Spacer(modifier = Modifier.height(6.dp))

                            // Items grid row
                            tx.getItemsList().forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.name} x${item.quantity}",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Black,
                                        modifier = Modifier.weight(1.5f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatRupiah(item.total),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Black,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            DottedLine(color = Color.Black)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("TOTAL:", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text(formatRupiah(tx.totalAmount), fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("BAYAR:", fontSize = 10.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                                Text(formatRupiah(tx.paidAmount), fontSize = 10.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("KEMBALIAN:", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text(formatRupiah(tx.changeAmount), fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            DottedLine(color = Color.Black)
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "TERIMA KASIH SEBANYAK-BANYAKNYA!\nCetak Salinan Struk Transaksi",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.DarkGray,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                lineHeight = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { printTransactionReceipt(context, tx) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Print, contentDescription = "Cetak")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Salinan Cetak", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { activeHistoryReceipt = null },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Tutup Salinan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Single transaction register entry layout within report tab elements
@Composable
fun TransactionHistoryRow(
    tx: Transaction,
    onViewDetails: () -> Unit,
    onDeleteLog: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID")).format(Date(tx.timestamp))
    val totalItems = tx.getItemsList().sumOf { it.quantity }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1.4f)
            ) {
                // Payment type graphics
                val paymentIcon = when (tx.paymentMethod) {
                    "QRIS" -> Icons.Filled.QrCodeScanner
                    "TRANSFER" -> Icons.Filled.AccountBalance
                    else -> Icons.Filled.LocalAtm
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = paymentIcon,
                        contentDescription = tx.paymentMethod,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = formatRupiah(tx.totalAmount),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$dateStr • $totalItems item (${unifyMethodName(tx.paymentMethod)})",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onViewDetails, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = "Lihat struk",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDeleteLog, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Hapus log",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// Method name mapping helper
fun unifyMethodName(method: String): String {
    return when (method) {
        "QRIS" -> "QRIS"
        "TRANSFER" -> "Trf Bank"
        else -> "Tunai"
    }
}

// TAB 3: MENU ITEMS CONFIGURATION screen elements
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MenuManagerTabScreen(
    viewModel: CashierViewModel,
    menuItems: List<MenuItem>
) {
    var insertNameText by remember { mutableStateOf("") }
    var insertPriceText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Makanan") } // "Makanan", "Cemilan", "Minuman"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Core input header card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Text(
                    text = "Tambah Menu Hidangan Baru",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = insertNameText,
                        onValueChange = { insertNameText = it },
                        placeholder = { Text("Nama Makanan", fontSize = 12.sp) },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = insertPriceText,
                        onValueChange = { insertPriceText = it.filter { ch -> ch.isDigit() } },
                        placeholder = { Text("Harga (Rp)", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Triple choice selector category: Makanan, Cemilan, Minuman
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kategori:", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 4.dp))
                    listOf("Makanan", "Cemilan", "Minuman").forEach { category ->
                        val isMatched = selectedCategory == category
                        FilterChip(
                            selected = isMatched,
                            onClick = { selectedCategory = category },
                            label = { Text(category, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val nameStr = insertNameText.trim()
                        val priceNum = insertPriceText.toDoubleOrNull() ?: 0.0
                        if (nameStr.isBlank()) {
                            // Empty validation
                            return@Button
                        }
                        if (priceNum <= 0.0) {
                            // Negative validation
                            return@Button
                        }
                        viewModel.addNewMenuItem(nameStr, priceNum, selectedCategory)
                        insertNameText = ""
                        insertPriceText = ""
                        selectedCategory = "Makanan"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Simpan")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Simpan ke Daftar Jualan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Daftar Menu Saat Ini",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Contents grid mapping active catalog items
        if (menuItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Daftar Menu Kosong", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(menuItems) { dish ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = dish.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = dish.category,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = formatRupiah(dish.price),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Delete button (cannot delete default elements or let them, let let them delete to keep list flexible!)
                            IconButton(onClick = { viewModel.deleteMenuItemAndRemove(dish) }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Hapus Menu",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
