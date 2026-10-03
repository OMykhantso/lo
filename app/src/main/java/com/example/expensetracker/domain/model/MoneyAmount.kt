package com.example.expensetracker.domain.model

/** Сума в конкретній валюті, у мінімальних одиницях. */
data class MoneyAmount(val minor: Long, val currency: Currency)
