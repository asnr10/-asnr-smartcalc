package com.smartcalc.engine

/**
 * Recursive-descent parser.
 *
 * expr    := term (('+' | '-') term)*
 * term    := unary (('*' | '/' | implicit) unary)*
 * unary   := ('-' | '+') unary | power
 * power   := postfix ('^' unary)?          (right-associative, so -2^2 = -4)
 * postfix := primary ('!' | '%')*
 * primary := number | name | name '(' args ')' | '(' expr ')' | '√' postfix
 */
internal class Parser(private val tokens: List<Token>) {
    private var i = 0
    private var depth = 0

    private val peek: Token get() = tokens[i]

    fun parse(): Expr {
        val e = parseExpr()
        val t = peek
        if (t !is Token.End) throw syntax("Unexpected ${describe(t)}", t.pos)
        return e
    }

    private fun enter() {
        if (++depth > MAX_DEPTH) throw syntax("Expression is nested too deeply")
    }

    private fun leave() {
        depth--
    }

    private fun isSym(t: Token, ch: Char) = t is Token.Sym && t.ch == ch

    private fun describe(t: Token): String = when (t) {
        is Token.Num -> "number"
        is Token.Ident -> "'${t.name}'"
        is Token.Sym -> "'${t.ch}'"
        is Token.End -> "end of expression"
    }

    private fun parseExpr(): Expr {
        enter()
        var left = parseTerm()
        while (true) {
            val t = peek
            if (isSym(t, '+')) {
                i++
                left = Expr.Binary(BinOp.Add, left, parseTerm())
            } else if (isSym(t, '-')) {
                i++
                left = Expr.Binary(BinOp.Sub, left, parseTerm())
            } else break
        }
        leave()
        return left
    }

    private fun startsOperand(t: Token) =
        t is Token.Num || t is Token.Ident || isSym(t, '(') || isSym(t, '\u221A')

    private fun parseTerm(): Expr {
        var left = parseUnary()
        while (true) {
            val t = peek
            when {
                isSym(t, '*') -> {
                    i++
                    left = Expr.Binary(BinOp.Mul, left, parseUnary())
                }
                isSym(t, '/') -> {
                    i++
                    left = Expr.Binary(BinOp.Div, left, parseUnary())
                }
                startsOperand(t) -> {
                    // Implicit multiplication: 2(3), 2x, 2pi, (1+2)(3+4).
                    // Two bare numbers ("2 3") are rejected: that is almost always a typo.
                    if (t is Token.Num && tokens[i - 1] is Token.Num) {
                        throw syntax("Missing operator between numbers", t.pos)
                    }
                    left = Expr.Binary(BinOp.Mul, left, parseUnary())
                }
                else -> break
            }
        }
        return left
    }

    private fun parseUnary(): Expr {
        enter()
        val t = peek
        val result = when {
            isSym(t, '-') -> {
                i++
                Expr.Negate(parseUnary())
            }
            isSym(t, '+') -> {
                i++
                parseUnary()
            }
            else -> parsePower()
        }
        leave()
        return result
    }

    private fun parsePower(): Expr {
        val base = parsePostfix()
        if (isSym(peek, '^')) {
            i++
            return Expr.Binary(BinOp.Pow, base, parseUnary())
        }
        return base
    }

    private fun parsePostfix(): Expr {
        var e = parsePrimary()
        while (true) {
            val t = peek
            e = when {
                isSym(t, '!') -> { i++; Expr.Postfix(PostfixOp.Factorial, e) }
                isSym(t, '%') -> { i++; Expr.Postfix(PostfixOp.Percent, e) }
                else -> return e
            }
        }
    }

    private fun parsePrimary(): Expr {
        val t = peek
        return when {
            t is Token.Num -> {
                i++
                Expr.Const(t.value)
            }
            t is Token.Ident -> {
                i++
                val name = Functions.canonical(t.name)
                val spec = Functions.spec(name)
                if (spec == null) {
                    Expr.Variable(name)
                } else {
                    if (!isSym(peek, '(')) throw syntax("Function '${t.name}' needs parentheses", t.pos)
                    i++
                    val args = parseArgs()
                    if (args.size < spec.minArgs || args.size > spec.maxArgs) {
                        throw syntax("Wrong number of arguments for '${t.name}'", t.pos)
                    }
                    Expr.Call(name, args)
                }
            }
            isSym(t, '(') -> {
                i++
                val inner = parseExpr()
                expectClose()
                inner
            }
            isSym(t, '\u221A') -> {
                i++
                Expr.Call("sqrt", listOf(parsePostfix()))
            }
            else -> throw syntax("Unexpected ${describe(t)}", t.pos)
        }
    }

    private fun parseArgs(): List<Expr> {
        val args = ArrayList<Expr>()
        if (isSym(peek, ')')) {
            i++
            return args
        }
        while (true) {
            args += parseExpr()
            val t = peek
            when {
                isSym(t, ',') -> i++
                isSym(t, ')') -> {
                    i++
                    return args
                }
                t is Token.End -> throw syntax("Missing closing parenthesis", t.pos)
                else -> throw syntax("Unexpected ${describe(t)}", t.pos)
            }
        }
    }

    private fun expectClose() {
        val t = peek
        if (isSym(t, ')')) {
            i++
        } else if (t is Token.End) {
            throw syntax("Missing closing parenthesis", t.pos)
        } else {
            throw syntax("Unexpected ${describe(t)}", t.pos)
        }
    }

    private companion object {
        const val MAX_DEPTH = 400
    }
}
