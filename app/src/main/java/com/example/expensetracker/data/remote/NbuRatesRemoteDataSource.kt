package com.example.expensetracker.data.remote

import kotlinx.serialization.SerializationException
import retrofit2.HttpException

class NbuRatesRemoteDataSource(private val api: NbuApi) : RatesRemoteDataSource {
    override suspend fun fetchRates(): List<RemoteRate> {
        val dtos = try {
            api.getRates()
        } catch (e: HttpException) {
            throw RatesServerException(e.code())
        } catch (e: SerializationException) {
            throw RatesFormatException(e)
        }
        // IOException (немає мережі, таймаут) навмисно проходить без змін — це «офлайн», а не помилка даних.
        return NbuRateMapper.map(dtos)
    }
}
