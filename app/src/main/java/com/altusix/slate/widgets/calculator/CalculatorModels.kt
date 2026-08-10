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
                } else {
                    currentState
                }
            }

            else -> {
                if (isOperator(key)) {
                    if (isEval) {
                        expr = currentState.resultText + key
                        isEval = false
                    } else if (expr.isEmpty()) {
                        // Prepend "0" if starting with an operator (+, ×, ÷, %) or "-" for negative numbers
                        expr = if (key == "-") "-" else "0$key"
                    } else if (isOperator(expr.last().toString())) {
                        // REPLACEMENT LOGIC: Replace the existing trailing operator with the newly pressed operator
                        expr = expr.substring(0, expr.length - 1) + key
                    } else {
                        expr += key
                    }
                } else if (key == ".") {
                    if (isEval) {
                        expr = "0."
                        isEval = false
                    } else {
                        // Prevent multiple decimals in the current active number
                        val lastOpIndex = expr.indexOfLast { isOperator(it.toString()) }
                        val currentNumSegment = if (lastOpIndex != -1) expr.substring(lastOpIndex + 1) else expr

                        if (!currentNumSegment.contains(".")) {
                            expr = if (currentNumSegment.isEmpty()) "${expr}0." else "$expr."
                        }
                    }
                } else { // Digit keys 0-9
                    if (isEval) {
                        expr = key
                        isEval = false
                    } else {
                        // Replace isolated leading "0" in the active number (e.g. prevent "05")
                        val lastOpIndex = expr.indexOfLast { isOperator(it.toString()) }
                        val currentNumSegment = if (lastOpIndex != -1) expr.substring(lastOpIndex + 1) else expr

                        expr = if (currentNumSegment == "0") {
                            expr.substring(0, expr.length - 1) + key
                        } else {
                            expr + key
                        }
                    }
                }

                val newRes = evaluateExpression(expr)
                CalculatorState(expr, if (expr.isEmpty()) "0" else newRes, isEval)
            }
        }
    }

    private fun isOperator(key: String): Boolean = key in listOf("+", "-", "×", "÷", "%", "*", "/")

    private fun evaluateExpression(rawExpr: String): String {
        return try {
            if (rawExpr.isEmpty()) return "0"
            var cleanExpr = rawExpr.replace("×", "*").replace("÷", "/")

            // Trim trailing uncommitted operators before evaluating
            while (cleanExpr.isNotEmpty() && isOperator(cleanExpr.last().toString())) {
                cleanExpr = cleanExpr.substring(0, cleanExpr.length - 1)
            }
            if (cleanExpr.isEmpty()) return "0"

            val result = parseAndEvaluate(cleanExpr)
            if (result.isNaN() || result.isInfinite()) return "Error"

            if (result % 1.0 == 0.0) {
                result.toLong().toString()
            } else {
                String.format("%.4f", result).trimEnd('0').trimEnd('.')
            }
        } catch (e: Exception) {
            "Error"
        }
    }

    private fun parseAndEvaluate(expression: String): Double {
        val tokens = mutableListOf<String>()
        var numberBuffer = ""

        for (i in expression.indices) {
            val ch = expression[i]
            if (ch in listOf('+', '-', '*', '/', '%')) {
                // Handle negative numbers at start or following an operator
                if (ch == '-' && (numberBuffer.isEmpty() && (tokens.isEmpty() || isOperator(tokens.last())))) {
                    numberBuffer += ch
                } else {
                    if (numberBuffer.isNotEmpty()) {
                        tokens.add(numberBuffer)
                        numberBuffer = ""
                    }
                    tokens.add(ch.toString())
                }
            } else {
                numberBuffer += ch
            }
        }
        if (numberBuffer.isNotEmpty()) tokens.add(numberBuffer)

        if (tokens.isEmpty()) return 0.0

        // Handle percentage operations first
        val processedTokens = mutableListOf<String>()
        var idx = 0
        while (idx < tokens.size) {
            val token = tokens[idx]
            if (token == "%") {
                if (processedTokens.isNotEmpty()) {
                    val prevVal = processedTokens.removeAt(processedTokens.size - 1).toDoubleOrNull() ?: 0.0
                    processedTokens.add((prevVal / 100.0).toString())
                }
            } else {
                processedTokens.add(token)
            }
            idx++
        }

        if (processedTokens.isEmpty()) return 0.0

        var currentVal = processedTokens[0].toDoubleOrNull() ?: 0.0
        var i = 1
        while (i < processedTokens.size - 1) {
            val op = processedTokens[i]
            val nextVal = processedTokens[i + 1].toDoubleOrNull() ?: 0.0
            when (op) {
                "+" -> currentVal += nextVal
                "-" -> currentVal -= nextVal
                "*" -> currentVal *= nextVal
                "/" -> if (nextVal != 0.0) currentVal /= nextVal else return Double.NaN
            }
            i += 2
        }
        return currentVal
    }
}