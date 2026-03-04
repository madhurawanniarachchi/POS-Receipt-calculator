package com.madhura.clodeposcal

// ═══════════════════════════════════════════════════════════════════════════════
// POS Tax Calculator — fulfills the spec in the requirement document
//
// Key differences from the previous POSTaxCalculator:
//  • Item discounts use the ORIGINAL base, not a running total
//  • Receipt-discount distribution uses largest-remainder (round-robin pennies)
//  • Tax ordering: BEFORE taxes first, then AFTER taxes
//  • INCLUDE tax formula: taxable * rate / (100 + sumIncludeRates)
//  • AFTER taxes: taxable base = afterReceiptDiscount + previousTaxes
//  • totalInclTax = afterReceiptDiscount + sum(EXCLUDE tax amounts only)
// ═══════════════════════════════════════════════════════════════════════════════

import java.math.BigDecimal
import java.math.RoundingMode

// ─────────────────────────────────────────────────────────────────────────────
// Enums
// ─────────────────────────────────────────────────────────────────────────────

enum class DiscountType { PERCENT, FIXED }
enum class TaxMode      { INCLUDE, EXCLUDE }
enum class TaxOrder     { BEFORE, AFTER }   // NEW: controls ordering within tax list

// ─────────────────────────────────────────────────────────────────────────────
// Data Models
// ─────────────────────────────────────────────────────────────────────────────

data class Discount(
    val id: String,
    val label: String = "",
    val type: DiscountType = DiscountType.PERCENT,
    val value: BigDecimal = BigDecimal.ZERO
)

/**
 * Tax definition.
 * [mode]     — INCLUDE (embedded in price) or EXCLUDE (added on top).
 * [taxOrder] — BEFORE (applied before other taxes) or AFTER (taxable base includes
 *              previously computed taxes — "tax on tax").
 */
data class Tax(
    val id: String,
    val name: String,
    val rate: BigDecimal,
    val mode: TaxMode = TaxMode.EXCLUDE,
    val taxOrder: TaxOrder = TaxOrder.BEFORE   // replaces the old `taxOnTax: Boolean`
)

data class Item(
    val id: String,
    val name: String,
    val qty: BigDecimal,
    val unitPrice: BigDecimal,
    val appliedDiscountIds: Set<String> = emptySet(),
    val appliedTaxIds: Set<String> = emptySet()
)

data class FixedCharge(
    val id: String,
    val label: String = "Service Fee",
    val value: BigDecimal = BigDecimal.ZERO
)

// ─────────────────────────────────────────────────────────────────────────────
// Result / Output Models
// ─────────────────────────────────────────────────────────────────────────────

/** Per-tax detail attached to a processed item. */
data class TaxLine(
    val taxId: String,
    val name: String,
    val ratePercent: BigDecimal,
    val taxMode: TaxMode,
    val taxOrder: TaxOrder,
    val taxableBase: BigDecimal,
    val taxAmount: BigDecimal
)

data class ProcessedItem(
    val item: Item,
    /** unitPrice × qty, rounded to 2dp */
    val gross: BigDecimal,
    /** Breakdown of each applied line discount (discount, computed amount). */
    val lineDiscountBreakdown: List<Pair<Discount, BigDecimal>>,
    val totalLineDiscount: BigDecimal,
    /** gross − totalLineDiscount */
    val netAfterLine: BigDecimal,
    /** Proportional share of the receipt-level discount. */
    val receiptDiscountShare: BigDecimal,
    /** netAfterLine − receiptDiscountShare */
    val afterReceiptDiscount: BigDecimal,
    /** Per-tax details, in processing order (BEFORE then AFTER). */
    val taxLines: List<TaxLine>,
    /** afterReceiptDiscount (pre-tax base, for display) */
    val totalExclTax: BigDecimal,
    /** Σ all taxAmounts (INCLUDE + EXCLUDE) */
    val totalTax: BigDecimal,
    /** afterReceiptDiscount + Σ EXCLUDE tax amounts */
    val totalInclTax: BigDecimal
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
    /** subtotal2 + exclusiveTaxTotal + totalFixedChargeAmount */
    val grandTotal: BigDecimal
)

// ─────────────────────────────────────────────────────────────────────────────
// Calculator Engine
// ─────────────────────────────────────────────────────────────────────────────

class POSTaxCalculator(
    private val items: List<Item>,
    private val lineDiscounts: List<Discount>,
    private val taxes: List<Tax>,
    private val receiptDiscounts: List<Discount> = emptyList(),
    private val fixedCharges: List<FixedCharge> = emptyList()
) {

    companion object {
        private val ZERO = BigDecimal.ZERO
        private val HUNDRED = BigDecimal(100)
    }

    /** Round to 2 decimal places, HALF_UP. */
    private fun BigDecimal.r2() = setScale(2, RoundingMode.HALF_UP)

    /** Clamp to >= 0 then round to 2dp. */
    private fun BigDecimal.clampR2() = this.max(ZERO).r2()

    fun calculate(): CalculationResult {

        // ══════════════════════════════════════════════════════════════════════
        // STEP 1 — Base price & item-level discounts
        //
        // Each discount amount is computed from the ORIGINAL base (not running).
        // Cumulative deduction is tracked separately and clamped after each step.
        // ══════════════════════════════════════════════════════════════════════

        data class Step1Result(
            val item: Item,
            val gross: BigDecimal,
            val discountBreakdown: List<Pair<Discount, BigDecimal>>,
            val totalDiscount: BigDecimal,
            val netAfterLine: BigDecimal
        )

        val step1 = items.map { item ->
            val base    = (item.unitPrice * item.qty).r2()
            var running = base

            val appliedDiscs = lineDiscounts.filter { it.id in item.appliedDiscountIds }
            val breakdown = appliedDiscs.map { disc ->
                // Discount amount always from ORIGINAL base
                val raw = when (disc.type) {
                    DiscountType.PERCENT -> (base * disc.value / HUNDRED).r2()
                    DiscountType.FIXED   -> disc.value.r2()
                }
                // Clamp so we don't go negative
                val amt = raw.min(running).clampR2()
                running = (running - amt).clampR2()
                disc to amt
            }
            val totalDisc = breakdown.sumOfDecimal { it.second }
            Step1Result(item, base, breakdown, totalDisc, running)
        }

        val grossTotal        = step1.sumOfDecimal { it.gross }
        val totalLineDiscount = step1.sumOfDecimal { it.totalDiscount }
        val subtotal1         = step1.sumOfDecimal { it.netAfterLine }   // = receiptDiscountBase

        // ══════════════════════════════════════════════════════════════════════
        // STEP 2 — Receipt-level discounts applied sequentially
        //
        // Percent discounts use the current running subtotal as base.
        // Fixed discounts use their nominal value.
        // Then distribute totalReceiptDiscount to items via largest-remainder.
        // ══════════════════════════════════════════════════════════════════════

        var runningSub = subtotal1
        val receiptDiscountAmounts = receiptDiscounts.map { disc ->
            val amt = when (disc.type) {
                DiscountType.PERCENT -> (runningSub * disc.value / HUNDRED).r2()
                DiscountType.FIXED   -> disc.value.r2().min(runningSub)
            }.clampR2()
            runningSub = (runningSub - amt).clampR2()
            disc to amt
        }
        val totalReceiptDiscountAmount = receiptDiscountAmounts.sumOfDecimal { it.second }
        val subtotal2 = (subtotal1 - totalReceiptDiscountAmount).r2()

        // Distribute totalReceiptDiscountAmount to items using largest-remainder method
        val perItemReceiptDisc: List<BigDecimal> = largestRemainder(
            total  = totalReceiptDiscountAmount,
            bases  = step1.map { it.netAfterLine },
            baseSum = subtotal1
        )

        // ══════════════════════════════════════════════════════════════════════
        // STEP 3 — Per-item afterReceiptDiscount
        // ══════════════════════════════════════════════════════════════════════

        data class Step3Result(
            val s1: Step1Result,
            val receiptDiscShare: BigDecimal,
            val afterReceiptDiscount: BigDecimal
        )

        val step3 = step1.mapIndexed { idx, s1 ->
            val share = perItemReceiptDisc[idx]
            val after = (s1.netAfterLine - share).clampR2()
            Step3Result(s1, share, after)
        }

        // ══════════════════════════════════════════════════════════════════════
        // STEP 4 — Per-item tax calculation
        //
        // Processing order: BEFORE taxes first (in catalogue order),
        //                   then AFTER taxes (in catalogue order).
        //
        // INCLUDE formula : taxable * rate / (100 + sumIncludeRates)
        // EXCLUDE BEFORE  : taxable = afterReceiptDiscount
        // EXCLUDE AFTER   : taxable = afterReceiptDiscount + previousTaxes
        // ══════════════════════════════════════════════════════════════════════

        data class ItemTaxResult(
            val s3: Step3Result,
            val taxLines: List<TaxLine>,
            val totalExclTaxAmt: BigDecimal,  // Σ EXCLUDE tax amounts
            val totalAllTaxAmt: BigDecimal    // Σ INCLUDE + EXCLUDE
        )

        val itemTaxResults = step3.map { s3 ->
            val itemTaxes = taxes.filter { it.id in s3.s1.item.appliedTaxIds && it.rate > ZERO }

            // Sum of INCLUDE rates for this item (used in INCLUDE formula denominator)
            val sumIncludeRates = itemTaxes
                .filter { it.mode == TaxMode.INCLUDE }
                .sumOfDecimal { it.rate }

            // Sort: BEFORE first, then AFTER
            val orderedTaxes = itemTaxes.filter { it.taxOrder == TaxOrder.BEFORE } +
                    itemTaxes.filter { it.taxOrder == TaxOrder.AFTER }

            var previousTaxes = ZERO  // tracks cumulative EXCLUDE taxes for AFTER calculation

            val taxLines = orderedTaxes.map { tax ->
                val taxableBase: BigDecimal = when (tax.taxOrder) {
                    TaxOrder.BEFORE -> s3.afterReceiptDiscount
                    TaxOrder.AFTER  -> (s3.afterReceiptDiscount + previousTaxes).r2()
                }

                val taxAmount: BigDecimal = when (tax.mode) {
                    TaxMode.EXCLUDE -> (taxableBase * tax.rate / HUNDRED).r2()
                    TaxMode.INCLUDE -> {
                        val denom = HUNDRED + sumIncludeRates
                        if (denom > ZERO) (taxableBase * tax.rate / denom).r2() else ZERO
                    }
                }

                // Accumulate for AFTER-type taxes
                if (tax.mode == TaxMode.EXCLUDE) {
                    previousTaxes = (previousTaxes + taxAmount).r2()
                }

                TaxLine(
                    taxId       = tax.id,
                    name        = tax.name,
                    ratePercent = tax.rate,
                    taxMode     = tax.mode,
                    taxOrder    = tax.taxOrder,
                    taxableBase = taxableBase,
                    taxAmount   = taxAmount
                )
            }

            val totalExclTaxAmt = taxLines
                .filter { it.taxMode == TaxMode.EXCLUDE }
                .sumOfDecimal { it.taxAmount }
            val totalAllTaxAmt  = taxLines.sumOfDecimal { it.taxAmount }

            ItemTaxResult(s3, taxLines, totalExclTaxAmt, totalAllTaxAmt)
        }

        // ══════════════════════════════════════════════════════════════════════
        // STEP 5 — Aggregate TaxResults (for receipt-level display / RR verify)
        //
        // For each tax in catalogue, sum amounts across opted-in items.
        // ══════════════════════════════════════════════════════════════════════

        val taxResults: List<TaxResult> = taxes.mapNotNull { tax ->
            val linesForTax = itemTaxResults.flatMap { it.taxLines }.filter { it.taxId == tax.id }
            if (linesForTax.isEmpty()) return@mapNotNull null
            val base   = linesForTax.sumOfDecimal { it.taxableBase }
            val amount = linesForTax.sumOfDecimal { it.taxAmount }
            TaxResult(tax, base, amount)
        }

        val inclusiveTaxTotal = taxResults
            .filter { it.tax.mode == TaxMode.INCLUDE }
            .sumOfDecimal { it.amount }
        val exclusiveTaxTotal = taxResults
            .filter { it.tax.mode == TaxMode.EXCLUDE }
            .sumOfDecimal { it.amount }

        // ══════════════════════════════════════════════════════════════════════
        // STEP 6 — Build ProcessedItem list
        // ══════════════════════════════════════════════════════════════════════

        val processedItems = itemTaxResults.map { itr ->
            val after = itr.s3.afterReceiptDiscount
            ProcessedItem(
                item                   = itr.s3.s1.item,
                gross                  = itr.s3.s1.gross,
                lineDiscountBreakdown  = itr.s3.s1.discountBreakdown,
                totalLineDiscount      = itr.s3.s1.totalDiscount,
                netAfterLine           = itr.s3.s1.netAfterLine,
                receiptDiscountShare   = itr.s3.receiptDiscShare,
                afterReceiptDiscount   = after,
                taxLines               = itr.taxLines,
                totalExclTax           = after,
                totalTax               = itr.totalAllTaxAmt,
                totalInclTax           = (after + itr.totalExclTaxAmt).r2()
            )
        }

        // ══════════════════════════════════════════════════════════════════════
        // STEP 7 — Fixed charges & grand total
        // ══════════════════════════════════════════════════════════════════════

        val fixedChargeResults     = fixedCharges.map { fc -> fc to fc.value.r2() }
        val totalFixedChargeAmount = fixedChargeResults.sumOfDecimal { it.second }

        // grandTotal = subtotal2 + exclusiveTaxTotal + fixedCharges
        val grandTotal = (subtotal2 + exclusiveTaxTotal + totalFixedChargeAmount).r2()

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

    // ══════════════════════════════════════════════════════════════════════════
    // Largest-Remainder proportional distribution
    //
    // Distributes [total] across items proportional to their [bases].
    // Any penny remainder is assigned to items with the largest fractional parts
    // (ties broken by list order), guaranteeing Σ allocations == total exactly.
    // ══════════════════════════════════════════════════════════════════════════

    private fun largestRemainder(
        total: BigDecimal,
        bases: List<BigDecimal>,
        baseSum: BigDecimal
    ): List<BigDecimal> {
        if (total == ZERO || bases.isEmpty()) return List(bases.size) { ZERO }

        // Exact shares at high precision
        val exact = bases.map { b ->
            if (baseSum > ZERO) (b * total / baseSum) else ZERO
        }

        // Floor to cents
        val floored = exact.map { it.setScale(2, RoundingMode.DOWN) }

        // How many extra pennies remain?
        val distributed = floored.sumOfDecimal { it }
        val remainderCents = ((total - distributed) * BigDecimal(100))
            .setScale(0, RoundingMode.HALF_UP).toInt()

        // Fractional parts (the 3rd decimal onward) determine priority
        val fractions = exact.mapIndexed { idx, e -> idx to (e - floored[idx]) }
            .sortedByDescending { it.second }

        val result = floored.toMutableList()
        repeat(remainderCents) { i ->
            val idx = fractions[i % fractions.size].first
            result[idx] = (result[idx] + BigDecimal("0.01")).r2()
        }
        return result
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Extension
// ─────────────────────────────────────────────────────────────────────────────

fun <T> Iterable<T>.sumOfDecimal(selector: (T) -> BigDecimal): BigDecimal =
    fold(BigDecimal.ZERO) { acc, v -> acc.add(selector(v)) }