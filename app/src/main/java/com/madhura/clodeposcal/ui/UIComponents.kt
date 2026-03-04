// ═══════════════════════════════════════════════════════════════════════════════
// UI Components - Part 2
// Reusable components for POS Tax Calculator
// ═══════════════════════════════════════════════════════════════════════════════

package com.madhura.clodeposcal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal

// ─────────────────────────────────────────────────────────────────────────────
// Row Components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LineDiscountRow(
    discount: LineDiscount,
    onUpdate: (LineDiscount) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = discount.enabled,
            onCheckedChange = { onUpdate(discount.copy(enabled = it)) },
            modifier = Modifier.size(20.dp)
        )
        
        ToggleGroup(
            options = listOf("%" to DiscountType.PERCENT, "$" to DiscountType.FIXED),
            selected = discount.type,
            onSelect = { onUpdate(discount.copy(type = it)) },
            modifier = Modifier.width(80.dp),
            enabled = discount.enabled
        )
        
        POSTextField(
            value = discount.value.toPlainString(),
            onValueChange = {
                try { onUpdate(discount.copy(value = BigDecimal(it))) } catch (e: Exception) {}
            },
            modifier = Modifier.width(80.dp),
            keyboardType = KeyboardType.Decimal,
            enabled = discount.enabled
        )
        
        Text(
            if (discount.type == DiscountType.PERCENT) "${discount.value}%" else "$${discount.value}",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = if (discount.enabled) AppColors.Red else AppColors.Muted,
            modifier = Modifier.width(90.dp)
        )
        
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(Icons.Default.Delete, "Delete", tint = AppColors.Red, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun ItemTaxRow(
    tax: ItemTax,
    onUpdate: (ItemTax) -> Unit,
    onDelete: () -> Unit
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = tax.enabled,
                onCheckedChange = { onUpdate(tax.copy(enabled = it)) },
                modifier = Modifier.size(20.dp)
            )
            
            POSTextField(
                value = tax.name,
                onValueChange = { onUpdate(tax.copy(name = it)) },
                modifier = Modifier.weight(1f),
                placeholder = "Tax name",
                enabled = tax.enabled
            )
            
            POSTextField(
                value = tax.rate.toPlainString(),
                onValueChange = {
                    try { onUpdate(tax.copy(rate = BigDecimal(it))) } catch (e: Exception) {}
                },
                modifier = Modifier.width(80.dp),
                keyboardType = KeyboardType.Decimal,
                suffix = "%",
                enabled = tax.enabled
            )
            
            ToggleGroup(
                options = listOf("Excl" to TaxMode.EXCLUDE, "Incl" to TaxMode.INCLUDE),
                selected = tax.mode,
                onSelect = { onUpdate(tax.copy(mode = it)) },
                modifier = Modifier.width(90.dp),
                enabled = tax.enabled
            )
            
            ToggleGroup(
                options = listOf("Base" to false, "ToT" to true),
                selected = tax.taxOnTax,
                onSelect = { onUpdate(tax.copy(taxOnTax = it)) },
                modifier = Modifier.width(90.dp),
                enabled = tax.enabled
            )
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Delete, "Delete", tint = AppColors.Red, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun InvoiceDiscountRow(
    discount: InvoiceDiscount,
    index: Int,
    onUpdate: (InvoiceDiscount) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = discount.enabled,
            onCheckedChange = { onUpdate(discount.copy(enabled = it)) },
            modifier = Modifier.size(20.dp)
        )
        
        ToggleGroup(
            options = listOf("%" to DiscountType.PERCENT, "$" to DiscountType.FIXED),
            selected = discount.type,
            onSelect = { onUpdate(discount.copy(type = it)) },
            modifier = Modifier.weight(1f),
            enabled = discount.enabled
        )
        
        POSTextField(
            value = discount.value.toPlainString(),
            onValueChange = {
                try { onUpdate(discount.copy(value = BigDecimal(it))) } catch (e: Exception) {}
            },
            modifier = Modifier.width(80.dp),
            keyboardType = KeyboardType.Decimal,
            enabled = discount.enabled
        )
        
        Text(
            "Disc ${index + 1}: ${if (discount.type == DiscountType.PERCENT) "${discount.value}%" else "$${discount.value}"}",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = if (discount.enabled) AppColors.Purple else AppColors.Muted,
            modifier = Modifier.width(90.dp)
        )
        
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(Icons.Default.Delete, "Delete", tint = AppColors.Red, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun GlobalTaxRow(
    tax: GlobalTax,
    onUpdate: (GlobalTax) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = tax.enabled,
            onCheckedChange = { onUpdate(tax.copy(enabled = it)) },
            modifier = Modifier.size(20.dp)
        )
        
        POSTextField(
            value = tax.name,
            onValueChange = { onUpdate(tax.copy(name = it)) },
            modifier = Modifier.weight(1f),
            placeholder = "Tax name",
            enabled = tax.enabled
        )
        
        POSTextField(
            value = tax.rate.toPlainString(),
            onValueChange = {
                try { onUpdate(tax.copy(rate = BigDecimal(it))) } catch (e: Exception) {}
            },
            modifier = Modifier.width(80.dp),
            keyboardType = KeyboardType.Decimal,
            suffix = "%",
            enabled = tax.enabled
        )
        
        ToggleGroup(
            options = listOf("Exclude" to TaxMode.EXCLUDE, "Include" to TaxMode.INCLUDE),
            selected = tax.mode,
            onSelect = { onUpdate(tax.copy(mode = it)) },
            modifier = Modifier.width(120.dp),
            enabled = tax.enabled
        )
        
        ToggleGroup(
            options = listOf("Base" to false, "ToT" to true),
            selected = tax.taxOnTax,
            onSelect = { onUpdate(tax.copy(taxOnTax = it)) },
            modifier = Modifier.width(120.dp),
            enabled = tax.enabled
        )
        
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.Delete, "Delete", tint = AppColors.Red)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Receipt Panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ReceiptPanel(result: CalculationResult, fixedCharge: FixedCharge) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Title
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AppColors.Surface
        ) {
            Text(
                "RECEIPT",
                modifier = Modifier.padding(vertical = 16.dp, horizontal = 20.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = AppColors.Muted
            )
        }
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Items
            result.items.forEach { item ->
                ReceiptItem(item, result.allTaxes)
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Divider(color = AppColors.Border2, modifier = Modifier.padding(vertical = 8.dp))
            
            // Summary
            ReceiptRow("Gross Total", result.grossTotal)
            if (result.totalLineDiscount > BigDecimal.ZERO) {
                ReceiptRow("Line Discounts", result.totalLineDiscount, AppColors.Red, negative = true)
            }
            ReceiptRow("Subtotal (1)", result.subtotal1)
            
            if (result.totalInvoiceDiscount > BigDecimal.ZERO) {
                ReceiptRow("Invoice Discounts", result.totalInvoiceDiscount, AppColors.Purple, negative = true)
            }
            ReceiptRow("Subtotal (2)", result.subtotal2, fontWeight = FontWeight.Bold)
            
            if (result.inclusiveTaxTotal > BigDecimal.ZERO) {
                Divider(color = AppColors.Border2, modifier = Modifier.padding(vertical = 8.dp))
                ReceiptRow("Tax-Exclusive Base", result.inclusiveTaxBase, fontWeight = FontWeight.Bold)
            }
            
            // Taxes
            Divider(color = AppColors.Border2, modifier = Modifier.padding(vertical = 8.dp))
            
            result.allTaxes.forEach { tax ->
                val color = if (tax.mode == TaxMode.INCLUDE) AppColors.Amber else AppColors.Green
                ReceiptRow(
                    "${tax.name} (${tax.rate}% ${if (tax.mode == TaxMode.INCLUDE) "incl" else "excl"})" +
                    if (!tax.isGlobal) " [item]" else "" +
                    if (tax.taxOnTax) " ToT" else "",
                    tax.amount,
                    color,
                    positive = true
                )
            }
            
            if (result.inclusiveTaxTotal > BigDecimal.ZERO) {
                ReceiptRow("Incl Tax Total", result.inclusiveTaxTotal, AppColors.Amber, fontWeight = FontWeight.Bold)
            }
            if (result.exclusiveTaxTotal > BigDecimal.ZERO) {
                ReceiptRow("Excl Tax Total", result.exclusiveTaxTotal, AppColors.Green, fontWeight = FontWeight.Bold)
            }
            
            // Fixed Charge
            if (result.fixedChargeAmount > BigDecimal.ZERO) {
                Divider(color = AppColors.Border2, modifier = Modifier.padding(vertical = 8.dp))
                ReceiptRow(fixedCharge.label, result.fixedChargeAmount, AppColors.Teal, positive = true)
            }
            
            // Grand Total
            Divider(color = AppColors.Border2, thickness = 2.dp, modifier = Modifier.padding(vertical = 8.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "GRAND TOTAL",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Text
                )
                Text(
                    result.grandTotal.format(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = AppColors.Green
                )
            }
        }
    }
}

@Composable
fun ReceiptItem(item: ProcessedItem, allTaxes: List<TaxResult>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            // Item name and gross
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    item.item.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Text
                )
                Text(
                    item.gross.format(),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = AppColors.Muted
                )
            }
            
            Text(
                "${item.item.qty.stripTrailingZeros()} × ${item.item.unitPrice.format()}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = AppColors.Muted
            )
            
            // Line discounts
            item.lineDiscountBreakdown.forEachIndexed { index, disc ->
                ReceiptSubRow(
                    "Line disc ${index + 1} (${if (disc.discount.type == DiscountType.PERCENT) "${disc.discount.value}%" else "$"})",
                    disc.amount,
                    AppColors.Red,
                    negative = true
                )
            }
            
            // Invoice discounts
            item.invoiceDiscountBreakdown.forEachIndexed { index, disc ->
                ReceiptSubRow(
                    "Invoice disc ${index + 1} (${if (disc.discount.type == DiscountType.PERCENT) "${disc.discount.value}%" else "$"})",
                    disc.amount,
                    AppColors.Purple,
                    negative = true
                )
            }
            
            ReceiptSubRow("Tax-excl base", item.inclusiveTaxBase, AppColors.Text)
            
            // Per-item taxes
            item.allItemTaxes.forEach { (taxId, amount) ->
                if (amount > BigDecimal.ZERO) {
                    val tax = allTaxes.find { it.id == taxId }
                    val color = if (tax?.mode == TaxMode.INCLUDE) AppColors.Amber else AppColors.Green
                    ReceiptSubRow(
                        "${tax?.name} (${if (tax?.mode == TaxMode.INCLUDE) "incl" else "excl"})" +
                        if (tax?.isGlobal == false) " [item]" else "" +
                        if (tax?.taxOnTax == true) " ToT" else "",
                        amount,
                        color,
                        positive = true
                    )
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "→ Line Total",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Muted
                )
                Text(
                    item.lineTotal.format(),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Accent2
                )
            }
        }
    }
}

@Composable
fun ReceiptSubRow(
    label: String,
    value: BigDecimal,
    color: Color,
    negative: Boolean = false,
    positive: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = AppColors.Muted
        )
        Text(
            "${if (negative) "-" else if (positive) "+" else ""}${value.format()}",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = color
        )
    }
}

@Composable
fun ReceiptRow(
    label: String,
    value: BigDecimal,
    color: Color = AppColors.Text,
    fontWeight: FontWeight = FontWeight.Normal,
    negative: Boolean = false,
    positive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = AppColors.Muted
        )
        Text(
            "${if (negative) "-" else if (positive) "+" else ""}${value.format()}",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = fontWeight,
            color = color
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable UI Components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SectionCard(
    title: String,
    onAdd: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppColors.Card,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, AppColors.Border)
    ) {
        Column {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.02f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        color = AppColors.Muted
                    )
                    
                    if (onAdd != null) {
                        Button(
                            onClick = onAdd,
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SubSection(
    title: String,
    onAdd: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        color = Color.White.copy(alpha = 0.02f),
        shape = RoundedCornerShape(6.dp),
//        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Border2, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = AppColors.Muted
                )
                
                Button(
                    onClick = onAdd,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Surface,
                        contentColor = AppColors.Muted
                    ),
                    border = BorderStroke(1.dp, AppColors.Border2),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("+ Add", fontSize = 11.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            content()
        }
    }
}

@Composable
fun POSTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    prefix: String? = null,
    suffix: String? = null,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.then(if (!enabled) Modifier.alpha(0.4f) else Modifier),
        placeholder = { Text(placeholder, fontSize = 12.sp, color = AppColors.Muted) },
        prefix = prefix?.let { { Text(it, fontSize = 12.sp, color = AppColors.Muted) } },
        suffix = suffix?.let { { Text(it, fontSize = 12.sp, color = AppColors.Muted) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.Accent,
            unfocusedBorderColor = AppColors.Border2,
            focusedTextColor = AppColors.Text,
            unfocusedTextColor = AppColors.Text,
            disabledTextColor = AppColors.Muted,
            cursorColor = AppColors.Accent
        ),
        textStyle = LocalTextStyle.current.copy(
            fontSize = 13.sp,
            fontFamily = if (keyboardType == KeyboardType.Decimal) FontFamily.Monospace else FontFamily.Default
        ),
        shape = RoundedCornerShape(6.dp)
    )
}

@Composable
fun <T> ToggleGroup(
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier.then(if (!enabled) Modifier.alpha(0.4f) else Modifier),
        color = AppColors.Surface,
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(1.dp, AppColors.Border2)
    ) {
        Row {
            options.forEach { (label, value) ->
                val isSelected = selected == value
                Surface(
                    onClick = { if (enabled) onSelect(value) },
                    color = if (isSelected) AppColors.Accent else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        label,
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color.White else AppColors.Muted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
