package com.example.mislugares

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class WeatherData(
    val tempCelsius: Int,
    val condicion: String,
    val emoji: String
)

object WeatherService {

    suspend fun obtenerClima(latitud: Double, longitud: Double): WeatherData? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=$latitud" +
                        "&longitude=$longitud" +
                        "&current=temperature_2m,weathercode" +
                        "&timezone=auto"
                val response = URL(url).readText()
                val json = JSONObject(response)
                val current = json.getJSONObject("current")
                val temp = current.getDouble("temperature_2m").toInt()
                val code = current.getInt("weathercode")
                val (condicion, emoji) = weatherCodeToInfo(code)
                WeatherData(temp, condicion, emoji)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun weatherCodeToInfo(code: Int): Pair<String, String> {
        return when (code) {
            0 -> Pair("Despejado", "☀️")
            1 -> Pair("Mayormente despejado", "🌤️")
            2 -> Pair("Parcialmente nublado", "⛅")
            3 -> Pair("Nublado", "☁️")
            45, 48 -> Pair("Niebla", "🌫️")
            51, 53, 55 -> Pair("Llovizna", "🌦️")
            56, 57 -> Pair("Llovizna helada", "🌨️")
            61, 63, 65 -> Pair("Lluvia", "🌧️")
            66, 67 -> Pair("Lluvia helada", "🌨️")
            71, 73, 75 -> Pair("Nevada", "❄️")
            77 -> Pair("Granizo", "🌨️")
            80, 81, 82 -> Pair("Chubascos", "🌦️")
            85, 86 -> Pair("Chubascos de nieve", "❄️")
            95 -> Pair("Tormenta", "⛈️")
            96, 99 -> Pair("Tormenta con granizo", "⛈️")
            else -> Pair("Variable", "🌡️")
        }
    }
}
