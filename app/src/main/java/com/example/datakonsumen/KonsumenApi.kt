package com.example.datakonsumen

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object KonsumenApi {
    private const val ENDPOINT = "http://10.0.2.2/api_konsumen/konsumen.php"

    fun ambilDaftar(): JSONArray {
        val koneksi = URL(ENDPOINT).openConnection() as HttpURLConnection

        try {
            koneksi.requestMethod = "GET"
            koneksi.connectTimeout = 8_000
            koneksi.readTimeout = 8_000
            koneksi.setRequestProperty("Accept", "application/json")

            val kode = koneksi.responseCode
            val berhasil = kode in 200..299
            val stream = if (berhasil) koneksi.inputStream else koneksi.errorStream
            val teks = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (!berhasil) {
                val pesan = runCatching { JSONObject(teks).getString("pesan") }
                    .getOrDefault("Periksa alamat dan server API.")
                throw IOException("HTTP $kode. $pesan")
            }

            return JSONObject(teks).getJSONArray("data")
        } finally {
            koneksi.disconnect()
        }
    }
}