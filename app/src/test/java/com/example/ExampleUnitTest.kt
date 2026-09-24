package com.example

import com.example.calcvault.ui.calculator.CalculatorEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun calculatorEngine_basicOperations() {
        assertEquals("4", CalculatorEngine.evaluate("2 + 2"))
        assertEquals("6", CalculatorEngine.evaluate("10 - 4"))
        assertEquals("24", CalculatorEngine.evaluate("6 * 4"))
        assertEquals("5", CalculatorEngine.evaluate("20 / 4"))
    }

    @Test
    fun calculatorEngine_orderOfOperations() {
        assertEquals("14", CalculatorEngine.evaluate("2 + 3 * 4"))
        assertEquals("20", CalculatorEngine.evaluate("(2 + 3) * 4"))
    }

    @Test
    fun calculatorEngine_divideByZero() {
        assertEquals("Error", CalculatorEngine.evaluate("10 / 0"))
    }
}
