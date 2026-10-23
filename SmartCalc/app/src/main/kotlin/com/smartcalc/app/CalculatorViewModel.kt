package com.smartcalc.app

import androidx.lifecycle.ViewModel
import com.smartcalc.engine.AngleMode
import com.smartcalc.engine.CalcResult
import com.smartcalc.engine.Calculator
import com.smartcalc.engine.EvalContext
import com.smartcalc.engine.ResultFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CalcUiState(
    val expression: String = "",
    val result: String = "",
    val error: String? = null,
    val angleMode: AngleMode = AngleMode.DEGREES,
)

class CalculatorViewModel : ViewModel() {
    private val _state = MutableStateFlow(CalcUiState())
    val state: StateFlow<CalcUiState> = _state.asStateFlow()

    private var ans: Double? = null

    fun append(text: String) = _state.update {
        it.copy(expression = (it.expression + text).take(Calculator.MAX_INPUT_LENGTH), error = null)
    }

    fun backspace() = _state.update { it.copy(expression = it.expression.dropLast(1), error = null) }

    fun clear() = _state.update { it.copy(expression = "", result = "", error = null) }

    fun toggleAngleMode() = _state.update {
        it.copy(
            angleMode = if (it.angleMode == AngleMode.DEGREES) AngleMode.RADIANS else AngleMode.DEGREES,
        )
    }

    fun evaluate() {
        val s = _state.value
        when (val r = Calculator.evaluate(s.expression, EvalContext(ans = ans, angleMode = s.angleMode))) {
            is CalcResult.Success -> {
                ans = r.value
                _state.update { it.copy(result = ResultFormatter.format(r.value), error = null) }
            }
            is CalcResult.Failure -> _state.update { it.copy(result = "", error = r.error.message) }
        }
    }
}
