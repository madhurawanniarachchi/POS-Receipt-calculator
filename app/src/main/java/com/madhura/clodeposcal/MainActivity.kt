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
                Discount(UUID.randomUUID().toString(), "Nexus 25%",  DiscountType.PERCENT, BigDecimal("25")),
                Discount(UUID.randomUUID().toString(), "Quick deal 30%",  DiscountType.PERCENT,   BigDecimal("30")),
            )
        )
    }

    // ── Tax catalogue  (taxOrder replaces the old taxOnTax Boolean) ───────────
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

    // ── Items ─────────────────────────────────────────────────────────────────
    var items by remember {
        mutableStateOf(
            listOf(
                Item("1", "Ambewela Non-Fat Milk Uht Tetra 1L", BigDecimal("1"), BigDecimal("390.00"),
                    appliedDiscountIds = emptySet(),
                    appliedTaxIds      = setOf("inc1", "inc2", "service", "gst")),
                Item("2", "Elephant House Cream Soda Pet Bottle 1.5L", BigDecimal("1"), BigDecimal("315.00"),
                    appliedDiscountIds = emptySet(),
                    appliedTaxIds      = setOf("inc1", "inc2", "service", "gst")),
                Item("3", "Keells Drinking Water Pet 500ml", BigDecimal("1"), BigDecimal("70.00"),
                    appliedDiscountIds = emptySet(),
                    appliedTaxIds      = setOf("inc1", "inc2", "service", "gst"))
            )
        )
    }

    // ── Receipt discounts ─────────────────────────────────────────────────────
    var receiptDiscounts by remember {
        mutableStateOf(
            listOf(
                Discount(UUID.randomUUID().toString(), "R - Nexus", DiscountType.PERCENT, 25.toBigDecimal()),
                Discount(UUID.randomUUID().toString(), "R - Quick deal", DiscountType.PERCENT, 30.toBigDecimal()),
                Discount(UUID.randomUUID().toString(), "E - Re-usable Bag", DiscountType.FIXED, 50.toBigDecimal())
            )
        )
    }

    // ── Fixed charges ─────────────────────────────────────────────────────────
    var fixedCharges by remember {
        mutableStateOf(
            listOf(FixedCharge(UUID.randomUUID().toString(), "Service Fee", BigDecimal.ZERO))
        )
    }

    // ── Reactive calculation ──────────────────────────────────────────────────
    val result = remember(items, lineDiscounts, taxes, receiptDiscounts, fixedCharges) {
        POSTaxCalculator(items, lineDiscounts, taxes, receiptDiscounts, fixedCharges).calculate()
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
                        onAddLineDiscount    = {
                            lineDiscounts = lineDiscounts + Discount(UUID.randomUUID().toString(), "", DiscountType.PERCENT, BigDecimal.ZERO)
                        },
                        onUpdateLineDiscount = { id, d -> lineDiscounts = lineDiscounts.map { if (it.id == id) d else it } },
                        onDeleteLineDiscount = { id ->
                            lineDiscounts = lineDiscounts.filter { it.id != id }
                            items = items.map { it.copy(appliedDiscountIds = it.appliedDiscountIds - id) }
                        },
                        onAddItem    = { items = items + Item(UUID.randomUUID().toString(), "", BigDecimal.ONE, BigDecimal.ZERO) },
                        onUpdateItem = { id, it -> items = items.map { item -> if (item.id == id) it else item } },
                        onDeleteItem = { id -> items = items.filter { it.id != id } },
                        onAddTax     = { taxes = taxes + Tax(UUID.randomUUID().toString(), "", BigDecimal.ZERO, TaxMode.EXCLUDE, TaxOrder.BEFORE) },
                        onUpdateTax  = { id, it -> taxes = taxes.map { t -> if (t.id == id) it else t } },
                        onDeleteTax  = { id ->
                            taxes = taxes.filter { it.id != id }
                            items = items.map { it.copy(appliedTaxIds = it.appliedTaxIds - id) }
                        },
                        onAddReceiptDiscount    = { receiptDiscounts = receiptDiscounts + Discount(UUID.randomUUID().toString(), "", DiscountType.PERCENT, BigDecimal.ZERO) },
                        onUpdateReceiptDiscount = { id, it -> receiptDiscounts = receiptDiscounts.map { d -> if (d.id == id) it else d } },
                        onDeleteReceiptDiscount = { id -> receiptDiscounts = receiptDiscounts.filter { it.id != id } },
                        onAddFixedCharge        = { fixedCharges = fixedCharges + FixedCharge(UUID.randomUUID().toString(), "Charge", BigDecimal.ZERO) },
                        onUpdateFixedCharge     = { id, it -> fixedCharges = fixedCharges.map { fc -> if (fc.id == id) it else fc } },
                        onDeleteFixedCharge     = { id -> fixedCharges = fixedCharges.filter { it.id != id } }
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
                        Text("NAME",  modifier = Modifier.weight(1f),       style = ColumnHeaderStyle)
                        Text("QTY",   modifier = Modifier.width(70.dp),     style = ColumnHeaderStyle, textAlign = TextAlign.End)
                        Text("PRICE", modifier = Modifier.width(90.dp),     style = ColumnHeaderStyle, textAlign = TextAlign.End)
                        Text("GROSS", modifier = Modifier.width(80.dp),     style = ColumnHeaderStyle, textAlign = TextAlign.End)
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
    Surface(
        color  = POSColors.Background,
        shape  = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, POSColors.Border)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // Name / Qty / Price / Gross / Delete
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
                    (item.qty * item.unitPrice).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                    modifier     = Modifier.width(80.dp),
                    fontFamily   = FontFamily.Monospace,
                    fontSize     = 12.sp,
                    color        = POSColors.Text,
                    textAlign    = TextAlign.End
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, "Delete", tint = POSColors.Red, modifier = Modifier.size(18.dp))
                }
            }

            // Line-discount checkboxes
            CheckboxPanel(
                title       = "LINE DISCOUNTS",
                emptyLabel  = "No discounts defined in catalogue",
                badgeLabel  = "No Discount",
                badgeColor  = POSColors.Orange,
                isEmpty     = item.appliedDiscountIds.isEmpty(),
                accentColor = POSColors.Orange,
                rows        = allDiscounts.map { disc ->
                    CheckboxRow(
                        id      = disc.id,
                        checked = disc.id in item.appliedDiscountIds,
                        label   = disc.label.ifBlank { "Unnamed discount" },
                        detail  = "${disc.value.stripTrailingZeros().toPlainString()}${if (disc.type == DiscountType.PERCENT) "%" else "$"} off"
                    )
                },
                onToggle = { id, checked ->
                    val ids = if (checked) item.appliedDiscountIds + id else item.appliedDiscountIds - id
                    onUpdate(item.copy(appliedDiscountIds = ids))
                }
            )

            // Tax checkboxes
            CheckboxPanel(
                title       = "TAXES APPLIED",
                emptyLabel  = "No taxes defined in catalogue",
                badgeLabel  = "Tax-Free",
                badgeColor  = POSColors.Amber,
                isEmpty     = item.appliedTaxIds.isEmpty(),
                accentColor = POSColors.Green,
                rows        = allTaxes.map { tax ->
                    CheckboxRow(
                        id      = tax.id,
                        checked = tax.id in item.appliedTaxIds,
                        label   = tax.name.ifBlank { "Unnamed tax" },
                        detail  = "${tax.rate.stripTrailingZeros().toPlainString()}% " +
                                "${if (tax.mode == TaxMode.INCLUDE) "(incl)" else "(excl)"} " +
                                "${if (tax.taxOrder == TaxOrder.AFTER) "⚡ToT" else ""}"
                    )
                },
                onToggle = { id, checked ->
                    val ids = if (checked) item.appliedTaxIds + id else item.appliedTaxIds - id
                    onUpdate(item.copy(appliedTaxIds = ids))
                }
            )
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
                        checked           = row.checked,
                        onCheckedChange   = { onToggle(row.id, it) },
                        modifier          = Modifier.size(20.dp),
                        colors            = CheckboxDefaults.colors(
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
// Tax Row  — now has BEFORE / AFTER (TaxOrder) toggle instead of Base / ToT
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TaxRow(tax: Tax, onUpdate: (Tax) -> Unit, onDelete: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            // Name
            POSTextField(
                value         = tax.name,
                onValueChange = { onUpdate(tax.copy(name = it)) },
                modifier      = Modifier.weight(1f),
                placeholder   = "Tax name"
            )
            // Rate
            POSTextField(
                value         = tax.rate.toPlainString(),
                onValueChange = { runCatching { onUpdate(tax.copy(rate = BigDecimal(it))) } },
                modifier      = Modifier.width(80.dp),
                keyboardType  = KeyboardType.Decimal,
                suffix        = "%",
                textAlign     = TextAlign.End
            )
            // Mode: Excl / Incl
            ToggleGroup(
                options  = listOf("Excl" to TaxMode.EXCLUDE, "Incl" to TaxMode.INCLUDE),
                selected = tax.mode,
                onSelect = { onUpdate(tax.copy(mode = it)) },
                modifier = Modifier.width(120.dp)
            )
            // Order: Before / After (tax-on-tax)
            ToggleGroup(
                options  = listOf("Before" to TaxOrder.BEFORE, "After" to TaxOrder.AFTER),
                selected = tax.taxOrder,
                onSelect = { onUpdate(tax.copy(taxOrder = it)) },
                modifier = Modifier.width(140.dp)
            )
            // Delete
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

        // Header bar
        Box(modifier = Modifier.fillMaxWidth().background(POSColors.Surface)
            .padding(vertical = 16.dp, horizontal = 20.dp)) {
            Text("RECEIPT", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp, color = POSColors.Muted)
        }

        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {

            // ── Per-item breakdown ────────────────────────────────────────────
            result.items.forEach { item -> ReceiptItem(item) }

            Divider(color = POSColors.Border2, modifier = Modifier.padding(vertical = 8.dp))

            // ── Subtotals ─────────────────────────────────────────────────────
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

            // ── Taxes ─────────────────────────────────────────────────────────
            if (result.taxResults.isNotEmpty()) {
                Divider(color = POSColors.Border2, modifier = Modifier.padding(vertical = 8.dp))
                result.taxResults.forEach { taxResult ->
                    val color = if (taxResult.tax.mode == TaxMode.INCLUDE) POSColors.Amber else POSColors.Green
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

            // ── Fixed charges ─────────────────────────────────────────────────
            if (result.fixedCharges.isNotEmpty()) {
                Divider(color = POSColors.Border2, modifier = Modifier.padding(vertical = 8.dp))
                result.fixedCharges.forEach { (fc, amount) ->
                    if (amount > BigDecimal.ZERO)
                        ReceiptRow(fc.label, amount, POSColors.Teal, positive = true)
                }
                if (result.totalFixedChargeAmount > BigDecimal.ZERO)
                    ReceiptRow("Total Fixed Charges", result.totalFixedChargeAmount, POSColors.Teal, fontWeight = FontWeight.Bold, positive = true)
            }

            // ── Grand total ───────────────────────────────────────────────────
            Divider(color = POSColors.Border2, thickness = 2.dp, modifier = Modifier.padding(vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("GRAND TOTAL", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = POSColors.Text)
                Text(result.grandTotal.fmt(), fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace, color = POSColors.Green)
            }

            // ── Balance verification panel ────────────────────────────────────
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
        // Item name row with gross
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(item.item.name.ifBlank { "—" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = POSColors.Text)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (item.item.appliedTaxIds.isEmpty()) {
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

            // Line discounts (computed from original base per spec)
            item.lineDiscountBreakdown.forEach { (disc, amount) ->
                if (amount > BigDecimal.ZERO)
                    SubRow("${disc.label.ifBlank { "Disc" }} (orig base)", amount, POSColors.Orange, negative = true)
            }
            if (item.totalLineDiscount > BigDecimal.ZERO)
                SubRow("After line disc", item.netAfterLine, POSColors.Orange)

            // Receipt discount share (largest-remainder allocation)
            if (item.receiptDiscountShare > BigDecimal.ZERO)
                SubRow("Receipt disc (LR alloc)", item.receiptDiscountShare, POSColors.Purple, negative = true)

            // afterReceiptDiscount = gross price still containing embedded inclusive taxes
            SubRow("After receipt disc (incl-tax price)", item.afterReceiptDiscount, POSColors.Muted)

            // inclusiveTaxBase = price net of all embedded inclusive taxes
            // This is the correct base for exclusive tax computation
            if (item.afterReceiptDiscount != item.inclusiveTaxBase)
                SubRow("Net of incl taxes (excl-tax base)", item.inclusiveTaxBase, POSColors.Accent2)

            // Tax lines — BEFORE first, then AFTER (tax-on-tax)
            item.taxLines.forEach { line ->
                if (line.taxAmount > BigDecimal.ZERO) {
                    val color    = if (line.taxMode == TaxMode.INCLUDE) POSColors.Amber else POSColors.Green
                    val modeTag  = if (line.taxMode == TaxMode.INCLUDE) "incl" else "excl"
                    val orderTag = if (line.taxOrder == TaxOrder.AFTER) " ⚡ToT" else ""
                    // Show the base used so the calculation is fully transparent
                    val baseTag  = " [base ${line.taxableBase.fmt()}]"
                    SubRow(
                        label    = "${line.name} ${line.ratePercent.stripTrailingZeros().toPlainString()}% $modeTag$orderTag$baseTag",
                        value    = line.taxAmount,
                        color    = color,
                        positive = line.taxMode == TaxMode.EXCLUDE
                    )
                }
            }

            // Item totals
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
    // grandTotal = subtotal2 + exclusiveTaxTotal + fixedCharges
    // Per-item cross-check: Σ(ard + exclTaxOnItem) + fixed == grandTotal
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

            // Grand total check: Σ item.totalInclTax (= inclusiveTaxBase + exclTaxes) + fixed == grandTotal
            VerifyRow(
                label    = "Σ item.totalInclTax + fixed",
                lhs      = grandTotalCheck,
                rhs      = result.grandTotal,
                match    = grandMatch,
                rhsLabel = "grandTotal"
            )

            // Receipt discount check: Σ item.receiptDiscountShare == totalReceiptDiscountAmount
            VerifyRow(
                label    = "Σ receiptDiscShare (LR)",
                lhs      = receiptDiscCheck,
                rhs      = result.totalReceiptDiscountAmount,
                match    = receiptMatch,
                rhsLabel = "totalReceiptDisc"
            )

            // Per-tax aggregate verification (Σ item tax lines == receipt-level tax amount)
            result.taxResults.forEach { taxResult ->
                val itemSum = result.items.sumOfDecimal { item ->
                    item.taxLines.find { it.taxId == taxResult.tax.id }?.taxAmount ?: BigDecimal.ZERO
                }
                val match = itemSum.compareTo(taxResult.amount) == 0
                VerifyRow(
                    label    = "${taxResult.tax.name} Σ items",
                    lhs      = itemSum,
                    rhs      = taxResult.amount,
                    match    = match,
                    rhsLabel = "receipt total"
                )
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
            fontSize    = 12.sp,
            fontFamily  = FontFamily.Monospace,
            fontWeight  = fontWeight,
            color       = color
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
                            onClick          = onAdd,
                            colors           = ButtonDefaults.buttonColors(containerColor = accentColor),
                            contentPadding   = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                            modifier         = Modifier.height(28.dp)
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
        value         = value,
        onValueChange = onValueChange,
        modifier      = modifier,
        placeholder   = { Text(placeholder, fontSize = 12.sp, color = POSColors.Muted) },
        prefix        = prefix?.let { { Text(it, fontSize = 12.sp, color = POSColors.Muted) } },
        suffix        = suffix?.let { { Text(it, fontSize = 12.sp, color = POSColors.Muted) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine    = true,
        colors        = OutlinedTextFieldDefaults.colors(
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
            "Gross"          to POSColors.Accent2,
            "Line Discs"     to POSColors.Orange,
            "Receipt Discs"  to POSColors.Purple,
            "Item Taxes"     to POSColors.Green,
            "Fixed Charges"  to POSColors.Teal,
            "Grand Total"    to POSColors.Accent2
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
    fontSize     = 10.sp,
    fontWeight   = FontWeight.Bold,
    letterSpacing = 0.5.sp,
    color        = POSColors.Muted
)

/** Format BigDecimal as a dollar amount. */
fun BigDecimal.fmt(): String =
    "$${this.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()}"

