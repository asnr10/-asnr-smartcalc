package com.smartcalc.engine

internal sealed interface Token {
    val pos: Int

    data class Num(val value: Double, override val pos: Int) : Token
    data class Ident(val name: String, override val pos: Int) : Token
    data class Sym(val ch: Char, override val pos: Int) : Token
    data class End(override val pos: Int) : Token
}

internal object Tokenizer {

    private const val SYMBOLS = "+-*/^()!%,\u221A"

    /** One-to-one character mapping so error positions stay valid. */
    private fun normalizeChar(c: Char): Char = when (c) {
        in '\u06F0'..'\u06F9' -> '0' + (c - '\u06F0') // Persian digits
        in '\u0660'..'\u0669' -> '0' + (c - '\u0660') // Arabic-Indic digits
        '\u066B' -> '.'                                // Arabic decimal separator
        '\u060C' -> ','                                // Arabic comma
        '\u00D7' -> '*'                                // ×
        '\u00F7' -> '/'                                // ÷
        '\u2212' -> '-'                                // −
        else -> c
    }

    fun tokenize(raw: String): List<Token> {
        val s = String(CharArray(raw.length) { normalizeChar(raw[it]) })
        val out = ArrayList<Token>()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c.isWhitespace() -> i++
                c in '0'..'9' || c == '.' -> {
                    val start = i
                    var dots = 0
                    while (i < s.length && (s[i] in '0'..'9' || s[i] == '.')) {
                        if (s[i] == '.') dots++
                        i++
                    }
                    val text = s.substring(start, i)
                    if (dots > 1 || text == ".") throw syntax("Invalid number '$text'", start)
                    out += Token.Num(text.toDouble(), start)
                }
                c == '\u03C0' -> { // π
                    out += Token.Ident("pi", i)
                    i++
                }
                c.isLetter() -> {
                    val start = i
                    while (i < s.length && s[i].isLetter() && s[i] != '\u03C0') i++
                    out += Token.Ident(s.substring(start, i).lowercase(), start)
                }
                c in SYMBOLS -> {
                    out += Token.Sym(c, i)
                    i++
                }
                else -> throw syntax("Unexpected character '$c'", i)
            }
        }
        out += Token.End(s.length)
        return out
    }
}
