package com.example.expensetracker.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET

/**
 * Офіційний API НБУ: `GET /NBUStatService/v1/statdirectory/exchange?json` — курси всіх валют до гривні.
 * Приклад елемента: `{"r030":840,"txt":"Долар США","rate":41.1783,"cc":"USD","exchangedate":"03.10.2025"}`.
 */
interface NbuApi {
    @GET("NBUStatService/v1/statdirectory/exchange?json")
    suspend fun getRates(): List<NbuRateDto>

    companion object {
        const val BASE_URL = "https://bank.gov.ua/"
    }
}

/** Усі поля опційні: відсутнє або порожнє поле відкидає лише цей елемент, а не всю відповідь. */
@Serializable
data class NbuRateDto(
    val r030: Int? = null,
    val txt: String? = null,
    val rate: Double? = null,
    val cc: String? = null,
    val exchangedate: String? = null,
)
