package com.smartcalc.engine

import kotlin.math.PI
import kotlin.math.abs

enum class AngleMode { DEGREES, RADIANS }

/** Values available to an expression besides its literals. */
data class EvalContext(
    val x: Double? = null,
    val ans: Double? = null,
    val angleMode: AngleMode = AngleMode.DEGREES,
)

internal object Evaluator {

    private const val TINY = 1e-14

    fun eval(e: Expr, ctx: EvalContext): Double = finite(
        when (e) {
            is Expr.Const -> e.value
            is Expr.Variable -> variable(e.name, ctx)
            is Expr.Negate -> -eval(e.operand, ctx)
            is Expr.Binary -> binary(e, ctx)
            is Expr.Postfix -> postfix(e, ctx)
            is Expr.Call -> call(e, ctx)
        },
    )

    private fun finite(v: Double): Double = when {
        v.isNaN() -> throw CalcException(CalcError.Domain("Result is undefined"))
        v.isInfinite() -> throw CalcException(CalcError.Overflow)
        else -> v
    }

    private fun variable(name: String, ctx: EvalContext): Double = when (name) {
        "pi" -> PI
        "e" -> Math.E
        "x" -> ctx.x ?: throw CalcException(CalcError.UnknownSymbol("x"))
        "ans" -> ctx.ans ?: throw CalcException(CalcError.UnknownSymbol("ans"))
        else -> throw CalcException(CalcError.UnknownSymbol(name))
    }

    private fun binary(e: Expr.Binary, ctx: EvalContext): Double {
        val a = eval(e.left, ctx)
        val b = eval(e.right, ctx)
        return when (e.op) {
            BinOp.Add -> a + b
            BinOp.Sub -> a - b
            BinOp.Mul -> a * b
            BinOp.Div -> {
                if (b == 0.0) throw CalcException(CalcError.DivisionByZero)
                a / b
            }
            BinOp.Pow -> {
                if (a == 0.0 && b < 0.0) throw CalcException(CalcError.DivisionByZero)
                if (a < 0.0 && b != Math.floor(b)) {
                    throw CalcException(CalcError.Domain("Result is not a real number"))
                }
                Math.pow(a, b)
            }
        }
    }

    private fun postfix(e: Expr.Postfix, ctx: EvalContext): Double {
        val v = eval(e.operand, ctx)
        return when (e.op) {
            PostfixOp.Percent -> v / 100.0
            PostfixOp.Factorial -> {
                if (v < 0.0 || v != Math.floor(v)) {
                    throw CalcException(CalcError.Domain("Factorial needs a non-negative integer"))
                }
                if (v > 170.0) throw CalcException(CalcError.Overflow)
                var r = 1.0
                for (k in 2..v.toInt()) r *= k
                r
            }
        }
    }

    private fun toRad(v: Double, ctx: EvalContext) =
        if (ctx.angleMode == AngleMode.DEGREES) v * PI / 180.0 else v

    private fun fromRad(v: Double, ctx: EvalContext) =
        if (ctx.angleMode == AngleMode.DEGREES) v * 180.0 / PI else v

    /** Snaps floating-point noise such as sin(180°) = 1.2e-16 to exactly 0. */
    private fun snap(v: Double) = if (abs(v) < TINY) 0.0 else v

    private fun call(e: Expr.Call, ctx: EvalContext): Double {
        val a = e.args.map { eval(it, ctx) }
        val v = a[0]
        return when (e.name) {
            "sin" -> snap(Math.sin(toRad(v, ctx)))
            "cos" -> snap(Math.cos(toRad(v, ctx)))
            "tan" -> {
                val r = toRad(v, ctx)
                if (abs(Math.cos(r)) < TINY) {
                    throw CalcException(CalcError.Domain("tan is undefined at this angle"))
                }
                snap(Math.tan(r))
            }
            "asin" -> {
                if (v < -1.0 || v > 1.0) throw CalcException(CalcError.Domain("asin needs a value between -1 and 1"))
                fromRad(Math.asin(v), ctx)
            }
            "acos" -> {
                if (v < -1.0 || v > 1.0) throw CalcException(CalcError.Domain("acos needs a value between -1 and 1"))
                fromRad(Math.acos(v), ctx)
            }
            "atan" -> fromRad(Math.atan(v), ctx)
            "sinh" -> Math.sinh(v)
            "cosh" -> Math.cosh(v)
            "tanh" -> Math.tanh(v)
            "ln" -> {
                if (v <= 0.0) throw CalcException(CalcError.Domain("ln needs a positive value"))
                Math.log(v)
            }
            "log" -> {
                if (v <= 0.0) throw CalcException(CalcError.Domain("log needs a positive value"))
                if (a.size == 1) {
                    Math.log10(v)
                } else {
                    val base = a[1]
                    if (base <= 0.0 || base == 1.0) throw CalcException(CalcError.Domain("Invalid logarithm base"))
                    Math.log(v) / Math.log(base)
                }
            }
            "exp" -> Math.exp(v)
            "sqrt" -> {
                if (v < 0.0) throw CalcException(CalcError.Domain("Square root of a negative number"))
                Math.sqrt(v)
            }
            "cbrt" -> Math.cbrt(v)
            "abs" -> abs(v)
            "floor" -> Math.floor(v)
            "ceil" -> Math.ceil(v)
            "round" -> Math.floor(v + 0.5)
            "min" -> minOf(v, a[1])
            "max" -> maxOf(v, a[1])
            else -> throw CalcException(CalcError.UnknownSymbol(e.name))
        }
    }
}
