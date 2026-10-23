package com.smartcalc.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs

object ResultFormatter {

    /**
     * Rounds to [significantDigits] to hide binary floating-point noise
     * (0.1 + 0.2 shows as 0.3), drops trailing zeros, and switches to
     * scientific notation for very large or very small magnitudes.
     */
    fun format(value: Double, significantDigits: Int = 12): String {
        if (value == 0.0) return "0"
        if (value.isNaN() || value.isInfinite()) return "NaN"
        val rounded = BigDecimal(value)
            .round(MathContext(significantDigits, RoundingMode.HALF_EVEN))
            .stripTrailingZeros()
        val magnitude = abs(value)
        return if (magnitude >= 1e15 || magnitude < 1e-9) {
            rounded.toString().replace("E+", "E")
        } else {
            rounded.toPlainString()
        }
    }
}
