package com.example.calcvault.ui.calculator

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CalculatorEngine {

    private val format = DecimalFormat("#,##0.######", DecimalFormatSymbols(Locale.US))

    fun evaluate(expression: String): String {
        if (expression.isBlank()) return "0"
        try {
            val cleaned = expression.replace("×", "*")
                .replace("÷", "/")
                .replace("%", "*0.01")
                .trim()

            val parser = Parser(cleaned)
            val result = parser.parse()
            if (result.isInfinite() || result.isNaN()) {
                return "Error"
            }
            return if (result == result.toLong().toDouble() && !result.toString().contains("E")) {
                result.toLong().toString()
            } else {
                format.format(result)
            }
        } catch (e: Exception) {
            return "Error"
        }
    }

    private class Parser(private val str: String) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            while (ch == ' '.code) nextChar()
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> {
                        val divisor = parseFactor()
                        if (divisor == 0.0) return Double.NaN
                        x /= divisor
                    }
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return +parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                val numStr = str.substring(startPos, pos)
                x = numStr.toDoubleOrNull() ?: 0.0
            } else {
                return 0.0
            }

            return x
        }
    }
}
