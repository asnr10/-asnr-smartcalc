package com.smartcalc.engine

internal object Functions {
    data class Spec(val minArgs: Int, val maxArgs: Int)

    private val specs: Map<String, Spec> = mapOf(
        "sin" to Spec(1, 1), "cos" to Spec(1, 1), "tan" to Spec(1, 1),
        "asin" to Spec(1, 1), "acos" to Spec(1, 1), "atan" to Spec(1, 1),
        "sinh" to Spec(1, 1), "cosh" to Spec(1, 1), "tanh" to Spec(1, 1),
        "ln" to Spec(1, 1), "log" to Spec(1, 2), "exp" to Spec(1, 1),
        "sqrt" to Spec(1, 1), "cbrt" to Spec(1, 1), "abs" to Spec(1, 1),
        "floor" to Spec(1, 1), "ceil" to Spec(1, 1), "round" to Spec(1, 1),
        "min" to Spec(2, 2), "max" to Spec(2, 2),
    )

    private val aliases = mapOf(
        "arcsin" to "asin", "arccos" to "acos", "arctan" to "atan", "lg" to "log",
    )

    fun canonical(name: String): String = aliases[name] ?: name

    fun spec(canonicalName: String): Spec? = specs[canonicalName]
}
