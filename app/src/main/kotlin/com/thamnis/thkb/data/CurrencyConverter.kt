package com.thamnis.thkb.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String,
    val unitsPerUsd: Double,
)

class CurrencyConverter(context: Context? = null) {
    private val prefs: SharedPreferences? =
        context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _ratesUsdBase = MutableStateFlow(loadRates())
    val ratesUsdBase: StateFlow<Map<String, Double>> = _ratesUsdBase.asStateFlow()

    private val _lastUpdatedMs = MutableStateFlow(prefs?.getLong(KEY_UPDATED_MS, 0L) ?: 0L)
    val lastUpdatedMs: StateFlow<Long> = _lastUpdatedMs.asStateFlow()

    fun convert(amount: Double, fromCode: String, toCode: String): Double {
        if (amount.isNaN() || amount.isInfinite()) return 0.0
        val table = _ratesUsdBase.value
        val fromRate = table[fromCode] ?: return 0.0
        val toRate = table[toCode] ?: return 0.0
        if (fromRate <= 0.0) return 0.0
        return (amount / fromRate) * toRate
    }

    fun formatResult(value: Double, currencyCode: String): String {
        val decimals = if (currencyCode in setOf("XOF", "XAF", "JPY")) 0 else 2
        val pattern = if (decimals == 0) "#,##0" else "#,##0.##"
        val fmt = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
        return fmt.format(value)
    }

    suspend fun refreshLiveRates(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL("https://open.er-api.com/v6/latest/USD").openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 8000
                requestMethod = "GET"
            }
            if (conn.responseCode !in 200..299) {
                error("HTTP ${conn.responseCode}")
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val ratesObj = json.getJSONObject("rates")
            val updated = DEFAULT_CURRENCIES.associate { info ->
                info.code to ratesObj.optDouble(info.code, info.unitsPerUsd)
            }
            // Enforce exact EUR <-> XOF CFA fixed parity when both EUR and XOF exist
            val eur = updated["EUR"] ?: 0.92
            val synced = updated.toMutableMap().apply {
                this["XOF"] = eur * 655.957
                this["XAF"] = eur * 655.957
            }
            val now = System.currentTimeMillis()
            saveRates(synced, now)
            synced.size
        }
    }

    private fun saveRates(rates: Map<String, Double>, timestampMs: Long) {
        _ratesUsdBase.value = rates
        _lastUpdatedMs.value = timestampMs
        val obj = JSONObject()
        rates.forEach { (k, v) -> obj.put(k, v) }
        prefs?.edit()
            ?.putString(KEY_RATES_JSON, obj.toString())
            ?.putLong(KEY_UPDATED_MS, timestampMs)
            ?.apply()
    }

    private fun loadRates(): Map<String, Double> {
        val base = DEFAULT_CURRENCIES.associate { it.code to it.unitsPerUsd }.toMutableMap()
        val raw = prefs?.getString(KEY_RATES_JSON, null) ?: return base
        runCatching {
            val obj = JSONObject(raw)
            obj.keys().forEach { code ->
                val v = obj.optDouble(code, -1.0)
                if (v > 0.0) base[code] = v
            }
        }
        return base
    }

    companion object {
        private const val PREFS_NAME = "th_kb_currency_prefs"
        private const val KEY_RATES_JSON = "rates_usd_json"
        private const val KEY_UPDATED_MS = "rates_updated_ms"

        val DEFAULT_CURRENCIES = listOf(
            CurrencyInfo("XOF", "West African CFA", "CFA", 603.48),
            CurrencyInfo("EUR", "Euro", "€", 0.92),
            CurrencyInfo("USD", "US Dollar", "$", 1.0),
            CurrencyInfo("GBP", "British Pound", "£", 0.79),
            CurrencyInfo("CAD", "Canadian Dollar", "CA$", 1.40),
            CurrencyInfo("CHF", "Swiss Franc", "CHF", 0.88),
            CurrencyInfo("XAF", "Central African CFA", "FCFA", 603.48),
            CurrencyInfo("NGN", "Nigerian Naira", "₦", 1650.0),
            CurrencyInfo("GHS", "Ghanaian Cedi", "₵", 15.8),
            CurrencyInfo("MAD", "Moroccan Dirham", "MAD", 9.9),
            CurrencyInfo("JPY", "Japanese Yen", "¥", 154.0),
            CurrencyInfo("CNY", "Chinese Yuan", "¥", 7.24),
        )
    }
}
