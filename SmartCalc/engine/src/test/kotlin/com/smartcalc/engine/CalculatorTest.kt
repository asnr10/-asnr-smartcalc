package com.smartcalc.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CalculatorTest {

    private fun ok(expr: String, expected: Double, ctx: EvalContext = EvalContext(), eps: Double = 1e-9) {
        val r = Calculator.evaluate(expr, ctx)
        assertIs<CalcResult.Success>(r, "'$expr' failed: $r")
        assertTrue(abs(r.value - expected) <= eps, "'$expr' = ${r.value}, expected $expected")
    }

    private inline fun <reified T : CalcError> fails(expr: String, ctx: EvalContext = EvalContext()) {
        val r = Calculator.evaluate(expr, ctx)
        assertIs<CalcResult.Failure>(r, "'$expr' should fail but gave $r")
        assertIs<T>(r.error, "'$expr' failed with the wrong error: ${r.error}")
    }

    // ---- basics and precedence ----
    @Test fun basicOperations() {
        ok("2+3", 5.0); ok("7-10", -3.0); ok("6*7", 42.0); ok("9/4", 2.25)
    }

    @Test fun precedence() {
        ok("2+3*4", 14.0); ok("(2+3)*4", 20.0); ok("100/4/5", 5.0); ok("8-3-2", 3.0)
    }

    @Test fun powerIsRightAssociative() = ok("2^3^2", 512.0)

    @Test fun unaryMinusBindsLooserThanPower() {
        ok("-2^2", -4.0); ok("2^-2", 0.25); ok("2*-3", -6.0); ok("--4", 4.0)
    }

    @Test fun decimals() { ok("0.5+.25", 0.75); ok("5.", 5.0) }

    @Test fun unicodeOperators() = ok("2\u00D73\u00F76\u22121", 0.0)

    @Test fun persianDigits() {
        ok("\u06F2+\u06F3\u00D7\u06F4", 14.0)
        ok("\u06F1\u066B\u06F5\u00D72", 3.0)
    }

    // ---- percent, factorial, implicit multiplication ----
    @Test fun percent() { ok("50%", 0.5); ok("200*10%", 20.0); ok("5+10%", 5.1) }

    @Test fun factorial() { ok("5!", 120.0); ok("0!", 1.0); ok("170!", 7.257415615307994E306, eps = 1e292) }

    @Test fun implicitMultiplication() {
        ok("2(3+4)", 14.0); ok("(1+2)(3+4)", 21.0); ok("2pi", 2 * PI); ok("2\u03C0", 2 * PI)
        ok("3x", 12.0, EvalContext(x = 4.0)); ok("2x^2", 18.0, EvalContext(x = 3.0))
    }

    // ---- functions and constants ----
    @Test fun scientificFunctionsDegrees() {
        ok("sin(30)", 0.5); ok("cos(60)", 0.5); ok("tan(45)", 1.0)
        ok("sin(180)", 0.0, eps = 0.0); ok("cos(90)", 0.0, eps = 0.0)
        ok("asin(1)", 90.0); ok("acos(0)", 90.0); ok("atan(1)", 45.0)
    }

    @Test fun scientificFunctionsRadians() {
        val rad = EvalContext(angleMode = AngleMode.RADIANS)
        ok("sin(pi/2)", 1.0, rad); ok("cos(0)", 1.0, rad); ok("asin(1)", PI / 2, rad)
    }

    @Test fun logsRootsAndMore() {
        ok("sqrt(16)", 4.0); ok("\u221A9", 3.0); ok("cbrt(27)", 3.0)
        ok("ln(e)", 1.0); ok("log(1000)", 3.0); ok("log(8,2)", 3.0); ok("exp(0)", 1.0)
        ok("abs(-3)", 3.0); ok("floor(2.7)", 2.0); ok("ceil(2.1)", 3.0); ok("round(2.5)", 3.0)
        ok("max(2,5)", 5.0); ok("min(2,5)", 2.0); ok("arcsin(1)", 90.0)
    }

    @Test fun variablesAndAns() {
        ok("x^2", 9.0, EvalContext(x = 3.0))
        ok("ans*2", 10.0, EvalContext(ans = 5.0))
    }

    // ---- errors ----
    @Test fun divisionByZero() {
        fails<CalcError.DivisionByZero>("1/0"); fails<CalcError.DivisionByZero>("5/(2-2)")
        fails<CalcError.DivisionByZero>("0^-1")
    }

    @Test fun domainErrors() {
        fails<CalcError.Domain>("sqrt(-1)"); fails<CalcError.Domain>("ln(0)"); fails<CalcError.Domain>("log(-5)")
        fails<CalcError.Domain>("tan(90)"); fails<CalcError.Domain>("asin(2)")
        fails<CalcError.Domain>("(-8)^0.5"); fails<CalcError.Domain>("2.5!"); fails<CalcError.Domain>("(-1)!")
    }

    @Test fun overflow() { fails<CalcError.Overflow>("10^400"); fails<CalcError.Overflow>("171!") }

    @Test fun syntaxErrors() {
        fails<CalcError.Syntax>(""); fails<CalcError.Syntax>("   "); fails<CalcError.Syntax>("2+")
        fails<CalcError.Syntax>("2+*3"); fails<CalcError.Syntax>("(1+2"); fails<CalcError.Syntax>("1+2)")
        fails<CalcError.Syntax>("()"); fails<CalcError.Syntax>("1..2"); fails<CalcError.Syntax>("2 3")
        fails<CalcError.Syntax>("sin"); fails<CalcError.Syntax>("sin()"); fails<CalcError.Syntax>("max(1)")
        fails<CalcError.Syntax>("2 \$ 3")
    }

    @Test fun unknownSymbols() {
        fails<CalcError.UnknownSymbol>("foo(3)"); fails<CalcError.UnknownSymbol>("x")
        fails<CalcError.UnknownSymbol>("ans")
    }

    @Test fun hostileInputsDoNotCrash() {
        fails<CalcError.Syntax>("(".repeat(500) + "1" + ")".repeat(500))
        fails<CalcError.Syntax>("-".repeat(900) + "1")
        fails<CalcError.Syntax>("1+".repeat(600) + "1")
    }

    // ---- formatting ----
    @Test fun formatting() {
        assertEquals("14", ResultFormatter.format(14.0))
        assertEquals("0.3", ResultFormatter.format(0.1 + 0.2))
        assertEquals("0", ResultFormatter.format(-0.0))
        assertEquals("0.333333333333", ResultFormatter.format(1.0 / 3))
        assertEquals("0.666666666667", ResultFormatter.format(2.0 / 3))
        assertEquals("1234567.891", ResultFormatter.format(1234567.891))
        assertEquals("-100", ResultFormatter.format(-100.0))
        assertEquals("1E15", ResultFormatter.format(1e15))
        assertEquals("1.5E-10", ResultFormatter.format(1.5e-10))
    }
}
