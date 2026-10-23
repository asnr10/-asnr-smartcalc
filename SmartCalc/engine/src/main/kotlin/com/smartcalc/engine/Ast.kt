package com.smartcalc.engine

internal enum class BinOp { Add, Sub, Mul, Div, Pow }

internal enum class PostfixOp { Factorial, Percent }

internal sealed interface Expr {
    data class Const(val value: Double) : Expr
    data class Variable(val name: String) : Expr
    data class Negate(val operand: Expr) : Expr
    data class Binary(val op: BinOp, val left: Expr, val right: Expr) : Expr
    data class Postfix(val op: PostfixOp, val operand: Expr) : Expr
    data class Call(val name: String, val args: List<Expr>) : Expr
}
