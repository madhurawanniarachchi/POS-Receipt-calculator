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

/**
 * A modifier is an add-on or variation attached to an Item.
 * Its total value (qty × price) is added to the item's base price
 * before any discounts or taxes are applied.
 */
data class Modifier(
    val id: String,
    val name: String,
    val qty: BigDecimal,
    val price: BigDecimal
)

/**
 * [modifiers]        — add-ons/variations; their totals are summed into the base price.
 * [appliedDiscounts] — ordered list of line discounts to apply sequentially.
 * [appliedTaxes]     — ordered list of taxes to apply (order matches catalogue order).
 */
data class Item(
    val id: String,
    val name: String,
    val qty: BigDecimal,
    val unitPrice: BigDecimal,
    val modifiers: List<Modifier> = emptyList(),
    val appliedDiscounts: List<Discount> = emptyList(),
    val appliedTaxes: List<Tax> = emptyList()
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
    val modifierTotal: BigDecimal,
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
    private val lineDiscounts: List<Discount>,   // catalogue — kept for receipt-discount reference
    private val taxes: List<Tax>,                // catalogue — defines processing order
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
        // STEP 1 — Base price (item + modifiers) & item-level discounts
        //
        // gross = (unitPrice × qty) + Σ(item.qty × modifier.qty × modifier.price)
        //
        // Discounts are drawn from item.appliedDiscounts (the embedded list,
        // not the global catalogue) so each item is fully self-contained.
        // Each discount is applied SEQUENTIALLY to the running value.
        // ══════════════════════════════════════════════════════════════════════

        data class S1(
            val item: Item,
            val modifierTotal: BigDecimal,
            val gross: BigDecimal,
            val discBreakdown: List<Pair<Discount, BigDecimal>>,
            val totalDisc: BigDecimal,
            val netAfterLine: BigDecimal
        )

        val s1list = items.map { item ->
            // Modifier total: Σ (item.qty × modifier.qty × modifier.price)
            val modTotal = item.modifiers.fold(ZERO) { acc, mod ->
                acc + (item.qty * mod.qty * mod.price).r2()
            }.r2()

            // Gross = item lines + modifier contributions
            val base = (item.unitPrice * item.qty).r2() + modTotal

            var running = base
            val breakdown = item.appliedDiscounts.map { disc ->
                // Sequential: each discount applies to the running value after previous discounts
                val raw = when (disc.type) {
                    DiscountType.PERCENT -> (running * disc.value / HUNDRED).r2()
                    DiscountType.FIXED   -> disc.value.r2()
                }
                val amt = raw.min(running).clampR2()
                running = (running - amt).clampR2()
                disc to amt
            }
            val totalDisc = breakdown.fold(ZERO) { a, p -> a + p.second }
            S1(item, modTotal, base, breakdown, totalDisc, running)
        }

        val grossTotal        = s1list.fold(ZERO) { a, s -> a + s.gross }
        val totalLineDiscount = s1list.fold(ZERO) { a, s -> a + s.totalDisc }
        val subtotal1         = s1list.fold(ZERO) { a, s -> a + s.netAfterLine }

        // ══════════════════════════════════════════════════════════════════════
        // STEP 2 — Receipt discounts applied sequentially after line discounts
        //          + round-robin distribution to items
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

        val perItemReceiptDisc: List<BigDecimal> = roundRobin(
            total   = totalReceiptDiscountAmount,
            weights = s1list.map { it.netAfterLine }
        )

        val afterReceiptDiscounts: List<BigDecimal> = s1list.mapIndexed { i, s1 ->
            (s1.netAfterLine - perItemReceiptDisc[i]).clampR2()
        }

        // ══════════════════════════════════════════════════════════════════════
        // STEP 3 — Inclusive-tax base (aggregate → round-robin)
        //
        // sumInclRates is now derived from item.appliedTaxes (the embedded list).
        // ══════════════════════════════════════════════════════════════════════

        val sumInclRatesPerItem: List<BigDecimal> = s1list.map { s1 ->
            s1.item.appliedTaxes
                .filter { it.mode == TaxMode.INCLUDE && it.rate > ZERO }
                .fold(ZERO) { a, t -> a + t.rate }
        }

        val exactInclBasePerItem: List<BigDecimal> = s1list.mapIndexed { i, _ ->
            val ard = afterReceiptDiscounts[i]
            val sIR = sumInclRatesPerItem[i]
            if (sIR > ZERO)
                ard.multiply(HUNDRED, MC).divide(HUNDRED.add(sIR, MC), MC)
            else
                ard.setScale(10, RoundingMode.HALF_UP)
        }

        val aggNetBase: BigDecimal = exactInclBasePerItem.fold(ZERO) { a, b -> a.add(b, MC) }.r2()

        val inclBasePerItem: List<BigDecimal> = roundRobin(
            total   = aggNetBase,
            weights = exactInclBasePerItem
        )

        // ══════════════════════════════════════════════════════════════════════
        // STEP 4 — Per-tax aggregation → single receipt total → RR to items
        //
        // Tax eligibility is now determined by item.appliedTaxes (the embedded
        // list). The global [taxes] catalogue defines processing ORDER only
        // (BEFORE first, then AFTER).
        //
        // For any tax id that appears in an item's appliedTaxes but NOT in the
        // global catalogue, it is processed in BEFORE order with its own
        // embedded mode/taxOrder values.
        // ══════════════════════════════════════════════════════════════════════

        // Build ordered tax list: first from catalogue (BEFORE then AFTER),
        // then any "orphan" taxes embedded in items that aren't in the catalogue.
        val catalogueTaxIds = taxes.map { it.id }.toSet()
        val orphanTaxes: List<Tax> = items
            .flatMap { it.appliedTaxes }
            .filter { it.id !in catalogueTaxIds }
            .distinctBy { it.id }

        val orderedTaxes: List<Tax> =
            taxes.filter { it.taxOrder == TaxOrder.BEFORE } +
                    taxes.filter { it.taxOrder == TaxOrder.AFTER } +
                    orphanTaxes.filter { it.taxOrder == TaxOrder.BEFORE } +
                    orphanTaxes.filter { it.taxOrder == TaxOrder.AFTER }

        val taxAllocPerItem: MutableMap<String, List<BigDecimal>> = mutableMapOf()
        val taxResultMap:    MutableMap<String, TaxResult>        = mutableMapOf()
        val runningExclPerItem: MutableList<BigDecimal>            = MutableList(s1list.size) { ZERO }

        orderedTaxes.forEach { tax ->
            if (tax.rate <= ZERO) return@forEach

            // An item opts in if its embedded appliedTaxes list contains this tax id
            val eligible = s1list.indices.filter { i ->
                s1list[i].item.appliedTaxes.any { it.id == tax.id }
            }
            if (eligible.isEmpty()) return@forEach

            val exactBasesEligible: List<BigDecimal> = eligible.map { i ->
                when (tax.mode) {
                    TaxMode.INCLUDE ->
                        afterReceiptDiscounts[i].setScale(10, RoundingMode.HALF_UP)
                    TaxMode.EXCLUDE -> when (tax.taxOrder) {
                        TaxOrder.BEFORE ->
                            exactInclBasePerItem[i]
                        TaxOrder.AFTER  ->
                            (inclBasePerItem[i] + runningExclPerItem[i])
                                .setScale(10, RoundingMode.HALF_UP)
                    }
                }
            }

            val aggBase = exactBasesEligible.fold(ZERO) { a, b -> a.add(b, MC) }
            val aggAmount: BigDecimal = when (tax.mode) {
                TaxMode.EXCLUDE -> (aggBase * tax.rate / HUNDRED).r2()
                TaxMode.INCLUDE -> {
                    val exactAmountsSum = eligible.mapIndexed { _, i ->
                        val sIR   = sumInclRatesPerItem[i]
                        val denom = HUNDRED.add(sIR, MC)
                        afterReceiptDiscounts[i].multiply(tax.rate, MC).divide(denom, MC)
                    }.fold(ZERO) { a, b -> a.add(b, MC) }
                    exactAmountsSum.r2()
                }
            }

            val allocsEligible: List<BigDecimal> = roundRobin(
                total   = aggAmount,
                weights = exactBasesEligible
            )

            val fullAllocs = MutableList(s1list.size) { ZERO }
            eligible.forEachIndexed { pos, i -> fullAllocs[i] = allocsEligible[pos] }
            taxAllocPerItem[tax.id] = fullAllocs

            val displayBase = eligible.mapIndexed { _, i ->
                when (tax.mode) {
                    TaxMode.INCLUDE -> afterReceiptDiscounts[i]
                    TaxMode.EXCLUDE -> when (tax.taxOrder) {
                        TaxOrder.BEFORE -> inclBasePerItem[i]
                        TaxOrder.AFTER  -> (inclBasePerItem[i] + runningExclPerItem[i]).r2()
                    }
                }
            }.fold(ZERO, BigDecimal::add)

            taxResultMap[tax.id] = TaxResult(tax = tax, base = displayBase, amount = aggAmount)

            if (tax.mode == TaxMode.EXCLUDE) {
                eligible.forEachIndexed { pos, i ->
                    runningExclPerItem[i] = (runningExclPerItem[i] + allocsEligible[pos]).r2()
                }
            }
        }

        val taxResults        = orderedTaxes.mapNotNull { taxResultMap[it.id] }
        val inclusiveTaxTotal = taxResults.filter { it.tax.mode == TaxMode.INCLUDE }
            .fold(ZERO) { a, t -> a + t.amount }
        val exclusiveTaxTotal = taxResults.filter { it.tax.mode == TaxMode.EXCLUDE }
            .fold(ZERO) { a, t -> a + t.amount }

        // ══════════════════════════════════════════════════════════════════════
        // STEP 5 — Build ProcessedItem list
        // ══════════════════════════════════════════════════════════════════════

        val processedItems = s1list.mapIndexed { i, s1 ->
            val taxLines = orderedTaxes.mapNotNull { tax ->
                if (s1.item.appliedTaxes.none { it.id == tax.id }) return@mapNotNull null
                val alloc = taxAllocPerItem[tax.id]?.get(i) ?: return@mapNotNull null
                val displayBase = when (tax.mode) {
                    TaxMode.INCLUDE -> afterReceiptDiscounts[i]
                    TaxMode.EXCLUDE -> when (tax.taxOrder) {
                        TaxOrder.BEFORE -> inclBasePerItem[i]
                        TaxOrder.AFTER  -> {
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
                modifierTotal         = s1.modifierTotal,
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
                totalInclTax          = (afterReceiptDiscounts[i] + totalExclTaxAmt).r2()
            )
        }

        // ══════════════════════════════════════════════════════════════════════
        // STEP 6 — Fixed charges & grand total
        // ══════════════════════════════════════════════════════════════════════

        val fixedChargeResults     = fixedCharges.map { fc -> fc to fc.value.r2() }
        val totalFixedChargeAmount = fixedChargeResults.fold(ZERO) { a, p -> a + p.second }

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