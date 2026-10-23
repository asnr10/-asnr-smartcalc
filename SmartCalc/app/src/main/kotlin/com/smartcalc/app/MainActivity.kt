package com.smartcalc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartcalc.engine.AngleMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val scheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            MaterialTheme(colorScheme = scheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Math expressions are always laid out left-to-right, even in a Persian (RTL) UI.
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        CalculatorScreen(viewModel())
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorScreen(vm: CalculatorViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End,
        ) {
            SelectionContainer {
                Text(
                    text = state.expression.ifEmpty { "0" },
                    fontSize = 28.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state.error != null) {
                Text(
                    text = state.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 18.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                SelectionContainer {
                    Text(
                        text = state.result,
                        fontSize = 44.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        val angleLabel = if (state.angleMode == AngleMode.DEGREES) "DEG" else "RAD"
        KeyRow(
            "sin" to { vm.append("sin(") }, "cos" to { vm.append("cos(") },
            "tan" to { vm.append("tan(") }, angleLabel to vm::toggleAngleMode,
        )
        KeyRow(
            "\u221A" to { vm.append("\u221A(") }, "^" to { vm.append("^") },
            "%" to { vm.append("%") }, "!" to { vm.append("!") },
        )
        KeyRow(
            "ln" to { vm.append("ln(") }, "log" to { vm.append("log(") },
            "\u03C0" to { vm.append("\u03C0") }, "Ans" to { vm.append("ans") },
        )
        KeyRow("C" to vm::clear, "\u232B" to vm::backspace, "(" to { vm.append("(") }, ")" to { vm.append(")") })
        KeyRow("7" to { vm.append("7") }, "8" to { vm.append("8") }, "9" to { vm.append("9") }, "\u00F7" to { vm.append("\u00F7") })
        KeyRow("4" to { vm.append("4") }, "5" to { vm.append("5") }, "6" to { vm.append("6") }, "\u00D7" to { vm.append("\u00D7") })
        KeyRow("1" to { vm.append("1") }, "2" to { vm.append("2") }, "3" to { vm.append("3") }, "\u2212" to { vm.append("\u2212") })
        KeyRow("0" to { vm.append("0") }, "." to { vm.append(".") }, "=" to vm::evaluate, "+" to { vm.append("+") })
    }
}

@Composable
private fun KeyRow(vararg keys: Pair<String, () -> Unit>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.forEach { (label, onClick) ->
            Button(onClick = onClick, modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 20.sp)
            }
        }
    }
}
