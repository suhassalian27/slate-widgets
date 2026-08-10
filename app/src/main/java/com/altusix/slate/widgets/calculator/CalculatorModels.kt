package com.altusix.slate.widgets.calculator

import android.content.Context

data class CalculatorState(
    val expression: String = "",
    val resultText: String = "0",
    val isEvaluated: Boolean = false
)

object CalculatorEngine {

    fun getWidgetState(context: Context, appWidgetId: Int): CalculatorState {
        val prefs = context.getSharedPreferences("slate_calc_prefs", Context.MODE_PRIVATE)
        val expr = prefs.getString("calc_${appWidgetId}_expr", "") ?: ""
        val res = prefs.getString("calc_${appWidgetId}_res", "0") ?: "0"
        val isEval = prefs.getBoolean("calc_${appWidgetId}_eval", false)
        return CalculatorState(expr, res, isEval)
    }

    fun saveWidgetState(context: Context, appWidgetId: Int, state: CalculatorState) {
        val prefs = context.getSharedPreferences("slate_calc_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("calc_${appWidgetId}_expr", state.expression)
            .putString("calc_${appWidgetId}_res", state.resultText)
            .putBoolean("calc_${appWidgetId}_eval", state.isEvaluated)
            .apply()
    }

    fun handleKeyPress(currentState: CalculatorState, key: String): CalculatorState {
        var expr = currentState.expression
        var isEval = currentState.isEvaluated

        return when (key) {
            "AC" -> CalculatorState("", "0", false)
            "DEL" -> {
                if (isEval) {
                    CalculatorState("", "0", false)
                } else if (expr.isNotEmpty()) {
                    val updatedExpr = expr.substring(0, expr.length - 1)
                    val newRes = evaluateExpression(updatedExpr)
                    CalculatorState(updatedExpr, if (updatedExpr.isEmpty()) "0" else newRes, false)
                } else {
                    currentState
                }
            }
            "=" -> {
                if (expr.isNotEmpty()) {
                    val finalResult = evaluateExpression(expr)
                    CalculatorState(expr, finalResult, true)
                } else currentState
            }
            else -> {
                if (isEval) {
                    expr = if (isOperator(key)) currentState.resultText + key else key
                    isEval = false
                } else {
                    expr += key
                }
                val newRes = evaluateExpression(expr)
                CalculatorState(expr, newRes, false)
            }
        }
    }

    private fun isOperator(key: String): Boolean = key in listOf("+", "-", "×", "÷", "%")

    private fun evaluateExpression(expr: String): String {
        return try {
            if (expr.isEmpty()) return "0"
            val cleanExpr = expr.replace("×", "*").replace("÷", "/")

            // Basic sequential evaluator
            val result = simpleEvaluate(cleanExpr)
            if (result % 1.0 == 0.0) {
                result.toLong().toString()
            } else {
                String.format("%.4f", result).trimEnd('0').trimEnd('.')
            }
        } catch (e: Exception) {
            "Error"
        }
    }

    private fun simpleEvaluate(expression: String): Double {
        var expr = expression
        if (expr.endsWith("+") || expr.endsWith("-") || expr.endsWith("*") || expr.endsWith("/")) {
            expr = expr.substring(0, expr.length - 1)
        }
        if (expr.isEmpty()) return 0.0

        val tokens = mutableListOf<String>()
        var numberBuffer = ""
        for (ch in expr) {
            if (ch in listOf('+', '-', '*', '/')) {
                if (numberBuffer.isNotEmpty()) {
                    tokens.add(numberBuffer)
                    numberBuffer = ""
                }
                tokens.add(ch.toString())
            } else {
                numberBuffer += ch
            }
        }
        if (numberBuffer.isNotEmpty()) tokens.add(numberBuffer)

        if (tokens.isEmpty()) return 0.0

        var currentVal = tokens[0].toDoubleOrNull() ?: 0.0
        var i = 1
        while (i < tokens.size - 1) {
            val op = tokens[i]
            val nextVal = tokens[i + 1].toDoubleOrNull() ?: 0.0
            when (op) {
                "+" -> currentVal += nextVal
                "-" -> currentVal -= nextVal
                "*" -> currentVal *= nextVal
                "/" -> if (nextVal != 0.0) currentVal /= nextVal
            }
            i += 2
        }
        return currentVal
    }
}