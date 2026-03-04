// ═══════════════════════════════════════════════════════════════════════════════
// POS Tax Calculator - Complete Kotlin Implementation
// Features:
// - Multiple line discounts per item (sequential)
// - Multiple invoice discounts globally (sequential + allocated)
// - Multiple item-specific taxes with ToT option
// - Multiple global taxes with ToT option
// - Fixed charge at global level
// - All with enable/disable toggles
// ═══════════════════════════════════════════════════════════════════════════════

package com.madhura.clodeposcal.ui

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// Data Classes
// ─────────────────────────────────────────────────────────────────────────────

enum class DiscountType { PERCENT, FIXED }
enum class TaxMode { INCLUDE, EXCLUDE }

/**
 * Single line discount for an item
 */
data class LineDiscount(
    val id: String = UUID.randomUUID().toString(),
    val type: DiscountType = DiscountType.PERCENT,
    val value: BigDecimal = BigDecimal.ZERO,
    val enabled: Boolean = true
)

/**
 * Invoice discount (global, allocated to items)
 */
data class InvoiceDiscount(
    val id: String = UUID.randomUUID().toString(),
    val type: DiscountType = DiscountType.PERCENT,
    val value: BigDecimal = BigDecimal.ZERO,
    val enabled: Boolean = true
)

/**
 * Item-specific tax (applies only to this item)
 */
data class ItemTax(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val rate: BigDecimal = BigDecimal.ZERO,
    val mode: TaxMode = TaxMode.EXCLUDE,
    val taxOnTax: Boolean = false,
    val enabled: Boolean = true
)

/**
 * Global tax (applies to all items)
 */
data class GlobalTax(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val rate: BigDecimal = BigDecimal.ZERO,
    val mode: TaxMode = TaxMode.EXCLUDE,
    val taxOnTax: Boolean = false,
    val enabled: Boolean = true
)

/**
 * Line item with multiple discounts and taxes
 */
data class Item(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val qty: BigDecimal = BigDecimal.ONE,
    val unitPrice: BigDecimal = BigDecimal.ZERO,
    val lineDiscounts: List<LineDiscount> = listOf(LineDiscount()),
    val itemTaxes: List<ItemTax> = emptyList()
)

/**
 * Fixed charge (e.g., service fee)
 */
data class FixedCharge(
    val label: String = "Service Fee",
    val value: BigDecimal = BigDecimal.ZERO
)

// ─────────────────────────────────────────────────────────────────────────────
// Calculation Results
// ─────────────────────────────────────────────────────────────────────────────

data class LineDiscountBreakdown(
    val discount: LineDiscount,
    val amount: BigDecimal
)

data class InvoiceDiscountBreakdown(
    val discount: InvoiceDiscount,
    val amount: BigDecimal
)

data class TaxResult(
    val id: String,
    val name: String,
    val rate: BigDecimal,
    val mode: TaxMode,
    val taxOnTax: Boolean,
    val base: BigDecimal,
    val amount: BigDecimal,
    val isGlobal: Boolean
)

data class ProcessedItem(
    val item: Item,
    val gross: BigDecimal,
    val lineDiscountBreakdown: List<LineDiscountBreakdown>,
    val totalLineDiscount: BigDecimal,
    val netAfterLine: BigDecimal,
    val invoiceDiscountBreakdown: List<InvoiceDiscountBreakdown>,
    val totalInvoiceDiscount: BigDecimal,
    val netAfterInvoiceDisc: BigDecimal,
    val inclusiveTaxBase: BigDecimal,
    val allItemTaxes: Map<String, BigDecimal>,  // Tax ID -> Amount
    val lineTotal: BigDecimal
)

data class CalculationResult(
    val items: List<ProcessedItem>,
    val grossTotal: BigDecimal,
    val totalLineDiscount: BigDecimal,
    val subtotal1: BigDecimal,
    val totalInvoiceDiscount: BigDecimal,
    val subtotal2: BigDecimal,
    val inclusiveTaxBase: BigDecimal,
    val allTaxes: List<TaxResult>,
    val inclusiveTaxTotal: BigDecimal,
    val exclusiveTaxTotal: BigDecimal,
    val fixedChargeAmount: BigDecimal,
    val grandTotal: BigDecimal
)

// ─────────────────────────────────────────────────────────────────────────────
// POS Tax Calculator Engine
// ─────────────────────────────────────────────────────────────────────────────

class POSTaxCalculator(
    private val items: List<Item>,
    private val globalTaxes: List<GlobalTax>,
    private val invoiceDiscounts: List<InvoiceDiscount>,
    private val fixedCharge: FixedCharge
) {
    
    companion object {
        private fun BigDecimal.r2(): BigDecimal = 
            this.setScale(2, RoundingMode.HALF_UP)
        
        private fun BigDecimal.r4(): BigDecimal = 
            this.setScale(4, RoundingMode.HALF_UP)
    }
    
    fun calculate(): CalculationResult {
        
        // ── Step 1: Calculate per-item with multiple line discounts ──
        val step1 = items.map { item ->
            val gross = (item.unitPrice * item.qty).r2()
            
            // Apply multiple line discounts sequentially
            var netAfterLine = gross
            val lineDiscBreakdown = mutableListOf<LineDiscountBreakdown>()
            
            item.lineDiscounts.filter { it.enabled }.forEach { disc ->
                val discAmount = when (disc.type) {
                    DiscountType.PERCENT -> (netAfterLine * disc.value / BigDecimal(100)).r2()
                    DiscountType.FIXED -> disc.value.r2()
                }
                netAfterLine = (netAfterLine - discAmount).r2()
                lineDiscBreakdown.add(LineDiscountBreakdown(disc, discAmount))
            }
            
            val totalLineDisc = lineDiscBreakdown.sumOf { it.amount }
            
            ItemStep1(item, gross, lineDiscBreakdown, totalLineDisc, netAfterLine)
        }
        
        val grossTotal = step1.sumOf { it.gross }
        val totalLineDiscount = step1.sumOf { it.totalLineDisc }
        val subtotal1 = step1.sumOf { it.netAfterLine }
        
        // ── Step 2: Apply multiple invoice discounts sequentially ──
        var currentSubtotal = subtotal1
        val enabledInvoiceDiscounts = invoiceDiscounts.filter { it.enabled }
        
        val step2 = step1.map { item -> 
            ItemStep2(
                item,
                mutableListOf(),
                BigDecimal.ZERO,
                item.netAfterLine
            )
        }
        
        enabledInvoiceDiscounts.forEach { invDisc ->
            val discAmount = when (invDisc.type) {
                DiscountType.PERCENT -> (currentSubtotal * invDisc.value / BigDecimal(100)).r2()
                DiscountType.FIXED -> invDisc.value.r2()
            }
            
            // Round-robin allocation
            val allocations = allocateRoundRobin(
                total = discAmount,
                items = step2.map { it.netAfterInvoiceDisc },
                itemCount = items.size
            )
            
            allocations.forEachIndexed { idx, amt ->
                step2[idx].invoiceDiscBreakdown.add(InvoiceDiscountBreakdown(invDisc, amt))
                step2[idx].totalInvoiceDisc = (step2[idx].totalInvoiceDisc + amt).r2()
                step2[idx].netAfterInvoiceDisc = (step2[idx].netAfterInvoiceDisc - amt).r2()
            }
            
            currentSubtotal = (currentSubtotal - discAmount).r2()
        }
        
        val totalInvoiceDiscount = step2.sumOf { it.totalInvoiceDisc }
        val subtotal2 = step2.sumOf { it.netAfterInvoiceDisc }
        
        // ── Step 3: Extract inclusive tax base ──
        val enabledGlobalTaxes = globalTaxes.filter { it.enabled }
        val globalInclTaxes = enabledGlobalTaxes.filter { it.mode == TaxMode.INCLUDE && it.rate > BigDecimal.ZERO }
        val globalInclRateSum = globalInclTaxes.sumOf { it.rate }
        
        val step3 = step2.map { step2Item ->
            val itemInclTaxes = step2Item.step1.item.itemTaxes.filter { it.enabled && it.mode == TaxMode.INCLUDE }
            val itemInclRateSum = itemInclTaxes.sumOf { it.rate }
            val totalInclRate = globalInclRateSum + itemInclRateSum
            
            val inclusiveTaxBase = if (totalInclRate > BigDecimal.ZERO) {
                (step2Item.netAfterInvoiceDisc / (BigDecimal.ONE + totalInclRate / BigDecimal(100))).r4()
            } else {
                step2Item.netAfterInvoiceDisc
            }
            
            ItemStep3(step2Item, inclusiveTaxBase)
        }
        
        val totalInclBase = step3.sumOf { it.inclusiveTaxBase }
        val exclBase = totalInclBase
        
        // ── Step 4: Calculate aggregate global taxes ──
        val globalTaxResults = mutableListOf<TaxResult>()
        
        enabledGlobalTaxes.forEach { tax ->
            if (tax.rate <= BigDecimal.ZERO) return@forEach
            
            val base = when (tax.mode) {
                TaxMode.INCLUDE -> totalInclBase
                TaxMode.EXCLUDE -> {
                    if (tax.taxOnTax) {
                        val prevExclTaxSum = globalTaxResults
                            .filter { it.mode == TaxMode.EXCLUDE }
                            .sumOf { it.amount }
                        (exclBase + prevExclTaxSum).r2()
                    } else {
                        exclBase
                    }
                }
            }
            
            val amount = (base * tax.rate / BigDecimal(100)).r2()
            globalTaxResults.add(
                TaxResult(
                    id = tax.id,
                    name = tax.name,
                    rate = tax.rate,
                    mode = tax.mode,
                    taxOnTax = tax.taxOnTax,
                    base = base,
                    amount = amount,
                    isGlobal = true
                )
            )
        }
        
        // ── Step 5: Calculate item-specific taxes with ToT ──
        val step4 = step3.map { step3Item ->
            val itemTaxResults = mutableListOf<TaxResult>()
            val enabledItemTaxes = step3Item.step2.step1.item.itemTaxes.filter { it.enabled }
            
            enabledItemTaxes.forEach { tax ->
                if (tax.rate <= BigDecimal.ZERO) return@forEach
                
                var base = step3Item.inclusiveTaxBase
                
                // Tax-on-Tax for item-level taxes
                if (tax.taxOnTax) {
                    val prevItemTaxSum = itemTaxResults
                        .filter { it.mode == TaxMode.EXCLUDE }
                        .sumOf { it.amount }
                    base = (base + prevItemTaxSum).r2()
                }
                
                val amount = (base * tax.rate / BigDecimal(100)).r2()
                
                itemTaxResults.add(
                    TaxResult(
                        id = tax.id,
                        name = tax.name,
                        rate = tax.rate,
                        mode = tax.mode,
                        taxOnTax = tax.taxOnTax,
                        base = base,
                        amount = amount,
                        isGlobal = false
                    )
                )
            }
            
            ItemStep4(step3Item, itemTaxResults)
        }
        
        // Combine all taxes
        val allTaxes = globalTaxResults.toMutableList()
        step4.forEach { step4Item ->
            step4Item.itemTaxResults.forEach { tax ->
                val existing = allTaxes.find { it.id == tax.id }
                if (existing != null) {
                    val idx = allTaxes.indexOf(existing)
                    allTaxes[idx] = existing.copy(amount = (existing.amount + tax.amount).r2())
                } else {
                    allTaxes.add(tax)
                }
            }
        }
        
        val inclTaxTotal = allTaxes.filter { it.mode == TaxMode.INCLUDE }.sumOf { it.amount }
        val exclTaxTotal = allTaxes.filter { it.mode == TaxMode.EXCLUDE }.sumOf { it.amount }
        
        // ── Step 6: Round robin for global taxes ──
        val step5 = step4.map { step4Item ->
            ItemStep5(step4Item, mutableMapOf())
        }
        
        globalTaxResults.forEach { tax ->
            val baseItems = step5.map { it.step4.step3.inclusiveTaxBase }
            val allocations = allocateRoundRobin(tax.amount, baseItems, items.size)
            
            allocations.forEachIndexed { idx, amount ->
                step5[idx].rrGlobalTaxByTax[tax.id] = amount
            }
        }
        
        // ── Step 7: Build final processed items ──
        val processedItems = step5.map { step5Item ->
            val allItemTaxes = step5Item.rrGlobalTaxByTax.toMutableMap()
            
            step5Item.step4.itemTaxResults.forEach { tax ->
                allItemTaxes[tax.id] = (allItemTaxes[tax.id] ?: BigDecimal.ZERO) + tax.amount
            }
            
            val inclTaxForItem = allItemTaxes
                .filter { (id, _) -> allTaxes.find { it.id == id }?.mode == TaxMode.INCLUDE }
                .values
                .sumOf { it }
            
            val exclTaxForItem = allItemTaxes
                .filter { (id, _) -> allTaxes.find { it.id == id }?.mode == TaxMode.EXCLUDE }
                .values
                .sumOf { it }
            
            val lineTotal = (step5Item.step4.step3.inclusiveTaxBase + inclTaxForItem + exclTaxForItem).r2()
            
            ProcessedItem(
                item = step5Item.step4.step3.step2.step1.item,
                gross = step5Item.step4.step3.step2.step1.gross,
                lineDiscountBreakdown = step5Item.step4.step3.step2.step1.lineDiscBreakdown,
                totalLineDiscount = step5Item.step4.step3.step2.step1.totalLineDisc,
                netAfterLine = step5Item.step4.step3.step2.step1.netAfterLine,
                invoiceDiscountBreakdown = step5Item.step4.step3.step2.invoiceDiscBreakdown,
                totalInvoiceDiscount = step5Item.step4.step3.step2.totalInvoiceDisc,
                netAfterInvoiceDisc = step5Item.step4.step3.step2.netAfterInvoiceDisc,
                inclusiveTaxBase = step5Item.step4.step3.inclusiveTaxBase,
                allItemTaxes = allItemTaxes,
                lineTotal = lineTotal
            )
        }
        
        // ── Step 8: Grand total ──
        val fixedChargeAmount = fixedCharge.value.r2()
        val grandTotal = (totalInclBase + inclTaxTotal + exclTaxTotal + fixedChargeAmount).r2()
        
        return CalculationResult(
            items = processedItems,
            grossTotal = grossTotal,
            totalLineDiscount = totalLineDiscount,
            subtotal1 = subtotal1,
            totalInvoiceDiscount = totalInvoiceDiscount,
            subtotal2 = subtotal2,
            inclusiveTaxBase = totalInclBase,
            allTaxes = allTaxes,
            inclusiveTaxTotal = inclTaxTotal,
            exclusiveTaxTotal = exclTaxTotal,
            fixedChargeAmount = fixedChargeAmount,
            grandTotal = grandTotal
        )
    }
    
    private fun allocateRoundRobin(
        total: BigDecimal,
        items: List<BigDecimal>,
        itemCount: Int
    ): List<BigDecimal> {
        if (total == BigDecimal.ZERO || itemCount == 0) {
            return List(itemCount) { BigDecimal.ZERO }
        }
        
        val itemsSum = items.sumOf { it }
        val exactAmounts = items.map { itemValue ->
            if (itemsSum > BigDecimal.ZERO) itemValue * total / itemsSum else BigDecimal.ZERO
        }
        
        val flooredAmounts = exactAmounts.map { amount ->
            (amount.movePointRight(2).setScale(0, RoundingMode.DOWN).movePointLeft(2))
        }
        
        val flooredSum = flooredAmounts.sumOf { it }
        val remainderCents = ((total - flooredSum) * BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toInt()
        
        val finalAllocations = flooredAmounts.toMutableList()
        var remainingCents = remainderCents
        var index = 0
        
        while (remainingCents > 0) {
            finalAllocations[index] = (finalAllocations[index] + BigDecimal("0.01")).r2()
            remainingCents--
            index = (index + 1) % itemCount
        }
        
        return finalAllocations
    }
    
    // Helper classes for intermediate steps
    private data class ItemStep1(
        val item: Item,
        val gross: BigDecimal,
        val lineDiscBreakdown: List<LineDiscountBreakdown>,
        val totalLineDisc: BigDecimal,
        val netAfterLine: BigDecimal
    )
    
    private data class ItemStep2(
        val step1: ItemStep1,
        val invoiceDiscBreakdown: MutableList<InvoiceDiscountBreakdown>,
        var totalInvoiceDisc: BigDecimal,
        var netAfterInvoiceDisc: BigDecimal
    )
    
    private data class ItemStep3(
        val step2: ItemStep2,
        val inclusiveTaxBase: BigDecimal
    )
    
    private data class ItemStep4(
        val step3: ItemStep3,
        val itemTaxResults: List<TaxResult>
    )
    
    private data class ItemStep5(
        val step4: ItemStep4,
        val rrGlobalTaxByTax: MutableMap<String, BigDecimal>
    )
}

fun <T> Iterable<T>.sumOf(selector: (T) -> BigDecimal): BigDecimal =
    fold(BigDecimal.ZERO) { acc, item -> acc.add(selector(item)) }

// ─────────────────────────────────────────────────────────────────────────────
// Extension Functions
// ─────────────────────────────────────────────────────────────────────────────

fun BigDecimal.format(): String = 
    "\$${this.setScale(2, RoundingMode.HALF_UP).toPlainString()}"
