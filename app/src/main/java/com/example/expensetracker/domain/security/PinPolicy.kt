package com.example.expensetracker.domain.security

enum class PinError { WRONG_LENGTH, NOT_DIGITS, TRIVIAL }

/** Вимоги до PIN: рівно 4 цифри, без очевидних комбінацій (`0000`, `1234`, `4321`…). */
object PinPolicy {
    const val LENGTH = 4

    fun validate(pin: String): PinError? {
        if (pin.length != LENGTH) return PinError.WRONG_LENGTH
        if (!pin.all { it in '0'..'9' }) return PinError.NOT_DIGITS
        if (isTrivial(pin)) return PinError.TRIVIAL
        return null
    }

    private fun isTrivial(pin: String): Boolean {
        val digits = pin.map { it - '0' }
        val steps = digits.zipWithNext { a, b -> b - a }
        val allSame = steps.all { it == 0 }
        val ascending = steps.all { it == 1 }
        val descending = steps.all { it == -1 }
        return allSame || ascending || descending
    }
}
