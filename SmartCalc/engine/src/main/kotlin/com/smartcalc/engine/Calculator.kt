package com.smartcalc.engine

sealed interface CalcResult {
    data class Success(val value: Double) : CalcResult
    data class Failure(val error: CalcError) : CalcResult
}

/** Public entry point of the engine. Pure Kotlin/JVM, no Android dependencies. */
object Calculator {

    const val MAX_INPUT_LENGTH = 1000

    /** Never throws: every failure is returned as [CalcResult.Failure]. */
    fun evaluate(input: String, ctx: EvalContext = EvalContext()): CalcResult {
        if (input.isBlank()) return CalcResult.Failure(CalcError.Syntax("Empty expression"))
        if (input.length > MAX_INPUT_LENGTH) {
            return CalcResult.Failure(CalcError.Syntax("Expression is too long"))
        }
        return try {
            val ast = Parser(Tokenizer.tokenize(input)).parse()
            CalcResult.Success(Evaluator.eval(ast, ctx))
        } catch (e: CalcException) {
            CalcResult.Failure(e.error)
        } catch (e: StackOverflowError) {
            CalcResult.Failure(CalcError.Syntax("Expression is nested too deeply"))
        }
    }
}
