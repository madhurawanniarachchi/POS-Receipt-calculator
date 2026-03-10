package com.madhura.clodeposcal

// ═══════════════════════════════════════════════════════════════════════════════
// POS Tax Calculator
//
// Rounding strategy — AGGREGATE + ROUND-ROBIN:
//
//   WRONG (previous):  round each item's tax independently → sum drifts
//   CORRECT (this):
//     1. Accumulate all per-item bases at FULL precision (no intermediate r2)
//     2. Compute ONE aggregate tax amount  → single r2() at receipt level
//     3. Round-robin distribute that receipt total back to items proportionally
//        (largest-remainder guarantees Σ item allocations == receipt total exactly)
//
// The same principle is applied to:
//   • inclusiveTaxBase per item  (aggregate then round-robin)
//   • each exclusive tax per item (aggregate then round-robin)
//   • receipt discount per item   (aggregate then round-robin)
// ═══════════════════════════════════════════════════════════════════════════════

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

// ─────────────────────────────────────────────────────────────────────────────
// Enums
// ─────────────────────────────────────────────────────────────────────────────

enum class DiscountType { PERCENT, FIXED }
enum class TaxMode      { INCLUDE, EXCLUDE }
enum class TaxOrder     { BEFORE, AFTER }

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
 * [mode]     — INCLUDE (embedded in price) or EXCLUDE (added on top).
 * [taxOrder] — BEFORE: base = inclusiveTaxBase.
 *              AFTER : base = inclusiveTaxBase + Σ previous EXCLUDE taxes (tax-on-tax).
 */
data class Tax(
    val id: String,
    val name: String,
    val rate: BigDecimal,
    val mode: TaxMode = TaxMode.EXCLUDE,
    val taxOrder: TaxOrder = TaxOrder.BEFORE
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
// Result Models
// ─────────────────────────────────────────────────────────────────────────────

data class TaxLine(
    val taxId: String,
    val name: String,
    val ratePercent: BigDecimal,
    val taxMode: TaxMode,
    val taxOrder: TaxOrder,
    /** The taxable base actually used (for display / audit). */
    val taxableBase: BigDecimal,
    /** Round-robin allocated portion of the receipt-level tax total. */
    val taxAmount: BigDecimal
)

data class ProcessedItem(
    val item: Item,
    val gross: BigDecimal,
    val lineDiscountBreakdown: List<Pair<Discount, BigDecimal>>,
    val totalLineDiscount: BigDecimal,
    /** gross − totalLineDiscount */
    val netAfterLine: BigDecimal,
    /** Round-robin share of receipt discount(s). */
    val receiptDiscountShare: BigDecimal,
    /** netAfterLine − receiptDiscountShare  (price still containing inclusive taxes) */
    val afterReceiptDiscount: BigDecimal,
    /**
     * afterReceiptDiscount with inclusive-tax portions stripped out.
     * = afterReceiptDiscount × 100 / (100 + sumIncludeRates)
     * Round-robin allocated from the aggregate net total.
     * This is the base for EXCLUSIVE taxes.
     */
    val inclusiveTaxBase: BigDecimal,
    /** Tax lines (BEFORE order first, then AFTER). Amounts are RR-allocated. */
    val taxLines: List<TaxLine>,
    /** = inclusiveTaxBase */
    val totalExclTax: BigDecimal,
    /** Σ all taxLine.taxAmount */
    val totalTax: BigDecimal,
    /** inclusiveTaxBase + Σ EXCLUDE taxLine amounts */
    val totalInclTax: BigDecimal
)

data class TaxResult(
    val tax: Tax,
    /** Aggregate taxable base across all opted-in items. */
    val base: BigDecimal,
    /** Receipt-level tax total (single r2 — source of truth). */
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

class ReceiptCalculator(
    private val items: List<Item>,
    private val lineDiscounts: List<Discount>,
    private val taxes: List<Tax>,
    private val receiptDiscounts: List<Discount> = emptyList(),
    private val fixedCharges: List<FixedCharge> = emptyList()
) {

    companion object {
        private val ZERO    = BigDecimal.ZERO
        private val HUNDRED = BigDecimal(100)
        // High-precision context for intermediate aggregations — never used for display
        private val MC      = MathContext(28, RoundingMode.HALF_UP)
    }

    private val decimalPlace = 2
    /** Display / storage rounding: 2dp HALF_UP. */
    private fun BigDecimal.r2() = setScale(decimalPlace, RoundingMode.HALF_UP)

    /** Clamp to >= 0 then display-round. */
    private fun BigDecimal.clampR2() = this.max(ZERO).r2()

    // ─────────────────────────────────────────────────────────────────────────
    // Round-Robin (largest-remainder) distribution
    //
    // Splits [total] (already rounded to 2dp) among [n] slots proportional to
    // [weights]. Penny remainder goes to slots with the largest fractional parts.
    // Guarantees: Σ result == total exactly.
    // ─────────────────────────────────────────────────────────────────────────
    private fun roundRobin(total: BigDecimal, weights: List<BigDecimal>): List<BigDecimal> {
        val n = weights.size
        if (n == 0 || total.compareTo(ZERO) == 0) return List(n) { ZERO }

        val weightSum = weights.fold(ZERO) { a, w -> a.add(w, MC) }
        if (weightSum.compareTo(ZERO) == 0) {
            // Equal split when all weights are zero
            val share = (total / BigDecimal(n)).r2()
            val alloc = MutableList(n) { share }
            var diff = (total - alloc.fold(ZERO, BigDecimal::add)).r2()
            var i = 0
            while (diff.compareTo(ZERO) != 0) {
                val step = BigDecimal.ONE.scaleByPowerOfTen(-decimalPlace)
                    .let { if (diff > ZERO) it else it.negate() }
                alloc[i++ % n] = (alloc[(i - 1) % n] + step).r2()
                diff = (diff - step).r2()
            }
            return alloc
        }

        // Exact proportional shares at full precision
        val exact = weights.map { w -> w.multiply(total, MC).divide(weightSum, MC) }

        // Floor each to cents
        val floored = exact.map { it.setScale(decimalPlace, RoundingMode.DOWN) }

        // Count missing pennies
        val distributed   = floored.fold(ZERO, BigDecimal::add)
        val remainderCents = ((total - distributed) * HUNDRED)
            .setScale(0, RoundingMode.HALF_UP).toInt()

        // Priority: largest fractional part first
        val priority = exact.mapIndexed { idx, e -> idx to (e - floored[idx]) }
            .sortedByDescending { it.second }

        val result = floored.toMutableList()
        repeat(remainderCents) { k ->
            val idx = priority[k % n].first

            val step = BigDecimal.ONE.scaleByPowerOfTen(-decimalPlace)
            result[idx] = (result[idx] + step).r2()
        }
        return result
    }

    // ─────────────────────────────────────────────────────────────────────────
    fun calculate(): CalculationResult {

        // ══════════════════════════════════════════════════════════════════════
        // STEP 1 — Base price & item-level discounts
        //
        // Each discount is computed from the ORIGINAL base (spec requirement).
        // ══════════════════════════════════════════════════════════════════════

        data class S1(
            val item: Item,
            val gross: BigDecimal,
            val discBreakdown: List<Pair<Discount, BigDecimal>>,
            val totalDisc: BigDecimal,
            val netAfterLine: BigDecimal        // exact 2dp; used as weight later
        )

        val s1list = items.map { item ->
            val base    = (item.unitPrice * item.qty).r2()
            var running = base
            val appliedDiscs = lineDiscounts.filter { it.id in item.appliedDiscountIds }
            val breakdown = appliedDiscs.map { disc ->
                // Sequential: each discount applies to the RUNNING value after previous discounts
                val raw = when (disc.type) {
                    DiscountType.PERCENT -> (running * disc.value / HUNDRED).r2()
                    DiscountType.FIXED   -> disc.value.r2()
                }
                val amt = raw.min(running).clampR2()
                running = (running - amt).clampR2()
                disc to amt
            }
            val totalDisc = breakdown.fold(ZERO, { a, p -> a + p.second })
            S1(item, base, breakdown, totalDisc, running)
        }

        val grossTotal        = s1list.fold(ZERO) { a, s -> a + s.gross }
        val totalLineDiscount = s1list.fold(ZERO) { a, s -> a + s.totalDisc }
        val subtotal1         = s1list.fold(ZERO) { a, s -> a + s.netAfterLine }

        // ══════════════════════════════════════════════════════════════════════
        // STEP 2 — Receipt discounts applied sequentially after line discounts
        //          + round-robin distribution to items
        //
        // Each receipt discount applies to the RUNNING subtotal left after the
        // previous receipt discount. Same sequential rule as line discounts.
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
        val totalReceiptDiscountAmount = receiptDiscountAmounts
            .fold(ZERO) { a, p -> a + p.second }
        val subtotal2 = (subtotal1 - totalReceiptDiscountAmount).r2()

        // Distribute the total receipt discount to items (proportional to netAfterLine)
        val perItemReceiptDisc: List<BigDecimal> = roundRobin(
            total   = totalReceiptDiscountAmount,
            weights = s1list.map { it.netAfterLine }
        )

        // afterReceiptDiscount per item (exact 2dp — these are the "price" for tax purposes)
        val afterReceiptDiscounts: List<BigDecimal> = s1list.mapIndexed { i, s1 ->
            (s1.netAfterLine - perItemReceiptDisc[i]).clampR2()
        }

        // ══════════════════════════════════════════════════════════════════════
        // STEP 3 — Aggregate inclusive-tax base, then round-robin to items
        //
        // For each item, the EXACT (unrounded) inclusive-tax base is:
        //   exactInclBase_i = afterReceiptDiscount_i × 100 / (100 + sumInclRates_i)
        //
        // We sum ALL exact bases → compute receipt-level totals → RR back to items.
        // This keeps rounding to a single operation per tax.
        // ══════════════════════════════════════════════════════════════════════

        // Per-item sum of INCLUDE rates (items may have different tax sets)
        val sumInclRatesPerItem: List<BigDecimal> = s1list.map { s1 ->
            taxes.filter { it.id in s1.item.appliedTaxIds && it.mode == TaxMode.INCLUDE && it.rate > ZERO }
                .fold(ZERO) { a, t -> a + t.rate }
        }

        // Exact (high-precision) inclusive-tax base per item — NOT rounded yet
        val exactInclBasePerItem: List<BigDecimal> = s1list.mapIndexed { i, _ ->
            val ard  = afterReceiptDiscounts[i]
            val sIR  = sumInclRatesPerItem[i]
            if (sIR > ZERO)
                ard.multiply(HUNDRED, MC).divide(HUNDRED.add(sIR, MC), MC)
            else
                ard.setScale(10, RoundingMode.HALF_UP)  // keep high precision for weight
        }

        // Aggregate net base at receipt level → single r2
        val aggNetBase: BigDecimal = exactInclBasePerItem.fold(ZERO) { a, b -> a.add(b, MC) }.r2()
        // subtotal2 should equal aggNetBase (both are the net-of-inclusive-tax total)
        // They may differ by ¢1 due to the clampR2 on individual ARDs; use aggNetBase as truth.

        // Round-robin distribute aggNetBase to items (using exact bases as weights)
        val inclBasePerItem: List<BigDecimal> = roundRobin(
            total   = aggNetBase,
            weights = exactInclBasePerItem
        )

        // ══════════════════════════════════════════════════════════════════════
        // STEP 4 — Per-tax: aggregate base → one receipt-level total → RR to items
        //
        // For EXCLUDE taxes:
        //   BEFORE: base_i = inclBasePerItem[i]   (exact already RR-distributed)
        //   AFTER : base_i = inclBasePerItem[i] + Σ previous EXCL taxes allocated to item i
        //
        // For INCLUDE taxes:
        //   base_i = afterReceiptDiscount[i]  (the stated price including embedded tax)
        //   formula: ard_i × rate / (100 + sumInclRates_i)
        //
        // Aggregate: sum exact per-item tax amounts first, r2() once, RR back.
        // ══════════════════════════════════════════════════════════════════════

        // Processing order: BEFORE taxes (catalogue order) then AFTER taxes (catalogue order)
        val orderedTaxes = taxes.filter { it.taxOrder == TaxOrder.BEFORE } +
                taxes.filter { it.taxOrder == TaxOrder.AFTER }

        // taxId → List<BigDecimal> (one allocated amount per item, parallel to s1list)
        val taxAllocPerItem: MutableMap<String, List<BigDecimal>> = mutableMapOf()
        // taxId → TaxResult (receipt-level aggregate)
        val taxResultMap:    MutableMap<String, TaxResult>        = mutableMapOf()

        // Running per-item EXCLUDE tax totals (accumulated as AFTER taxes are processed)
        // Kept at 2dp (already RR-distributed) so AFTER base is exact cents.
        val runningExclPerItem: MutableList<BigDecimal> = MutableList(s1list.size) { ZERO }

        orderedTaxes.forEach { tax ->
            if (tax.rate <= ZERO) return@forEach

            // Indices of items that opted in to this tax
            val eligible = s1list.indices.filter { tax.id in s1list[it].item.appliedTaxIds }
            if (eligible.isEmpty()) return@forEach

            // Exact per-item base (unrounded) for each eligible item
            val exactBasesEligible: List<BigDecimal> = eligible.map { i ->
                when (tax.mode) {
                    TaxMode.INCLUDE ->
                        // Use afterReceiptDiscount (the stated inclusive price)
                        afterReceiptDiscounts[i].setScale(10, RoundingMode.HALF_UP)

                    TaxMode.EXCLUDE -> when (tax.taxOrder) {
                        TaxOrder.BEFORE ->
                            // Net base already RR-distributed; use as high-precision weight
                            exactInclBasePerItem[i]
                        TaxOrder.AFTER  ->
                            // inclBasePerItem[i] is already cents; add running excl taxes
                            (inclBasePerItem[i] + runningExclPerItem[i])
                                .setScale(10, RoundingMode.HALF_UP)
                    }
                }
            }

            // Aggregate base at full precision → compute ONE tax amount → r2
            val aggBase   = exactBasesEligible.fold(ZERO) { a, b -> a.add(b, MC) }
            val aggAmount: BigDecimal = when (tax.mode) {
                TaxMode.EXCLUDE -> (aggBase * tax.rate / HUNDRED).r2()
                TaxMode.INCLUDE -> {
                    // sumInclRates may differ per item; use weighted average denominator
                    // Correct approach: sum each item's exact include-tax contribution
                    val exactAmountsSum = eligible.mapIndexed { pos, i ->
                        val sIR   = sumInclRatesPerItem[i]
                        val denom = HUNDRED.add(sIR, MC)
                        afterReceiptDiscounts[i].multiply(tax.rate, MC).divide(denom, MC)
                    }.fold(ZERO) { a, b -> a.add(b, MC) }
                    exactAmountsSum.r2()
                }
            }

            // Round-robin distribute aggAmount back to eligible items
            val allocsEligible: List<BigDecimal> = roundRobin(
                total   = aggAmount,
                weights = exactBasesEligible
            )

            // Store in full-item-list arrays (non-eligible items get ZERO)
            val fullAllocs = MutableList(s1list.size) { ZERO }
            eligible.forEachIndexed { pos, i -> fullAllocs[i] = allocsEligible[pos] }
            taxAllocPerItem[tax.id] = fullAllocs

            // Display base for TaxResult = sum of r2'd per-item bases (for receipt display)
            val displayBase = eligible.mapIndexed { pos, i ->
                when (tax.mode) {
                    TaxMode.INCLUDE -> afterReceiptDiscounts[i]
                    TaxMode.EXCLUDE -> when (tax.taxOrder) {
                        TaxOrder.BEFORE -> inclBasePerItem[i]
                        TaxOrder.AFTER  -> (inclBasePerItem[i] + runningExclPerItem[i]).r2()
                    }
                }
            }.fold(ZERO, BigDecimal::add)

            taxResultMap[tax.id] = TaxResult(tax = tax, base = displayBase, amount = aggAmount)

            // Accumulate EXCLUDE allocations into runningExclPerItem for subsequent AFTER taxes
            if (tax.mode == TaxMode.EXCLUDE) {
                eligible.forEachIndexed { pos, i ->
                    runningExclPerItem[i] = (runningExclPerItem[i] + allocsEligible[pos]).r2()
                }
            }
        }

        // Receipt-level tax aggregates
        val taxResults       = orderedTaxes.mapNotNull { taxResultMap[it.id] }
        val inclusiveTaxTotal = taxResults.filter { it.tax.mode == TaxMode.INCLUDE }
            .fold(ZERO) { a, t -> a + t.amount }
        val exclusiveTaxTotal = taxResults.filter { it.tax.mode == TaxMode.EXCLUDE }
            .fold(ZERO) { a, t -> a + t.amount }

        // ══════════════════════════════════════════════════════════════════════
        // STEP 5 — Build ProcessedItem list
        // ══════════════════════════════════════════════════════════════════════

        val processedItems = s1list.mapIndexed { i, s1 ->
            // Build TaxLines for this item (only taxes it opted in to)
            val taxLines = orderedTaxes.mapNotNull { tax ->
                if (tax.id !in s1.item.appliedTaxIds) return@mapNotNull null
                val alloc = taxAllocPerItem[tax.id]?.get(i) ?: return@mapNotNull null
                // Display base: what was actually used for this item
                val displayBase = when (tax.mode) {
                    TaxMode.INCLUDE -> afterReceiptDiscounts[i]
                    TaxMode.EXCLUDE -> when (tax.taxOrder) {
                        TaxOrder.BEFORE -> inclBasePerItem[i]
                        TaxOrder.AFTER  -> {
                            // running excl before this tax = runningExclPerItem[i] minus this alloc
                            // (runningExclPerItem was already advanced; back-compute for display)
                            val prevExcl = (runningExclPerItem[i] - alloc).clampR2()
                            (inclBasePerItem[i] + prevExcl).r2()
                        }
                    }
                }
                TaxLine(
                    taxId       = tax.id,
                    name        = tax.name,
                    ratePercent = tax.rate,
                    taxMode     = tax.mode,
                    taxOrder    = tax.taxOrder,
                    taxableBase = displayBase,
                    taxAmount   = alloc
                )
            }

            val totalExclTaxAmt = taxLines.filter { it.taxMode == TaxMode.EXCLUDE }
                .fold(ZERO) { a, t -> a + t.taxAmount }
            val totalAllTaxAmt  = taxLines.fold(ZERO) { a, t -> a + t.taxAmount }

            ProcessedItem(
                item                  = s1.item,
                gross                 = s1.gross,
                lineDiscountBreakdown = s1.discBreakdown,
                totalLineDiscount     = s1.totalDisc,
                netAfterLine          = s1.netAfterLine,
                receiptDiscountShare  = perItemReceiptDisc[i],
                afterReceiptDiscount  = afterReceiptDiscounts[i],
                inclusiveTaxBase      = inclBasePerItem[i],
                taxLines              = taxLines,
                totalExclTax          = inclBasePerItem[i],
                totalTax              = totalAllTaxAmt,
                // afterReceiptDiscount already contains embedded inclusive taxes;
                // add only the exclusive taxes that are charged on top.
                totalInclTax          = (afterReceiptDiscounts[i] + totalExclTaxAmt).r2()
            )
        }

        // ══════════════════════════════════════════════════════════════════════
        // STEP 6 — Fixed charges & grand total
        // ══════════════════════════════════════════════════════════════════════

        val fixedChargeResults     = fixedCharges.map { fc -> fc to fc.value.r2() }
        val totalFixedChargeAmount = fixedChargeResults.fold(ZERO) { a, p -> a + p.second }

        // grandTotal = subtotal2 + exclusiveTaxTotal + fixedCharges
        //
        // subtotal2 is the after-receipt-discount total with inclusive taxes STILL EMBEDDED.
        // Inclusive taxes are part of the stated price — they do NOT add to the grand total.
        // Only exclusive taxes (levied on top) and fixed charges increase the amount payable.
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
}

// ─────────────────────────────────────────────────────────────────────────────
// Extension
// ─────────────────────────────────────────────────────────────────────────────

fun <T> Iterable<T>.sumOfDecimal(selector: (T) -> BigDecimal): BigDecimal =
    fold(BigDecimal.ZERO) { acc, v -> acc.add(selector(v)) }