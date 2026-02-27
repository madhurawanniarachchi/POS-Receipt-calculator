package com.madhura.clodeposcal// ═══════════════════════════════════════════════════════════════════════════════


// ═══════════════════════════════════════════════════════════════════════════════
// POS Tax Calculator - Kotlin Implementation
// Features: Global line-discount catalogue (checkbox per item),
//           Global tax catalogue (checkbox per item),
//           Multiple receipt discounts, Multiple fixed charges,
//           Inclusive/Exclusive taxes, Tax-on-tax, Round-robin allocation
// ═══════════════════════════════════════════════════════════════════════════════

import java.math.BigDecimal
import java.math.RoundingMode

// ─────────────────────────────────────────────────────────────────────────────
// Data Classes
// ─────────────────────────────────────────────────────────────────────────────

enum class DiscountType { PERCENT, FIXED }
enum class TaxMode { INCLUDE, EXCLUDE }

/**
 * A discount definition in the global catalogue.
 * Used for BOTH the line-discount catalogue and the receipt-discount list.
 */
data class Discount(
    val id: String,
    val label: String = "",
    val type: DiscountType = DiscountType.PERCENT,
    val value: BigDecimal = BigDecimal.ZERO
)

/**
 * Tax definition in the global catalogue.
 * Items opt in via [Item.appliedTaxIds].
 */
data class Tax(
    val id: String,
    val name: String,
    val rate: BigDecimal,
    val mode: TaxMode = TaxMode.EXCLUDE,
    val taxOnTax: Boolean = false
)

/**
 * Line item.
 * [appliedDiscountIds] — which line-discount definitions apply (ordered by catalogue order).
 * [appliedTaxIds]      — which tax definitions apply.
 */
data class Item(
    val id: String,
    val name: String,
    val qty: BigDecimal,
    val unitPrice: BigDecimal,
    val appliedDiscountIds: Set<String> = emptySet(),
    val appliedTaxIds: Set<String> = emptySet()
)

/** Fixed charge (e.g. delivery fee). Multiple supported via List<FixedCharge>. */
data class FixedCharge(
    val id: String,
    val label: String = "Service Fee",
    val value: BigDecimal = BigDecimal.ZERO
)

// ─────────────────────────────────────────────────────────────────────────────
// Result Types
// ─────────────────────────────────────────────────────────────────────────────

data class ProcessedItem(
    val item: Item,
    val gross: BigDecimal,
    /** Ordered list of (discount, computed amount) for only the applied discounts. */
    val lineDiscountBreakdown: List<Pair<Discount, BigDecimal>>,
    val totalLineDiscount: BigDecimal,
    val netAfterLine: BigDecimal,
    /** Round-robin allocation of each receipt discount to this item. */
    val receiptDiscountAllocBreakdown: List<Pair<Discount, BigDecimal>>,
    val totalReceiptDiscountAlloc: BigDecimal,
    val netAfterInvoice: BigDecimal,
    val inclusiveTaxBase: BigDecimal,
    /** taxId → RR-allocated amount; only taxes this item opted in to. */
    val taxByTaxId: Map<String, BigDecimal>,
    val totalTax: BigDecimal,
    val lineTotal: BigDecimal
)

data class TaxResult(
    val tax: Tax,
    val base: BigDecimal,
    val amount: BigDecimal
)

data class CalculationResult(
    val items: List<ProcessedItem>,
    val grossTotal: BigDecimal,
    val totalLineDiscount: BigDecimal,
    val subtotal1: BigDecimal,
    val receiptDiscountAmounts: List<Pair<Discount, BigDecimal>>,
    val totalReceiptDiscountAmount: BigDecimal,
    val subtotal2: BigDecimal,
    val taxResults: List<TaxResult>,
    val inclusiveTaxTotal: BigDecimal,
    val exclusiveTaxTotal: BigDecimal,
    val fixedCharges: List<Pair<FixedCharge, BigDecimal>>,
    val totalFixedChargeAmount: BigDecimal,
    val grandTotal: BigDecimal
)

// ─────────────────────────────────────────────────────────────────────────────
// Calculator Engine
// ─────────────────────────────────────────────────────────────────────────────

class POSTaxCalculator(
    private val items: List<Item>,
    private val lineDiscounts: List<Discount>,          // global line-discount catalogue
    private val taxes: List<Tax>,                       // global tax catalogue
    private val receiptDiscounts: List<Discount> = emptyList(),
    private val fixedCharges: List<FixedCharge> = emptyList()
) {

    private fun BigDecimal.r2() = setScale(2, RoundingMode.HALF_UP)
    private fun BigDecimal.r4() = setScale(4, RoundingMode.HALF_UP)

    fun calculate(): CalculationResult {

        // ── Step 1: Gross & applied line-item discounts ───────────────────────
        // Discounts are applied in the order they appear in the global catalogue,
        // but only those whose id is in item.appliedDiscountIds.
        data class Step1(
            val item: Item,
            val gross: BigDecimal,
            val breakdown: List<Pair<Discount, BigDecimal>>,
            val totalDisc: BigDecimal,
            val net: BigDecimal
        )

        val step1 = items.map { item ->
            val gross = (item.unitPrice * item.qty).r2()
            var running = gross

            // Filter catalogue to only applied discounts, keeping catalogue order
            val appliedDiscs = lineDiscounts.filter { it.id in item.appliedDiscountIds }
            val breakdown = appliedDiscs.map { disc ->
                val amt = when (disc.type) {
                    DiscountType.PERCENT -> (running * disc.value / BigDecimal(100)).r2()
                    DiscountType.FIXED   -> disc.value.r2().min(running)
                }
                running = (running - amt).r2()
                disc to amt
            }
            val totalDisc = breakdown.sumOfDecimal { it.second }
            Step1(item, gross, breakdown, totalDisc, running)
        }

        val grossTotal        = step1.sumOfDecimal { it.gross }
        val totalLineDiscount = step1.sumOfDecimal { it.totalDisc }
        val subtotal1         = step1.sumOfDecimal { it.net }

        // ── Step 2: Receipt-level discounts, proportionally RR-allocated ──────
        data class Step2(
            val s1: Step1,
            val receiptDiscBreakdown: List<Pair<Discount, BigDecimal>>,
            val totalReceiptDisc: BigDecimal,
            val net: BigDecimal
        )

        var runningSub = subtotal1
        val receiptDiscountAmounts = receiptDiscounts.map { disc ->
            val amt = when (disc.type) {
                DiscountType.PERCENT -> (runningSub * disc.value / BigDecimal(100)).r2()
                DiscountType.FIXED   -> disc.value.r2().min(runningSub)
            }
            runningSub = (runningSub - amt).r2()
            disc to amt
        }
        val totalReceiptDiscountAmount = receiptDiscountAmounts.sumOfDecimal { it.second }

        // Allocate each receipt discount to items proportionally via RR
        val itemNets = step1.map { it.net }.toMutableList()
        val perItemPerReceiptDisc = receiptDiscountAmounts.map { (_, discTotal) ->
            val allocs = roundRobin(discTotal, itemNets, items.size)
            allocs.forEachIndexed { i, a -> itemNets[i] = (itemNets[i] - a).r2() }
            allocs
        }

        val step2 = step1.mapIndexed { i, s1 ->
            val breakdown = receiptDiscountAmounts.mapIndexed { dIdx, (disc, _) ->
                disc to perItemPerReceiptDisc[dIdx][i]
            }
            val totalReceiptDisc = breakdown.sumOfDecimal { it.second }
            Step2(s1, breakdown, totalReceiptDisc, (s1.net - totalReceiptDisc).r2())
        }

        val subtotal2 = step2.sumOfDecimal { it.net }

        // ── Step 3: Per-item inclusive-tax base extraction ────────────────────
        data class Step3(val s2: Step2, val inclusiveTaxBase: BigDecimal)

        val step3 = step2.map { s2 ->
            val itemInclTaxes = taxes.filter {
                it.id in s2.s1.item.appliedTaxIds && it.mode == TaxMode.INCLUDE && it.rate > BigDecimal.ZERO
            }
            val inclRateSum = itemInclTaxes.sumOfDecimal { it.rate }
            val inclBase = if (inclRateSum > BigDecimal.ZERO)
                (s2.net / (BigDecimal.ONE + inclRateSum / BigDecimal(100))).r4()
            else s2.net
            Step3(s2, inclBase)
        }

        // ── Step 4: Aggregate tax amount per tax (opted-in items only) ────────
        val taxResults = mutableListOf<TaxResult>()

        taxes.forEach { tax ->
            if (tax.rate <= BigDecimal.ZERO) return@forEach
            val eligibleStep3 = step3.filter { tax.id in it.s2.s1.item.appliedTaxIds }
            if (eligibleStep3.isEmpty()) return@forEach

            val base: BigDecimal = when (tax.mode) {
                TaxMode.INCLUDE -> eligibleStep3.sumOfDecimal { it.inclusiveTaxBase }
                TaxMode.EXCLUDE -> {
                    val eligibleNet = eligibleStep3.sumOfDecimal { it.s2.net }
                    if (tax.taxOnTax) {
                        val prevExclSum = taxResults
                            .filter { it.tax.mode == TaxMode.EXCLUDE }
                            .sumOfDecimal { it.amount }
                        (eligibleNet + prevExclSum).r2()
                    } else eligibleNet
                }
            }
            val amount = (base * tax.rate / BigDecimal(100)).r2()
            taxResults.add(TaxResult(tax, base, amount))
        }

        val inclusiveTaxTotal = taxResults.filter { it.tax.mode == TaxMode.INCLUDE }.sumOfDecimal { it.amount }
        val exclusiveTaxTotal = taxResults.filter { it.tax.mode == TaxMode.EXCLUDE }.sumOfDecimal { it.amount }

        // ── Step 5: Round-robin distribute each tax to its opted-in items ─────
        val taxByItemByTaxId: Array<MutableMap<String, BigDecimal>> = Array(items.size) { mutableMapOf() }

        taxResults.forEach { taxResult ->
            val eligibleIndices = step3.indices.filter { taxResult.tax.id in step3[it].s2.s1.item.appliedTaxIds }
            if (eligibleIndices.isEmpty()) return@forEach
            val bases = eligibleIndices.map { idx ->
                when (taxResult.tax.mode) {
                    TaxMode.INCLUDE -> step3[idx].inclusiveTaxBase
                    TaxMode.EXCLUDE -> step3[idx].s2.net
                }
            }
            val allocs = roundRobin(taxResult.amount, bases, eligibleIndices.size)
            allocs.forEachIndexed { pos, amt -> taxByItemByTaxId[eligibleIndices[pos]][taxResult.tax.id] = amt }
        }

        // ── Step 6: Build ProcessedItem results ───────────────────────────────
        val processedItems = step3.mapIndexed { idx, s3 ->
            val taxMap         = taxByItemByTaxId[idx].toMap()
            val totalItemTax   = taxMap.values.sumOfDecimal { it }
            val exclTaxForItem = taxMap
                .filter { (taxId, _) -> taxResults.find { it.tax.id == taxId }?.tax?.mode == TaxMode.EXCLUDE }
                .values.sumOfDecimal { it }
            val lineTotal = (s3.s2.net + exclTaxForItem).r2()

            ProcessedItem(
                item                        = s3.s2.s1.item,
                gross                       = s3.s2.s1.gross,
                lineDiscountBreakdown       = s3.s2.s1.breakdown,
                totalLineDiscount           = s3.s2.s1.totalDisc,
                netAfterLine                = s3.s2.s1.net,
                receiptDiscountAllocBreakdown = s3.s2.receiptDiscBreakdown,
                totalReceiptDiscountAlloc   = s3.s2.totalReceiptDisc,
                netAfterInvoice             = s3.s2.net,
                inclusiveTaxBase            = s3.inclusiveTaxBase,
                taxByTaxId                  = taxMap,
                totalTax                    = totalItemTax,
                lineTotal                   = lineTotal
            )
        }

        // ── Step 7: Fixed charges & grand total ───────────────────────────────
        val fixedChargeResults     = fixedCharges.map { fc -> fc to fc.value.r2() }
        val totalFixedChargeAmount = fixedChargeResults.sumOfDecimal { it.second }
        val grandTotal             = (subtotal2 + exclusiveTaxTotal + totalFixedChargeAmount).r2()

        return CalculationResult(
            items                      = processedItems,
            grossTotal                 = grossTotal,
            totalLineDiscount          = totalLineDiscount,
            subtotal1                  = subtotal1,
            receiptDiscountAmounts     = receiptDiscountAmounts,
            totalReceiptDiscountAmount = totalReceiptDiscountAmount,
            subtotal2                  = subtotal2,
            taxResults                 = taxResults,
            inclusiveTaxTotal          = inclusiveTaxTotal,
            exclusiveTaxTotal          = exclusiveTaxTotal,
            fixedCharges               = fixedChargeResults,
            totalFixedChargeAmount     = totalFixedChargeAmount,
            grandTotal                 = grandTotal
        )
    }

    // ── Round-robin proportional allocation ───────────────────────────────────
    private fun roundRobin(total: BigDecimal, bases: List<BigDecimal>, count: Int): List<BigDecimal> {
        if (total == BigDecimal.ZERO || count == 0) return List(count) { BigDecimal.ZERO }
        val baseSum = bases.sumOfDecimal { it }
        val exact   = bases.map { b -> if (baseSum > BigDecimal.ZERO) b * total / baseSum else BigDecimal.ZERO }
        val floored = exact.map { it.movePointRight(2).setScale(0, RoundingMode.DOWN).movePointLeft(2) }
        val remainder = ((total - floored.sumOfDecimal { it }) * BigDecimal(100))
            .setScale(0, RoundingMode.HALF_UP).toInt()
        val result = floored.toMutableList()
        repeat(remainder) { i ->
            result[i % count] = (result[i % count] + BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP)
        }
        return result
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Extension
// ─────────────────────────────────────────────────────────────────────────────

fun <T> Iterable<T>.sumOfDecimal(selector: (T) -> BigDecimal): BigDecimal =
    fold(BigDecimal.ZERO) { acc, v -> acc.add(selector(v)) }