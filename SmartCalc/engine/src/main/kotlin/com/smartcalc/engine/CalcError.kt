package com.smartcalc.engine

/** Every way a calculation can fail. The UI maps each type to a localized message. */
sealed interface CalcError {
    val message: String

    data object DivisionByZero : CalcError {
        override val message = "Division by zero"
    }

    data class Syntax(override val message: String, val position: Int? = null) : CalcError

    data class Domain(override val message: String) : CalcError

    data class UnknownSymbol(val name: String) : CalcError {
        override val message = "Unknown symbol: $name"
    }

    data object Overflow : CalcError {
        override val message = "Result is too large"
    }
}

/** Internal control-flow exception; never escapes [Calculator]. */
internal class CalcException(val error: CalcError) : RuntimeException(error.message)

internal fun syntax(message: String, position: Int? = null) =
    CalcException(CalcError.Syntax(message, position))
