package com.example.expensetracker.data

import com.example.expensetracker.data.remote.NbuApi
import com.example.expensetracker.data.remote.NbuApiFactory
import com.example.expensetracker.data.remote.NbuRateDto
import com.example.expensetracker.data.remote.NbuRateMapper
import com.example.expensetracker.data.remote.NbuRatesRemoteDataSource
import com.example.expensetracker.data.remote.RatesFormatException
import com.example.expensetracker.data.remote.RatesServerException
import com.example.expensetracker.domain.model.Currency
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NbuRateMapperTest {
    private fun dto(cc: String? = "USD", rate: Double? = 41.5, date: String? = "03.10.2025") =
        NbuRateDto(r030 = 840, txt = "x", rate = rate, cc = cc, exchangedate = date)

    @Test
    fun `keeps only supported foreign currencies`() {
        val result = NbuRateMapper.map(listOf(dto("USD"), dto("EUR", 48.0), dto("PLN", 10.0), dto("UAH", 1.0), dto("JPY", 0.3)))
        assertEquals(listOf(Currency.USD, Currency.EUR), result.map { it.currency })
    }

    @Test
    fun `parses rate and date`() {
        val rate = NbuRateMapper.map(listOf(dto(rate = 41.1783))).single()
        assertEquals(BigDecimal("41.1783"), rate.rate)
        assertEquals(LocalDate.of(2025, 10, 3), rate.date)
    }

    @Test
    fun `drops zero, negative, missing and non finite rates`() {
        val result = NbuRateMapper.map(
            listOf(
                dto("USD", 0.0), dto("USD", -1.0), dto("USD", null),
                dto("USD", Double.NaN), dto("USD", Double.POSITIVE_INFINITY),
                dto("EUR", 48.0),
            ),
        )
        assertEquals(listOf(Currency.EUR), result.map { it.currency })
    }

    @Test
    fun `drops absurd rates`() {
        assertTrue(NbuRateMapper.map(listOf(dto("USD", 1.0e9), dto("EUR", 1.0e-9))).isEmpty())
    }

    @Test
    fun `drops entries with a bad or missing date`() {
        val result = NbuRateMapper.map(listOf(dto("USD", date = "2025-10-03"), dto("EUR", date = null), dto("USD", date = "31.02.2025")))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `currency code is case insensitive and trimmed`() {
        assertEquals(Currency.USD, NbuRateMapper.map(listOf(dto(" usd "))).single().currency)
    }

    @Test
    fun `duplicate currency keeps the most recent date`() {
        val result = NbuRateMapper.map(listOf(dto("USD", 40.0, "01.10.2025"), dto("USD", 41.0, "03.10.2025"), dto("USD", 39.0, "02.10.2025")))
        assertEquals(BigDecimal("41.0"), result.single().rate)
    }

    @Test
    fun `empty input`() {
        assertTrue(NbuRateMapper.map(emptyList()).isEmpty())
    }
}

/** Реальний HTTP: Retrofit + kotlinx.serialization проти MockWebServer, що імітує API НБУ. */
class NbuApiTest {
    private lateinit var server: MockWebServer
    private lateinit var source: NbuRatesRemoteDataSource

    private val sample = """
        [
          {"r030":36,"txt":"Австралійський долар","rate":27.5,"cc":"AUD","exchangedate":"03.10.2025"},
          {"r030":840,"txt":"Долар США","rate":41.1783,"cc":"USD","exchangedate":"03.10.2025"},
          {"r030":978,"txt":"Євро","rate":48.0123,"cc":"EUR","exchangedate":"03.10.2025","unknown_field":true}
        ]
    """.trimIndent()

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        source = NbuRatesRemoteDataSource(NbuApiFactory.create(server.url("/").toString()))
    }

    @After
    fun tearDown() = server.shutdown()

    private fun respond(body: String, code: Int = 200) =
        server.enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json; charset=utf-8").setBody(body))

    @Test
    fun `requests the NBU endpoint and maps USD and EUR`() = runBlocking {
        respond(sample)

        val rates = source.fetchRates()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/NBUStatService/v1/statdirectory/exchange?json", request.path)
        assertEquals(listOf(Currency.USD, Currency.EUR), rates.map { it.currency })
        assertEquals(BigDecimal("41.1783"), rates.first().rate)
    }

    @Test
    fun `unknown json fields are ignored`() = runBlocking {
        respond(sample)
        assertEquals(2, source.fetchRates().size)
    }

    @Test
    fun `http error becomes a server exception with the code`() {
        respond("oops", code = 503)
        val e = assertThrows(RatesServerException::class.java) { runBlocking { source.fetchRates() } }
        assertEquals(503, e.httpCode)
    }

    @Test
    fun `malformed json becomes a format exception`() {
        respond("<html>not json</html>")
        assertThrows(RatesFormatException::class.java) { runBlocking { source.fetchRates() } }
    }

    @Test
    fun `json of the wrong shape becomes a format exception`() {
        respond("""{"error":"maintenance"}""")
        assertThrows(RatesFormatException::class.java) { runBlocking { source.fetchRates() } }
    }

    @Test
    fun `empty array is a valid but empty answer`() = runBlocking {
        respond("[]")
        assertTrue(source.fetchRates().isEmpty())
    }

    @Test
    fun `dropped connection surfaces as IOException (offline)`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertThrows(IOException::class.java) { runBlocking { source.fetchRates() } }
    }

    @Test
    fun `unreachable server surfaces as IOException`() {
        val unreachable = NbuRatesRemoteDataSource(NbuApiFactory.create("http://localhost:1/"))
        assertThrows(IOException::class.java) { runBlocking { unreachable.fetchRates() } }
    }

    @Test
    fun `default base url is https`() {
        assertTrue(NbuApi.BASE_URL.startsWith("https://"))
    }
}
