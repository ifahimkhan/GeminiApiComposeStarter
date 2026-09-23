package com.fahim.geminiApiComposeStarter.core.tools

import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class MathResult(
    val expression: String,
    val result: String,
    val explanation: String,
    val isSuccess: Boolean = true,
)

class MathEngineTool {

    fun execute(input: String): MathResult {
        val trimmed = input.trim()
        val lower = trimmed.lowercase()

        return try {
            when {
                // Unit conversion
                lower.startsWith("convert ") -> handleUnitConversion(trimmed)

                // Statistics: e.g. "mean of [1, 2, 3]" or "standard deviation of 12, 15, 20"
                lower.contains("mean") || lower.contains("average") || lower.contains("standard deviation") || lower.contains("variance") || lower.contains("median") ->
                    handleStatistics(trimmed)

                // Percentage: e.g. "15% of 250"
                lower.contains("% of ") -> handlePercentage(trimmed)

                // Standard expression evaluation: e.g. "sqrt(144) + 25 * 3"
                else -> {
                    val cleanExpr = cleanExpression(trimmed)
                    val value = evaluateExpression(cleanExpr)
                    val formatted = formatNumber(value)
                    MathResult(
                        expression = cleanExpr,
                        result = formatted,
                        explanation = "Evaluated `$cleanExpr` = **$formatted**",
                    )
                }
            }
        } catch (e: Exception) {
            MathResult(
                expression = input,
                result = "Error",
                explanation = "Could not evaluate expression: ${e.message ?: "Invalid syntax"}",
                isSuccess = false,
            )
        }
    }

    private fun cleanExpression(raw: String): String {
        return raw.replace(Regex("(?i)calculate|compute|solve|="), "").trim()
    }

    private fun handlePercentage(input: String): MathResult {
        val pattern = Regex("""([\d\.]+)\s*%\s*of\s*([\d\.]+)""", RegexOption.IGNORE_CASE)
        val match = pattern.find(input) ?: throw IllegalArgumentException("Format must be 'X% of Y'")
        val percent = match.groupValues[1].toDouble()
        val total = match.groupValues[2].toDouble()
        val result = (percent / 100.0) * total
        val formatted = formatNumber(result)
        return MathResult(
            expression = input,
            result = formatted,
            explanation = "$percent% of $total = ($percent / 100) × $total = **$formatted**",
        )
    }

    private fun handleStatistics(input: String): MathResult {
        val numbers = Regex("""[-+]?[\d\.]+""").findAll(input)
            .mapNotNull { it.value.toDoubleOrNull() }
            .toList()

        if (numbers.isEmpty()) {
            throw IllegalArgumentException("No numbers found for statistical calculation")
        }

        val lower = input.lowercase()
        val count = numbers.size
        val sum = numbers.sum()
        val mean = sum / count

        val sorted = numbers.sorted()
        val median = if (count % 2 == 1) {
            sorted[count / 2]
        } else {
            (sorted[count / 2 - 1] + sorted[count / 2]) / 2.0
        }

        val variance = numbers.map { (it - mean).pow(2) }.sum() / count
        val stdDev = sqrt(variance)

        val resultStr: String
        val explanation: String

        when {
            lower.contains("standard deviation") -> {
                resultStr = formatNumber(stdDev)
                explanation = "Standard Deviation (σ) of [${numbers.joinToString(", ")}]:\n" +
                    "- Count (N): $count\n" +
                    "- Mean (μ): ${formatNumber(mean)}\n" +
                    "- Variance (σ²): ${formatNumber(variance)}\n" +
                    "- **Standard Deviation (σ): $resultStr**"
            }
            lower.contains("variance") -> {
                resultStr = formatNumber(variance)
                explanation = "Variance (σ²) of [${numbers.joinToString(", ")}]:\n" +
                    "- Mean (μ): ${formatNumber(mean)}\n" +
                    "- **Variance (σ²): $resultStr**"
            }
            lower.contains("median") -> {
                resultStr = formatNumber(median)
                explanation = "Median of [${numbers.joinToString(", ")}]: **$resultStr**"
            }
            else -> {
                resultStr = formatNumber(mean)
                explanation = "Mean / Average of [${numbers.joinToString(", ")}]:\n" +
                    "- Sum: ${formatNumber(sum)}\n" +
                    "- Count: $count\n" +
                    "- **Mean: $resultStr**"
            }
        }

        return MathResult(
            expression = input,
            result = resultStr,
            explanation = explanation,
        )
    }

    private fun handleUnitConversion(input: String): MathResult {
        val lower = input.lowercase().replace("convert ", "").trim()
        val parts = lower.split(" to ")
        if (parts.size != 2) {
            throw IllegalArgumentException("Format must be 'convert <value> <unit> to <target_unit>'")
        }

        val sourceStr = parts[0].trim()
        val targetUnit = parts[1].trim()

        val valueMatch = Regex("""^([\d\.\-]+)\s*(.+)$""").find(sourceStr)
            ?: throw IllegalArgumentException("Invalid conversion source value")

        val value = valueMatch.groupValues[1].toDouble()
        val fromUnit = valueMatch.groupValues[2].trim()

        val (converted, exp) = convertUnits(value, fromUnit, targetUnit)
        val formatted = formatNumber(converted)

        return MathResult(
            expression = input,
            result = "$formatted $targetUnit",
            explanation = "$value $fromUnit = **$formatted $targetUnit** ($exp)",
        )
    }

    private fun normalizeUnit(u: String): String = when (u.lowercase().trim()) {
        "celsius", "c" -> "celsius"
        "fahrenheit", "f" -> "fahrenheit"
        "kelvin", "k" -> "kelvin"
        "km", "kms", "kilometer", "kilometers" -> "km"
        "mile", "miles", "mi" -> "mile"
        "meter", "meters", "m" -> "meter"
        "foot", "feet", "ft" -> "foot"
        "inch", "inches", "in" -> "inch"
        "cm", "cms", "centimeter", "centimeters" -> "cm"
        "kg", "kgs", "kilogram", "kilograms" -> "kg"
        "pound", "pounds", "lb", "lbs" -> "pound"
        "gb", "gbs", "gigabyte", "gigabytes" -> "gb"
        "mb", "mbs", "megabyte", "megabytes" -> "mb"
        "tb", "tbs", "terabyte", "terabytes" -> "tb"
        "kb", "kbs", "kilobyte", "kilobytes" -> "kb"
        else -> u.lowercase().trim()
    }

    private fun convertUnits(value: Double, from: String, to: String): Pair<Double, String> {
        val f = normalizeUnit(from)
        val t = normalizeUnit(to)

        return when {
            // Temperature
            f == "celsius" && t == "fahrenheit" ->
                Pair((value * 9.0 / 5.0) + 32.0, "°F = (°C × 9/5) + 32")
            f == "fahrenheit" && t == "celsius" ->
                Pair((value - 32.0) * 5.0 / 9.0, "°C = (°F - 32) × 5/9")
            f == "celsius" && t == "kelvin" ->
                Pair(value + 273.15, "K = °C + 273.15")

            // Distance / Length
            f == "km" && t == "mile" ->
                Pair(value * 0.621371, "1 km ≈ 0.621371 miles")
            f == "mile" && t == "km" ->
                Pair(value * 1.60934, "1 mile ≈ 1.60934 km")
            f == "meter" && t == "foot" ->
                Pair(value * 3.28084, "1 m ≈ 3.28084 ft")
            f == "inch" && t == "cm" ->
                Pair(value * 2.54, "1 in = 2.54 cm")

            // Mass
            f == "kg" && t == "pound" ->
                Pair(value * 2.20462, "1 kg ≈ 2.20462 lbs")
            f == "pound" && t == "kg" ->
                Pair(value * 0.453592, "1 lb ≈ 0.453592 kg")

            // Digital Storage
            f == "gb" && t == "mb" -> Pair(value * 1024.0, "1 GB = 1024 MB")
            f == "mb" && t == "gb" -> Pair(value / 1024.0, "1 MB = 1/1024 GB")
            f == "tb" && t == "gb" -> Pair(value * 1024.0, "1 TB = 1024 GB")
            f == "mb" && t == "kb" -> Pair(value * 1024.0, "1 MB = 1024 KB")

            else -> throw IllegalArgumentException("Unsupported conversion from '$from' to '$to'")
        }
    }

    /**
     * Recursive descent expression evaluator supporting +, -, *, /, ^, %,
     * parentheses and functions (sqrt, sin, cos, tan, abs, log, ln).
     */
    private fun evaluateExpression(str: String): Double {
        return ExpressionParser(str).parse()
    }

    private class ExpressionParser(private val str: String) {
        private var pos = -1
        private var ch = ' '

        private fun nextChar() {
            ch = if (++pos < str.length) str[pos] else '\u0000'
        }

        private fun eat(charToEat: Char): Boolean {
            while (ch == ' ') nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val result = parseExpression()
            if (pos < str.length) throw IllegalArgumentException("Unexpected trailing tokens: '${str.substring(pos)}'")
            return result
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+') -> x += parseTerm()
                    eat('-') -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*') -> x *= parseFactor()
                    eat('/') -> {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        x /= divisor
                    }
                    eat('%') -> x %= parseFactor()
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+')) return +parseFactor()
            if (eat('-')) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('(')) {
                x = parseExpression()
                if (!eat(')')) throw IllegalArgumentException("Missing closing parenthesis")
            } else if ((ch in '0'..'9') || ch == '.') {
                while ((ch in '0'..'9') || ch == '.') nextChar()
                x = str.substring(startPos, pos).toDouble()
            } else if (ch in 'a'..'z' || ch in 'A'..'Z') {
                while (ch in 'a'..'z' || ch in 'A'..'Z') nextChar()
                val func = str.substring(startPos, pos).lowercase()
                if (!eat('(')) throw IllegalArgumentException("Expected '(' after function $func")
                val arg = parseExpression()
                if (!eat(')')) throw IllegalArgumentException("Missing closing parenthesis after $func")
                x = when (func) {
                    "sqrt" -> sqrt(arg)
                    "sin" -> sin(Math.toRadians(arg))
                    "cos" -> cos(Math.toRadians(arg))
                    "tan" -> tan(Math.toRadians(arg))
                    "abs" -> abs(arg)
                    "log" -> log10(arg)
                    "ln" -> ln(arg)
                    else -> throw IllegalArgumentException("Unknown function: $func")
                }
            } else {
                throw IllegalArgumentException("Unexpected character: '$ch'")
            }

            if (eat('^')) x = x.pow(parseFactor())

            return x
        }
    }

    private fun formatNumber(num: Double): String {
        return if (num == num.toLong().toDouble()) {
            num.toLong().toString()
        } else {
            String.format(Locale.US, "%.4f", num).trimEnd('0').trimEnd('.')
        }
    }
}
