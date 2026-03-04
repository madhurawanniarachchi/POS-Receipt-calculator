// ═══════════════════════════════════════════════════════════════════════════════
// MainActivity - Jetpack Compose UI for POS Tax Calculator
// Complete implementation with all features
// ═══════════════════════════════════════════════════════════════════════════════

package com.madhura.clodeposcal.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import kotlin.collections.plus

// ─────────────────────────────────────────────────────────────────────────────
// Color Scheme
// ─────────────────────────────────────────────────────────────────────────────

object AppColors {
    val Background = Color(0xFF0E1117)
    val Surface = Color(0xFF161B27)
    val Card = Color(0xFF1E2535)
    val Border = Color(0xFF2A3347)
    val Border2 = Color(0xFF344060)
    val Text = Color(0xFFE8EAF0)
    val Muted = Color(0xFF7C8599)
    val Accent = Color(0xFF3B82F6)
    val Accent2 = Color(0xFF60A5FA)
    val Green = Color(0xFF22C55E)
    val Red = Color(0xFFEF4444)
    val Amber = Color(0xFFF59E0B)
    val Purple = Color(0xFFA855F7)
    val Teal = Color(0xFF14B8A6)
}

// ─────────────────────────────────────────────────────────────────────────────
// MainActivity
// ─────────────────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            POSTaxCalculatorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppColors.Background
                ) {
                    POSTaxCalculatorScreen()
                }
            }
        }
    }
}

@Composable
fun POSTaxCalculatorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AppColors.Accent,
            background = AppColors.Background,
            surface = AppColors.Surface,
            onPrimary = Color.White,
            onBackground = AppColors.Text,
            onSurface = AppColors.Text
        ),
        content = content
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun POSTaxCalculatorScreen() {
    // State management
    var items by remember {
        mutableStateOf(
            listOf(
                Item(
                    name = "Product A",
                    qty = BigDecimal("1"),
                    unitPrice = BigDecimal("390.00")
                ),
                Item(
                    name = "Product B",
                    qty = BigDecimal("1"),
                    unitPrice = BigDecimal("315.00")
                ),
                Item(
                    name = "Product C",
                    qty = BigDecimal("1"),
                    unitPrice = BigDecimal("70.00")
                )
            )
        )
    }
    
    var globalTaxes by remember {
        mutableStateOf(
            listOf(
                GlobalTax(
                    name = "Inc1",
                    rate = BigDecimal("5"),
                    mode = TaxMode.INCLUDE,
                    enabled = true
                ),
                GlobalTax(
                    name = "Inc2",
                    rate = BigDecimal("10"),
                    mode = TaxMode.INCLUDE,
                    enabled = true
                ),
                GlobalTax(
                    name = "Service Charge",
                    rate = BigDecimal("10"),
                    enabled = true
                ),
                GlobalTax(
                    name = "GST",
                    rate = BigDecimal("18"),
                    enabled = true
                )
            )
        )
    }
    
    var invoiceDiscounts by remember {
        mutableStateOf(
            listOf(
                InvoiceDiscount(
                    value = BigDecimal.ZERO,
                    enabled = true
                )
            )
        )
    }
    
    var fixedCharge by remember {
        mutableStateOf(
            FixedCharge(
                "Service Fee",
                BigDecimal.ZERO
            )
        )
    }
    
    // Calculate results
    val result = remember(items, globalTaxes, invoiceDiscounts, fixedCharge) {
        POSTaxCalculator(
            items,
            globalTaxes,
            invoiceDiscounts,
            fixedCharge
        ).calculate()
    }
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Left panel - Inputs
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(AppColors.Background)
        ) {
            InputPanel(
                items = items,
                globalTaxes = globalTaxes,
                invoiceDiscounts = invoiceDiscounts,
                fixedCharge = fixedCharge,
                onItemsChange = { items = it },
                onGlobalTaxesChange = { globalTaxes = it },
                onInvoiceDiscountsChange = { invoiceDiscounts = it },
                onFixedChargeChange = { fixedCharge = it }
            )
        }
        
        // Right panel - Receipt
        Box(
            modifier = Modifier
                .width(420.dp)
                .fillMaxHeight()
                .background(AppColors.Surface)
        ) {
            ReceiptPanel(result, fixedCharge)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Input Panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun InputPanel(
    items: List<Item>,
    globalTaxes: List<GlobalTax>,
    invoiceDiscounts: List<InvoiceDiscount>,
    fixedCharge: FixedCharge,
    onItemsChange: (List<Item>) -> Unit,
    onGlobalTaxesChange: (List<GlobalTax>) -> Unit,
    onInvoiceDiscountsChange: (List<InvoiceDiscount>) -> Unit,
    onFixedChargeChange: (FixedCharge) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = AppColors.Accent,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "POS",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Text(
                    "Tax Calculator",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Text
                )
            }
        }
        
        // Items Section
        item {
            SectionCard(
                title = "① Items",
                onAdd = { onItemsChange(items + Item()) }
            ) {
                items.forEach { item ->
                    ItemCard(
                        item = item,
                        onUpdate = { updated ->
                            onItemsChange(items.map { if (it.id == item.id) updated else it })
                        },
                        onDelete = {
                            onItemsChange(items.filter { it.id != item.id })
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
        
        // Invoice Discounts Section
        item {
            SectionCard(
                title = "② Invoice Discounts (Global)",
                onAdd = { onInvoiceDiscountsChange(invoiceDiscounts + InvoiceDiscount()) }
            ) {
                invoiceDiscounts.forEachIndexed { index, discount ->
                    InvoiceDiscountRow(
                        discount = discount,
                        index = index,
                        onUpdate = { updated ->
                            onInvoiceDiscountsChange(
                                invoiceDiscounts.map { if (it.id == discount.id) updated else it }
                            )
                        },
                        onDelete = {
                            onInvoiceDiscountsChange(invoiceDiscounts.filter { it.id != discount.id })
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    "✓ Applied sequentially after line discounts\n✓ Each allocated proportionally to items",
                    fontSize = 10.sp,
                    color = AppColors.Muted,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        
        // Global Taxes Section
        item {
            SectionCard(
                title = "③ Global Taxes",
                onAdd = { onGlobalTaxesChange(globalTaxes + GlobalTax()) }
            ) {
                globalTaxes.forEach { tax ->
                    GlobalTaxRow(
                        tax = tax,
                        onUpdate = { updated ->
                            onGlobalTaxesChange(globalTaxes.map { if (it.id == tax.id) updated else it })
                        },
                        onDelete = {
                            onGlobalTaxesChange(globalTaxes.filter { it.id != tax.id })
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
        
        // Fixed Charge Section
        item {
            SectionCard(title = "④ Fixed Charge") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    POSTextField(
                        value = fixedCharge.label,
                        onValueChange = { onFixedChargeChange(fixedCharge.copy(label = it)) },
                        modifier = Modifier.weight(1f),
                        placeholder = "Label"
                    )

                    POSTextField(
                        value = fixedCharge.value.toPlainString(),
                        onValueChange = {
                            try {
                                onFixedChargeChange(fixedCharge.copy(value = BigDecimal(it)))
                            } catch (e: Exception) {
                            }
                        },
                        modifier = Modifier.width(120.dp),
                        keyboardType = KeyboardType.Decimal,
                        prefix = "$"
                    )
                }

                Text(
                    "Applied after all taxes",
                    fontSize = 10.sp,
                    color = AppColors.Muted,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Item Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ItemCard(
    item: Item,
    onUpdate: (Item) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppColors.Surface,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Border2)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Item basic info
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                POSTextField(
                    value = item.name,
                    onValueChange = { onUpdate(item.copy(name = it)) },
                    modifier = Modifier.weight(1f),
                    placeholder = "Item name"
                )

                POSTextField(
                    value = item.qty.toPlainString(),
                    onValueChange = {
                        try {
                            onUpdate(item.copy(qty = BigDecimal(it)))
                        } catch (e: Exception) {
                        }
                    },
                    modifier = Modifier.width(70.dp),
                    keyboardType = KeyboardType.Decimal,
                    placeholder = "Qty"
                )

                POSTextField(
                    value = item.unitPrice.toPlainString(),
                    onValueChange = {
                        try {
                            onUpdate(item.copy(unitPrice = BigDecimal(it)))
                        } catch (e: Exception) {
                        }
                    },
                    modifier = Modifier.width(90.dp),
                    keyboardType = KeyboardType.Decimal,
                    placeholder = "Price"
                )
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Delete, "Delete", tint = AppColors.Red)
                }
            }
            
            // Line Discounts SubSection
            SubSection(
                title = "Line Discounts",
                onAdd = {
                    onUpdate(item.copy(lineDiscounts = item.lineDiscounts + LineDiscount()))
                }
            ) {
                item.lineDiscounts.forEach { discount ->
                    LineDiscountRow(
                        discount = discount,
                        onUpdate = { updated ->
                            onUpdate(
                                item.copy(
                                    lineDiscounts = item.lineDiscounts.map {
                                        if (it.id == discount.id) updated else it
                                    }
                                ))
                        },
                        onDelete = {
                            onUpdate(
                                item.copy(
                                    lineDiscounts = item.lineDiscounts.filter { it.id != discount.id }
                                ))
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
            
            // Item Taxes SubSection
            SubSection(
                title = "Item-Specific Taxes",
                onAdd = {
                    onUpdate(item.copy(itemTaxes = item.itemTaxes + ItemTax()))
                }
            ) {
                if (item.itemTaxes.isEmpty()) {
                    Text(
                        "No item-specific taxes. Global taxes will apply.",
                        fontSize = 11.sp,
                        color = AppColors.Muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                }

                item.itemTaxes.forEach { tax ->
                    ItemTaxRow(
                        tax = tax,
                        onUpdate = { updated ->
                            onUpdate(
                                item.copy(
                                    itemTaxes = item.itemTaxes.map {
                                        if (it.id == tax.id) updated else it
                                    }
                                ))
                        },
                        onDelete = {
                            onUpdate(
                                item.copy(
                                    itemTaxes = item.itemTaxes.filter { it.id != tax.id }
                                ))
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

// Continued in next message due to length...
