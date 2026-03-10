package com.madhura.clodeposcal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madhura.clodeposcal.ui.theme.ClodePOSCalTheme
import java.math.BigDecimal
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// Activity
// ─────────────────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClodePOSCalTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    POSTaxCalculatorScreen()
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Color Palette
// ─────────────────────────────────────────────────────────────────────────────

object POSColors {
    val Background = Color(0xFF0E1117)
    val Surface    = Color(0xFF161B27)
    val Card       = Color(0xFF1E2535)
    val Border     = Color(0xFF2A3347)
    val Border2    = Color(0xFF344060)
    val Text       = Color(0xFFE8EAF0)
    val Muted      = Color(0xFF7C8599)
    val Accent     = Color(0xFF3B82F6)
    val Accent2    = Color(0xFF60A5FA)
    val Green      = Color(0xFF22C55E)
    val Red        = Color(0xFFEF4444)
    val Amber      = Color(0xFFF59E0B)
    val Purple     = Color(0xFFA855F7)
    val Teal       = Color(0xFF14B8A6)
    val Orange     = Color(0xFFF97316)
    val Pink       = Color(0xFFEC4899)
}

// ─────────────────────────────────────────────────────────────────────────────
// Root Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun POSTaxCalculatorScreen() {

    // ── Line-discount catalogue ───────────────────────────────────────────────
    var lineDiscounts by remember {
        mutableStateOf(
            listOf(
                Discount(UUID.randomUUID().toString(), "Nexus 25%",    DiscountType.PERCENT, BigDecimal("25")),
                Discount(UUID.randomUUID().toString(), "Quick deal 30%", DiscountType.PERCENT, BigDecimal("30")),
            )
        )
    }

    // ── Tax catalogue ─────────────────────────────────────────────────────────
    var taxes by remember {
        mutableStateOf(
            listOf(
                Tax("inc1",    "Inc1",    BigDecimal("5"),  TaxMode.INCLUDE, TaxOrder.BEFORE),
                Tax("inc2",    "Inc2",    BigDecimal("10"), TaxMode.INCLUDE, TaxOrder.BEFORE),
                Tax("service", "Service", BigDecimal("10"), TaxMode.EXCLUDE, TaxOrder.BEFORE),
                Tax("gst",     "GST",     BigDecimal("18"), TaxMode.EXCLUDE, TaxOrder.BEFORE)
            )
        )
    }

    // ── Items — appliedDiscounts / appliedTaxes are now embedded objects ───────
    var items by remember {
        mutableStateOf(
            listOf(
                Item(
                    id = "1", name = "Ambewela Non-Fat Milk Uht Tetra 1L",
                    qty = BigDecimal("1"), unitPrice = BigDecimal("390.00"),
                    appliedDiscounts = emptyList(),
                    appliedTaxes     = listOf(
                        Tax("inc1",    "Inc1",    BigDecimal("5"),  TaxMode.INCLUDE, TaxOrder.BEFORE),
                        Tax("inc2",    "Inc2",    BigDecimal("10"), TaxMode.INCLUDE, TaxOrder.BEFORE),
                        Tax("service", "Service", BigDecimal("10"), TaxMode.EXCLUDE, TaxOrder.BEFORE),
                        Tax("gst",     "GST",     BigDecimal("18"), TaxMode.EXCLUDE, TaxOrder.BEFORE)
                    )
                ),
                Item(
                    id = "2", name = "Elephant House Cream Soda Pet Bottle 1.5L",
                    qty = BigDecimal("1"), unitPrice = BigDecimal("315.00"),
                    appliedDiscounts = emptyList(),
                    appliedTaxes     = listOf(
                        Tax("inc1",    "Inc1",    BigDecimal("5"),  TaxMode.INCLUDE, TaxOrder.BEFORE),
                        Tax("inc2",    "Inc2",    BigDecimal("10"), TaxMode.INCLUDE, TaxOrder.BEFORE),
                        Tax("service", "Service", BigDecimal("10"), TaxMode.EXCLUDE, TaxOrder.BEFORE),
                        Tax("gst",     "GST",     BigDecimal("18"), TaxMode.EXCLUDE, TaxOrder.BEFORE)
                    )
                ),
                Item(
                    id = "3", name = "Keells Drinking Water Pet 500ml",
                    qty = BigDecimal("1"), unitPrice = BigDecimal("70.00"),
                    appliedDiscounts = emptyList(),
                    appliedTaxes     = listOf(
                        Tax("inc1",    "Inc1",    BigDecimal("5"),  TaxMode.INCLUDE, TaxOrder.BEFORE),
                        Tax("inc2",    "Inc2",    BigDecimal("10"), TaxMode.INCLUDE, TaxOrder.BEFORE),
                        Tax("service", "Service", BigDecimal("10"), TaxMode.EXCLUDE, TaxOrder.BEFORE),
                        Tax("gst",     "GST",     BigDecimal("18"), TaxMode.EXCLUDE, TaxOrder.BEFORE)
                    )
                )
            )
        )
    }

    // ── Receipt discounts ─────────────────────────────────────────────────────
    var receiptDiscounts by remember {
        mutableStateOf(
            listOf(
                Discount(UUID.randomUUID().toString(), "R - Nexus",       DiscountType.PERCENT, 25.toBigDecimal()),
                Discount(UUID.randomUUID().toString(), "R - Quick deal",   DiscountType.PERCENT, 30.toBigDecimal()),
                Discount(UUID.randomUUID().toString(), "E - Re-usable Bag", DiscountType.FIXED,   50.toBigDecimal())
            )
        )
    }

    // ── Fixed charges ─────────────────────────────────────────────────────────
    var fixedCharges by remember {
        mutableStateOf(listOf(FixedCharge(UUID.randomUUID().toString(), "Service Fee", BigDecimal.ZERO)))
    }

    // ── Reactive calculation ──────────────────────────────────────────────────
    val result = remember(items, lineDiscounts, taxes, receiptDiscounts, fixedCharges) {
        ReceiptCalculator(items, lineDiscounts, taxes, receiptDiscounts, fixedCharges).calculate()
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary      = POSColors.Accent,
            background   = POSColors.Background,
            surface      = POSColors.Surface,
            onPrimary    = Color.White,
            onBackground = POSColors.Text,
            onSurface    = POSColors.Text
        )
    ) {
        Scaffold(containerColor = POSColors.Background) { padding ->
            Row(modifier = Modifier.fillMaxSize().padding(padding)) {

                // Left — inputs
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(POSColors.Background)) {
                    InputPanel(
                        items            = items,
                        lineDiscounts    = lineDiscounts,
                        taxes            = taxes,
                        receiptDiscounts = receiptDiscounts,
                        fixedCharges     = fixedCharges,
                        // ── line-discount catalogue CRUD ──
                        onAddLineDiscount    = {
                            lineDiscounts = lineDiscounts + Discount(UUID.randomUUID().toString(), "", DiscountType.PERCENT, BigDecimal.ZERO)
                        },
                        onUpdateLineDiscount = { id, d ->
                            // Update catalogue entry AND propagate change to all items that carry it
                            lineDiscounts = lineDiscounts.map { if (it.id == id) d else it }
                            items = items.map { item ->
                                item.copy(appliedDiscounts = item.appliedDiscounts.map { if (it.id == id) d else it })
                            }
                        },
                        onDeleteLineDiscount = { id ->
                            lineDiscounts = lineDiscounts.filter { it.id != id }
                            items = items.map { item ->
                                item.copy(appliedDiscounts = item.appliedDiscounts.filter { it.id != id })
                            }
                        },
                        // ── item CRUD ──
                        onAddItem    = {
                            items = items + Item(UUID.randomUUID().toString(), "", BigDecimal.ONE, BigDecimal.ZERO)
                        },
                        onUpdateItem = { id, updated -> items = items.map { if (it.id == id) updated else it } },
                        onDeleteItem = { id -> items = items.filter { it.id != id } },
                        // ── tax catalogue CRUD ──
                        onAddTax  = {
                            taxes = taxes + Tax(UUID.randomUUID().toString(), "", BigDecimal.ZERO, TaxMode.EXCLUDE, TaxOrder.BEFORE)
                        },
                        onUpdateTax  = { id, t ->
                            taxes = taxes.map { if (it.id == id) t else it }
                            items = items.map { item ->
                                item.copy(appliedTaxes = item.appliedTaxes.map { if (it.id == id) t else it })
                            }
                        },
                        onDeleteTax  = { id ->
                            taxes = taxes.filter { it.id != id }
                            items = items.map { item ->
                                item.copy(appliedTaxes = item.appliedTaxes.filter { it.id != id })
                            }
                        },
                        // ── receipt discount CRUD ──
                        onAddReceiptDiscount    = {
                            receiptDiscounts = receiptDiscounts + Discount(UUID.randomUUID().toString(), "", DiscountType.PERCENT, BigDecimal.ZERO)
                        },
                        onUpdateReceiptDiscount = { id, it -> receiptDiscounts = receiptDiscounts.map { d -> if (d.id == id) it else d } },
                        onDeleteReceiptDiscount = { id -> receiptDiscounts = receiptDiscounts.filter { it.id != id } },
                        // ── fixed charge CRUD ──
                        onAddFixedCharge    = {
                            fixedCharges = fixedCharges + FixedCharge(UUID.randomUUID().toString(), "Charge", BigDecimal.ZERO)
                        },
                        onUpdateFixedCharge = { id, it -> fixedCharges = fixedCharges.map { fc -> if (fc.id == id) it else fc } },
                        onDeleteFixedCharge = { id -> fixedCharges = fixedCharges.filter { it.id != id } }
                    )
                }

                // Right — receipt
                Box(modifier = Modifier.width(440.dp).fillMaxHeight().background(POSColors.Surface)) {
                    ReceiptPanel(result)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Input Panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun InputPanel(
    items: List<Item>,
    lineDiscounts: List<Discount>,
    taxes: List<Tax>,
    receiptDiscounts: List<Discount>,
    fixedCharges: List<FixedCharge>,
    onAddLineDiscount: () -> Unit,
    onUpdateLineDiscount: (String, Discount) -> Unit,
    onDeleteLineDiscount: (String) -> Unit,
    onAddItem: () -> Unit,
    onUpdateItem: (String, Item) -> Unit,
    onDeleteItem: (String) -> Unit,
    onAddTax: () -> Unit,
    onUpdateTax: (String, Tax) -> Unit,
    onDeleteTax: (String) -> Unit,
    onAddReceiptDiscount: () -> Unit,
    onUpdateReceiptDiscount: (String, Discount) -> Unit,
    onDeleteReceiptDiscount: (String) -> Unit,
    onAddFixedCharge: () -> Unit,
    onUpdateFixedCharge: (String, FixedCharge) -> Unit,
    onDeleteFixedCharge: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(color = POSColors.Accent, shape = RoundedCornerShape(4.dp)) {
                        Text("POS", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.8.sp)
                    }
                    Text("Tax Calculator", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = POSColors.Text)
                    Surface(color = POSColors.Surface, shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, POSColors.Border)) {
                        Text("🔢 Sequential Discounts + Aggregate Tax",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 10.sp, color = POSColors.Muted)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                FlowIndicator()
            }
        }

        // ① Line Discount Catalogue
        item {
            SectionCard(title = "① Line Discount Catalogue", accentColor = POSColors.Orange, onAdd = onAddLineDiscount) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (lineDiscounts.isEmpty())
                        Text("No line discounts defined", fontSize = 12.sp, color = POSColors.Muted)
                    lineDiscounts.forEach { disc ->
                        CatalogueDiscountRow(
                            discount = disc,
                            onUpdate = { onUpdateLineDiscount(disc.id, it) },
                            onDelete = { onDeleteLineDiscount(disc.id) }
                        )
                    }
                }
            }
        }

        // ② Tax Catalogue
        item {
            SectionCard(title = "② Tax Catalogue", accentColor = POSColors.Green, onAdd = onAddTax) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (taxes.isEmpty())
                        Text("No taxes defined", fontSize = 12.sp, color = POSColors.Muted)
                    taxes.forEach { tax ->
                        TaxRow(tax = tax, onUpdate = { onUpdateTax(tax.id, it) }, onDelete = { onDeleteTax(tax.id) })
                    }
                }
            }
        }

        // ③ Items
        item {
            SectionCard(title = "③ Items", accentColor = POSColors.Accent, onAdd = onAddItem) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("NAME",  modifier = Modifier.weight(1f),   style = ColumnHeaderStyle)
                        Text("QTY",   modifier = Modifier.width(70.dp), style = ColumnHeaderStyle, textAlign = TextAlign.End)
                        Text("PRICE", modifier = Modifier.width(90.dp), style = ColumnHeaderStyle, textAlign = TextAlign.End)
                        Text("GROSS", modifier = Modifier.width(80.dp), style = ColumnHeaderStyle, textAlign = TextAlign.End)
                        Spacer(modifier = Modifier.width(36.dp))
                    }
                    items.forEach { item ->
                        ItemCard(
                            item         = item,
                            allDiscounts = lineDiscounts,
                            allTaxes     = taxes,
                            onUpdate     = { onUpdateItem(item.id, it) },
                            onDelete     = { onDeleteItem(item.id) }
                        )
                    }
                }
            }
        }

        // ④ Receipt Discounts
        item {
            SectionCard(title = "④ Receipt Discounts", accentColor = POSColors.Purple, onAdd = onAddReceiptDiscount) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (receiptDiscounts.isEmpty())
                        Text("No receipt discounts", fontSize = 12.sp, color = POSColors.Muted)
                    receiptDiscounts.forEach { disc ->
                        CatalogueDiscountRow(
                            discount = disc,
                            onUpdate = { onUpdateReceiptDiscount(disc.id, it) },
                            onDelete = { onDeleteReceiptDiscount(disc.id) }
                        )
                    }
                    if (receiptDiscounts.size > 1)
                        Text("⚡ Applied sequentially — each taken from the running total after the previous discount",
                            fontSize = 10.sp, color = POSColors.Amber)
                }
            }
        }

        // ⑤ Fixed Charges
        item {
            SectionCard(title = "⑤ Fixed Charges", accentColor = POSColors.Teal, onAdd = onAddFixedCharge) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (fixedCharges.isEmpty())
                        Text("No fixed charges", fontSize = 12.sp, color = POSColors.Muted)
                    fixedCharges.forEach { fc ->
                        FixedChargeRow(charge = fc, onUpdate = { onUpdateFixedCharge(fc.id, it) }, onDelete = { onDeleteFixedCharge(fc.id) })
                    }
                }
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
    allDiscounts: List<Discount>,
    allTaxes: List<Tax>,
    onUpdate: (Item) -> Unit,
    onDelete: () -> Unit
) {
    // Gross = (unitPrice × qty) + Σ(item.qty × modifier.qty × modifier.price)
    val modifierTotal = item.modifiers.fold(BigDecimal.ZERO) { acc, mod ->
        acc + (item.qty * mod.qty * mod.price).setScale(2, java.math.RoundingMode.HALF_UP)
    }
    val gross = (item.qty * item.unitPrice)
        .setScale(2, java.math.RoundingMode.HALF_UP) + modifierTotal

    Surface(
        color  = POSColors.Background,
        shape  = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, POSColors.Border)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // ── Name / Qty / Price / Gross / Delete ───────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                POSTextField(
                    value         = item.name,
                    onValueChange = { onUpdate(item.copy(name = it)) },
                    modifier      = Modifier.weight(1f),
                    placeholder   = "Item name"
                )
                POSTextField(
                    value         = item.qty.toPlainString(),
                    onValueChange = { runCatching { onUpdate(item.copy(qty = BigDecimal(it))) } },
                    modifier      = Modifier.width(70.dp),
                    keyboardType  = KeyboardType.Decimal,
                    textAlign     = TextAlign.End
                )
                POSTextField(
                    value         = item.unitPrice.toPlainString(),
                    onValueChange = { runCatching { onUpdate(item.copy(unitPrice = BigDecimal(it))) } },
                    modifier      = Modifier.width(90.dp),
                    keyboardType  = KeyboardType.Decimal,
                    textAlign     = TextAlign.End
                )
                Text(
                    gross.toPlainString(),
                    modifier  = Modifier.width(80.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize   = 12.sp,
                    color      = POSColors.Text,
                    textAlign  = TextAlign.End
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, "Delete", tint = POSColors.Red, modifier = Modifier.size(18.dp))
                }
            }

            // ── Modifiers ─────────────────────────────────────────────────────
            ModifierPanel(
                modifiers = item.modifiers,
                itemQty   = item.qty,
                onAddModifier = {
                    onUpdate(item.copy(modifiers = item.modifiers + Modifier(UUID.randomUUID().toString(), "", BigDecimal.ONE, BigDecimal.ZERO)))
                },
                onUpdateModifier = { id, mod ->
                    onUpdate(item.copy(modifiers = item.modifiers.map { if (it.id == id) mod else it }))
                },
                onDeleteModifier = { id ->
                    onUpdate(item.copy(modifiers = item.modifiers.filter { it.id != id }))
                }
            )

            // ── Line-discount checkboxes ───────────────────────────────────────
            // checked  = discount is in item.appliedDiscounts
            // onToggle = add or remove the full Discount object
            CheckboxPanel(
                title       = "LINE DISCOUNTS",
                emptyLabel  = "No discounts defined in catalogue",
                badgeLabel  = "No Discount",
                badgeColor  = POSColors.Orange,
                isEmpty     = item.appliedDiscounts.isEmpty(),
                accentColor = POSColors.Orange,
                rows        = allDiscounts.map { disc ->
                    CheckboxRow(
                        id      = disc.id,
                        checked = item.appliedDiscounts.any { it.id == disc.id },
                        label   = disc.label.ifBlank { "Unnamed discount" },
                        detail  = "${disc.value.stripTrailingZeros().toPlainString()}${if (disc.type == DiscountType.PERCENT) "%" else "$"} off"
                    )
                },
                onToggle = { id, checked ->
                    val catalogue = allDiscounts.firstOrNull { it.id == id } ?: return@CheckboxPanel
                    val updated = if (checked)
                        item.appliedDiscounts + catalogue
                    else
                        item.appliedDiscounts.filter { it.id != id }
                    onUpdate(item.copy(appliedDiscounts = updated))
                }
            )

            // ── Tax checkboxes ────────────────────────────────────────────────
            CheckboxPanel(
                title       = "TAXES APPLIED",
                emptyLabel  = "No taxes defined in catalogue",
                badgeLabel  = "Tax-Free",
                badgeColor  = POSColors.Amber,
                isEmpty     = item.appliedTaxes.isEmpty(),
                accentColor = POSColors.Green,
                rows        = allTaxes.map { tax ->
                    CheckboxRow(
                        id      = tax.id,
                        checked = item.appliedTaxes.any { it.id == tax.id },
                        label   = tax.name.ifBlank { "Unnamed tax" },
                        detail  = "${tax.rate.stripTrailingZeros().toPlainString()}% " +
                                "${if (tax.mode == TaxMode.INCLUDE) "(incl)" else "(excl)"} " +
                                "${if (tax.taxOrder == TaxOrder.AFTER) "⚡ToT" else ""}"
                    )
                },
                onToggle = { id, checked ->
                    val catalogue = allTaxes.firstOrNull { it.id == id } ?: return@CheckboxPanel
                    val updated = if (checked)
                        item.appliedTaxes + catalogue
                    else
                        item.appliedTaxes.filter { it.id != id }
                    onUpdate(item.copy(appliedTaxes = updated))
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Modifier Panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ModifierPanel(
    modifiers: List<com.madhura.clodeposcal.Modifier>,
    itemQty: BigDecimal,
    onAddModifier: () -> Unit,
    onUpdateModifier: (String, com.madhura.clodeposcal.Modifier) -> Unit,
    onDeleteModifier: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(POSColors.Surface.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "MODIFIERS",
                fontSize = 9.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp, color = POSColors.Muted
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (modifiers.isNotEmpty()) {
                    val modTotal = modifiers.fold(BigDecimal.ZERO) { acc, m ->
                        acc + (itemQty * m.qty * m.price).setScale(2, java.math.RoundingMode.HALF_UP)
                    }
                    Surface(color = POSColors.Pink.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            "+${modTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            color = POSColors.Pink, fontFamily = FontFamily.Monospace
                        )
                    }
                }
                TextButton(
                    onClick       = onAddModifier,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier      = Modifier.height(24.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(12.dp), tint = POSColors.Pink)
                    Spacer(Modifier.width(2.dp))
                    Text("Add", fontSize = 10.sp, color = POSColors.Pink, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (modifiers.isEmpty()) {
            Text("No modifiers", fontSize = 11.sp, color = POSColors.Muted)
        } else {
            // Column headers
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("NAME",  modifier = Modifier.weight(1f),   style = ColumnHeaderStyle)
                Text("QTY",   modifier = Modifier.width(60.dp), style = ColumnHeaderStyle, textAlign = TextAlign.End)
                Text("PRICE", modifier = Modifier.width(90.dp), style = ColumnHeaderStyle, textAlign = TextAlign.End)
                Text("TOTAL", modifier = Modifier.width(70.dp), style = ColumnHeaderStyle, textAlign = TextAlign.End)
                Spacer(modifier = Modifier.width(36.dp))
            }
            modifiers.forEach { mod ->
                ModifierRow(
                    mod = mod,
                    itemQty  = itemQty,
                    onUpdate = { onUpdateModifier(mod.id, it) },
                    onDelete = { onDeleteModifier(mod.id) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Modifier Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ModifierRow(
    mod: com.madhura.clodeposcal.Modifier,
    itemQty: BigDecimal,
    onUpdate: (com.madhura.clodeposcal.Modifier) -> Unit,
    onDelete: () -> Unit
) {
    // lineTotal = itemQty × mod.qty × mod.price
    val lineTotal = (itemQty * mod.qty * mod.price)
        .setScale(2, java.math.RoundingMode.HALF_UP)

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        POSTextField(
            value         = mod.name,
            onValueChange = { onUpdate(mod.copy(name = it)) },
            modifier      = Modifier.weight(1f),
            placeholder   = "Modifier name"
        )
        POSTextField(
            value         = mod.qty.toPlainString(),
            onValueChange = { runCatching { onUpdate(mod.copy(qty = BigDecimal(it))) } },
            modifier      = Modifier.width(60.dp),
            keyboardType  = KeyboardType.Decimal,
            textAlign     = TextAlign.End
        )
        POSTextField(
            value         = mod.price.toPlainString(),
            onValueChange = { runCatching { onUpdate(mod.copy(price = BigDecimal(it))) } },
            modifier      = Modifier.width(90.dp),
            keyboardType  = KeyboardType.Decimal,
            textAlign     = TextAlign.End
        )
        Text(
            lineTotal.toPlainString(),
            modifier  = Modifier.width(70.dp),
            fontFamily = FontFamily.Monospace,
            fontSize   = 12.sp,
            color      = POSColors.Pink,
            textAlign  = TextAlign.End
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Delete, "Delete", tint = POSColors.Red, modifier = Modifier.size(18.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Checkbox Panel
// ─────────────────────────────────────────────────────────────────────────────

data class CheckboxRow(val id: String, val checked: Boolean, val label: String, val detail: String)

@Composable
fun CheckboxPanel(
    title: String,
    emptyLabel: String,
    badgeLabel: String,
    badgeColor: Color,
    isEmpty: Boolean,
    accentColor: Color,
    rows: List<CheckboxRow>,
    onToggle: (String, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(POSColors.Surface.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = POSColors.Muted)
            if (isEmpty) {
                Surface(color = badgeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                    Text(badgeLabel, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 9.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                }
            }
        }
        if (rows.isEmpty()) {
            Text(emptyLabel, fontSize = 11.sp, color = POSColors.Muted)
        } else {
            rows.forEach { row ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    Checkbox(
                        checked         = row.checked,
                        onCheckedChange = { onToggle(row.id, it) },
                        modifier        = Modifier.size(20.dp),
                        colors          = CheckboxDefaults.colors(
                            checkedColor   = accentColor,
                            uncheckedColor = POSColors.Border2
                        )
                    )
                    Text(row.label, fontSize = 12.sp, color = POSColors.Text, modifier = Modifier.weight(1f))
                    Text(row.detail, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = POSColors.Muted)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Catalogue Discount Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CatalogueDiscountRow(discount: Discount, onUpdate: (Discount) -> Unit, onDelete: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        POSTextField(value = discount.label, onValueChange = { onUpdate(discount.copy(label = it)) },
            modifier = Modifier.weight(1f), placeholder = "Label")
        ToggleGroup(
            options  = listOf("%" to DiscountType.PERCENT, "$" to DiscountType.FIXED),
            selected = discount.type,
            onSelect = { onUpdate(discount.copy(type = it)) },
            modifier = Modifier.width(90.dp)
        )
        POSTextField(
            value         = discount.value.toPlainString(),
            onValueChange = { runCatching { onUpdate(discount.copy(value = BigDecimal(it))) } },
            modifier      = Modifier.width(100.dp),
            keyboardType  = KeyboardType.Decimal,
            suffix        = if (discount.type == DiscountType.PERCENT) "%" else "$",
            textAlign     = TextAlign.End
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Delete, "Delete", tint = POSColors.Red, modifier = Modifier.size(18.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tax Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TaxRow(tax: Tax, onUpdate: (Tax) -> Unit, onDelete: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            POSTextField(
                value         = tax.name,
                onValueChange = { onUpdate(tax.copy(name = it)) },
                modifier      = Modifier.weight(1f),
                placeholder   = "Tax name"
            )
            POSTextField(
                value         = tax.rate.toPlainString(),
                onValueChange = { runCatching { onUpdate(tax.copy(rate = BigDecimal(it))) } },
                modifier      = Modifier.width(80.dp),
                keyboardType  = KeyboardType.Decimal,
                suffix        = "%",
                textAlign     = TextAlign.End
            )
            ToggleGroup(
                options  = listOf("Excl" to TaxMode.EXCLUDE, "Incl" to TaxMode.INCLUDE),
                selected = tax.mode,
                onSelect = { onUpdate(tax.copy(mode = it)) },
                modifier = Modifier.width(120.dp)
            )
            ToggleGroup(
                options  = listOf("Before" to TaxOrder.BEFORE, "After" to TaxOrder.AFTER),
                selected = tax.taxOrder,
                onSelect = { onUpdate(tax.copy(taxOrder = it)) },
                modifier = Modifier.width(140.dp)
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, "Delete", tint = POSColors.Red, modifier = Modifier.size(18.dp))
            }
        }
        if (tax.taxOrder == TaxOrder.AFTER)
            Text(
                "⚡ Tax-on-Tax: base = afterReceiptDiscount + previous EXCLUDE taxes",
                fontSize = 10.sp, color = POSColors.Amber, modifier = Modifier.padding(start = 8.dp)
            )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Fixed Charge Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FixedChargeRow(charge: FixedCharge, onUpdate: (FixedCharge) -> Unit, onDelete: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        POSTextField(value = charge.label, onValueChange = { onUpdate(charge.copy(label = it)) },
            modifier = Modifier.weight(1f), placeholder = "Label")
        POSTextField(
            value         = charge.value.toPlainString(),
            onValueChange = { runCatching { onUpdate(charge.copy(value = BigDecimal(it))) } },
            modifier      = Modifier.width(120.dp),
            keyboardType  = KeyboardType.Decimal,
            prefix        = "$",
            textAlign     = TextAlign.End
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Delete, "Delete", tint = POSColors.Red, modifier = Modifier.size(18.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Receipt Panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ReceiptPanel(result: CalculationResult) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        Box(modifier = Modifier.fillMaxWidth().background(POSColors.Surface)
            .padding(vertical = 16.dp, horizontal = 20.dp)) {
            Text("RECEIPT", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp, color = POSColors.Muted)
        }

        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {

            result.items.forEach { item -> ReceiptItem(item) }

            Divider(color = POSColors.Border2, modifier = Modifier.padding(vertical = 8.dp))

            ReceiptRow("Gross Total", result.grossTotal, POSColors.Accent2)

            if (result.totalLineDiscount > BigDecimal.ZERO)
                ReceiptRow("Line Discounts", result.totalLineDiscount, POSColors.Red, negative = true)

            ReceiptRow("Subtotal (1) — after line discounts", result.subtotal1)

            result.receiptDiscountAmounts.forEach { (disc, amount) ->
                if (amount > BigDecimal.ZERO) {
                    val suffix = if (disc.type == DiscountType.PERCENT) " (${disc.value.stripTrailingZeros().toPlainString()}%)" else ""
                    ReceiptRow("${disc.label.ifBlank { "Receipt Discount" }}$suffix", amount, POSColors.Purple, negative = true)
                }
            }

            if (result.totalReceiptDiscountAmount > BigDecimal.ZERO)
                ReceiptRow("Subtotal (2) — after receipt discounts", result.subtotal2, fontWeight = FontWeight.Bold)

            if (result.taxResults.isNotEmpty()) {
                Divider(color = POSColors.Border2, modifier = Modifier.padding(vertical = 8.dp))
                result.taxResults.forEach { taxResult ->
                    val color    = if (taxResult.tax.mode == TaxMode.INCLUDE) POSColors.Amber else POSColors.Green
                    val modeTag  = if (taxResult.tax.mode == TaxMode.INCLUDE) "incl" else "excl"
                    val orderTag = if (taxResult.tax.taxOrder == TaxOrder.AFTER) " ⚡ToT" else ""
                    ReceiptRow(
                        label    = "${taxResult.tax.name} (${taxResult.tax.rate.stripTrailingZeros().toPlainString()}% $modeTag$orderTag)",
                        value    = taxResult.amount,
                        color    = color,
                        positive = taxResult.tax.mode == TaxMode.EXCLUDE
                    )
                }
                if (result.exclusiveTaxTotal > BigDecimal.ZERO)
                    ReceiptRow("Total Exclusive Tax", result.exclusiveTaxTotal, POSColors.Green, fontWeight = FontWeight.Bold, positive = true)
                if (result.inclusiveTaxTotal > BigDecimal.ZERO)
                    ReceiptRow("Total Inclusive Tax (embedded)", result.inclusiveTaxTotal, POSColors.Amber)
            }

            if (result.fixedCharges.isNotEmpty()) {
                Divider(color = POSColors.Border2, modifier = Modifier.padding(vertical = 8.dp))
                result.fixedCharges.forEach { (fc, amount) ->
                    if (amount > BigDecimal.ZERO)
                        ReceiptRow(fc.label, amount, POSColors.Teal, positive = true)
                }
                if (result.totalFixedChargeAmount > BigDecimal.ZERO)
                    ReceiptRow("Total Fixed Charges", result.totalFixedChargeAmount, POSColors.Teal, fontWeight = FontWeight.Bold, positive = true)
            }

            Divider(color = POSColors.Border2, thickness = 2.dp, modifier = Modifier.padding(vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("GRAND TOTAL", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = POSColors.Text)
                Text(result.grandTotal.fmt(), fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace, color = POSColors.Green)
            }

            BalanceVerificationPanel(result)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Receipt Item breakdown
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ReceiptItem(item: ProcessedItem) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(item.item.name.ifBlank { "—" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = POSColors.Text)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (item.item.appliedTaxes.isEmpty()) {
                    Surface(color = POSColors.Amber.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)) {
                        Text("Tax-Free", modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            fontSize = 9.sp, fontWeight = FontWeight.Bold, color = POSColors.Amber)
                    }
                }
                Text(item.gross.fmt(), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = POSColors.Muted)
            }
        }

        Column(modifier = Modifier.padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("${item.item.qty.stripTrailingZeros()} × ${item.item.unitPrice.fmt()}",
                fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = POSColors.Muted)

            // Modifiers
            if (item.item.modifiers.isNotEmpty()) {
                item.item.modifiers.forEach { mod ->
                    val lineTotal = (item.item.qty * mod.qty * mod.price)
                        .setScale(2, java.math.RoundingMode.HALF_UP)
                    SubRow(
                        label    = "+ ${mod.name.ifBlank { "Modifier" }} (${item.item.qty.stripTrailingZeros()} × ${mod.qty.stripTrailingZeros()} × ${mod.price.fmt()})",
                        value    = lineTotal,
                        color    = POSColors.Pink,
                        positive = true
                    )
                }
                if (item.modifierTotal > BigDecimal.ZERO)
                    SubRow("Modifier total", item.modifierTotal, POSColors.Pink)
            }

            // Line discounts
            item.lineDiscountBreakdown.forEach { (disc, amount) ->
                if (amount > BigDecimal.ZERO)
                    SubRow("${disc.label.ifBlank { "Disc" }} (running base)", amount, POSColors.Orange, negative = true)
            }
            if (item.totalLineDiscount > BigDecimal.ZERO)
                SubRow("After line disc", item.netAfterLine, POSColors.Orange)

            // Receipt discount share
            if (item.receiptDiscountShare > BigDecimal.ZERO)
                SubRow("Receipt disc (LR alloc)", item.receiptDiscountShare, POSColors.Purple, negative = true)

            SubRow("After receipt disc (incl-tax price)", item.afterReceiptDiscount, POSColors.Muted)

            if (item.afterReceiptDiscount != item.inclusiveTaxBase)
                SubRow("Net of incl taxes (excl-tax base)", item.inclusiveTaxBase, POSColors.Accent2)

            item.taxLines.forEach { line ->
                if (line.taxAmount > BigDecimal.ZERO) {
                    val color    = if (line.taxMode == TaxMode.INCLUDE) POSColors.Amber else POSColors.Green
                    val modeTag  = if (line.taxMode == TaxMode.INCLUDE) "incl" else "excl"
                    val orderTag = if (line.taxOrder == TaxOrder.AFTER) " ⚡ToT" else ""
                    val baseTag  = " [base ${line.taxableBase.fmt()}]"
                    SubRow(
                        label    = "${line.name} ${line.ratePercent.stripTrailingZeros().toPlainString()}% $modeTag$orderTag$baseTag",
                        value    = line.taxAmount,
                        color    = color,
                        positive = line.taxMode == TaxMode.EXCLUDE
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("net (excl-tax base)", fontSize = 10.sp, color = POSColors.Muted, fontFamily = FontFamily.Monospace)
                Text(item.totalExclTax.fmt(), fontSize = 10.sp, color = POSColors.Muted, fontFamily = FontFamily.Monospace)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("tax total (incl+excl)", fontSize = 10.sp, color = POSColors.Muted, fontFamily = FontFamily.Monospace)
                Text(item.totalTax.fmt(), fontSize = 10.sp, color = POSColors.Muted, fontFamily = FontFamily.Monospace)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("→ line total (net + excl taxes)", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace, color = POSColors.Accent2)
                Text(item.totalInclTax.fmt(), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace, color = POSColors.Accent2)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Balance Verification Panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BalanceVerificationPanel(result: CalculationResult) {
    val itemInclTaxSum = result.items.sumOfDecimal { it.totalInclTax }
    val grandTotalCheck = (itemInclTaxSum + result.totalFixedChargeAmount)
        .setScale(2, java.math.RoundingMode.HALF_UP)
    val receiptDiscCheck = result.items.sumOfDecimal { it.receiptDiscountShare }

    val grandMatch   = grandTotalCheck.compareTo(result.grandTotal) == 0
    val receiptMatch = receiptDiscCheck.compareTo(result.totalReceiptDiscountAmount) == 0

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color    = POSColors.Teal.copy(alpha = 0.07f),
        shape    = RoundedCornerShape(8.dp),
        border   = androidx.compose.foundation.BorderStroke(1.dp, POSColors.Teal.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("✓ BALANCE VERIFICATION", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp, color = POSColors.Teal)
            VerifyRow("Σ item.totalInclTax + fixed", grandTotalCheck, result.grandTotal, grandMatch, "grandTotal")
            VerifyRow("Σ receiptDiscShare (LR)", receiptDiscCheck, result.totalReceiptDiscountAmount, receiptMatch, "totalReceiptDisc")
            result.taxResults.forEach { taxResult ->
                val itemSum = result.items.sumOfDecimal { item ->
                    item.taxLines.find { it.taxId == taxResult.tax.id }?.taxAmount ?: BigDecimal.ZERO
                }
                val match = itemSum.compareTo(taxResult.amount) == 0
                VerifyRow("${taxResult.tax.name} Σ items", itemSum, taxResult.amount, match, "receipt total")
            }
        }
    }
}

@Composable
fun VerifyRow(label: String, lhs: BigDecimal, rhs: BigDecimal, match: Boolean, rhsLabel: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = POSColors.Muted,
            modifier = Modifier.weight(1f))
        Text(
            "${lhs.fmt()} = ${rhs.fmt()} ($rhsLabel) ${if (match) "✅" else "⚠️"}",
            fontSize = 11.sp, fontFamily = FontFamily.Monospace,
            color    = if (match) POSColors.Teal else POSColors.Red
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable UI Components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SubRow(label: String, value: BigDecimal, color: Color, negative: Boolean = false, positive: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = POSColors.Muted)
        Text(
            "${if (negative) "-" else if (positive) "+" else ""}${value.fmt()}",
            fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = color
        )
    }
}

@Composable
fun ReceiptRow(
    label: String,
    value: BigDecimal,
    color: Color = POSColors.Text,
    fontWeight: FontWeight = FontWeight.Normal,
    negative: Boolean = false,
    positive: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, color = POSColors.Muted, modifier = Modifier.weight(1f))
        Text(
            "${if (negative) "-" else if (positive) "+" else ""}${value.fmt()}",
            fontSize   = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = fontWeight,
            color      = color
        )
    }
}

@Composable
fun SectionCard(
    title: String,
    accentColor: Color = POSColors.Accent,
    onAdd: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color    = POSColors.Card,
        shape    = RoundedCornerShape(10.dp),
        border   = androidx.compose.foundation.BorderStroke(1.dp, POSColors.Border)
    ) {
        Column {
            Surface(modifier = Modifier.fillMaxWidth(), color = accentColor.copy(alpha = 0.05f)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically) {
                    Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp, color = accentColor)
                    if (onAdd != null) {
                        Button(
                            onClick        = onAdd,
                            colors         = ButtonDefaults.buttonColors(containerColor = accentColor),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                            modifier       = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(16.dp)) { content() }
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
    textAlign: TextAlign = TextAlign.Start
) {
    OutlinedTextField(
        value           = value,
        onValueChange   = onValueChange,
        modifier        = modifier,
        placeholder     = { Text(placeholder, fontSize = 12.sp, color = POSColors.Muted) },
        prefix          = prefix?.let { { Text(it, fontSize = 12.sp, color = POSColors.Muted) } },
        suffix          = suffix?.let { { Text(it, fontSize = 12.sp, color = POSColors.Muted) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine      = true,
        colors          = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = POSColors.Accent,
            unfocusedBorderColor = POSColors.Border2,
            focusedTextColor     = POSColors.Text,
            unfocusedTextColor   = POSColors.Text,
            cursorColor          = POSColors.Accent
        ),
        textStyle = TextStyle(
            fontSize   = 13.sp,
            fontFamily = if (keyboardType == KeyboardType.Decimal) FontFamily.Monospace else FontFamily.Default,
            textAlign  = textAlign
        ),
        shape = RoundedCornerShape(6.dp)
    )
}

@Composable
fun <T> ToggleGroup(
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color    = POSColors.Surface,
        shape    = RoundedCornerShape(7.dp),
        border   = androidx.compose.foundation.BorderStroke(1.dp, POSColors.Border2)
    ) {
        Row {
            options.forEach { (label, value) ->
                val isSelected = selected == value
                Surface(
                    onClick  = { onSelect(value) },
                    color    = if (isSelected) POSColors.Accent else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(label,
                        modifier  = Modifier.padding(vertical = 6.dp, horizontal = 10.dp),
                        fontSize  = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color     = if (isSelected) Color.White else POSColors.Muted,
                        textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun FlowIndicator() {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        modifier              = Modifier.fillMaxWidth()) {
        val steps = listOf(
            "Gross"         to POSColors.Accent2,
            "Modifiers"     to POSColors.Pink,
            "Line Discs"    to POSColors.Orange,
            "Receipt Discs" to POSColors.Purple,
            "Item Taxes"    to POSColors.Green,
            "Fixed Charges" to POSColors.Teal,
            "Grand Total"   to POSColors.Accent2
        )
        steps.forEachIndexed { index, (label, color) ->
            Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp)) {
                Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color)
            }
            if (index < steps.size - 1) Text("→", fontSize = 12.sp, color = POSColors.Muted)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

val ColumnHeaderStyle = TextStyle(
    fontSize      = 10.sp,
    fontWeight    = FontWeight.Bold,
    letterSpacing = 0.5.sp,
    color         = POSColors.Muted
)

fun BigDecimal.fmt(): String =
    "$${this.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()}"