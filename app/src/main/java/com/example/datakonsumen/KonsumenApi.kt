package com.example.datakonsumen

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object KonsumenApi {
    private const val ENDPOINT = "http://10.0.2.2/api_konsumen/konsumen.php"

    fun ambilDaftar(): JSONArray = request("GET").getJSONArray("data")

    fun tambahKonsumen(data: JSONObject): Long = request("POST", body = data).getLong("id")

    fun ubahKonsumen(id: Long, data: JSONObject) {
        request("PUT", id, data)
    }

    fun hapusKonsumen(id: Long) {
        request("DELETE", id)
    }

    private fun request(
        metode: String,
        id: Long? = null,
        body: JSONObject? = null
    ): JSONObject {
        val alamat = if (id == null) ENDPOINT else "$ENDPOINT?id=$id"
        val koneksi = URL(alamat).openConnection() as HttpURLConnection

        try {
            koneksi.requestMethod = metode
            koneksi.connectTimeout = 8_000
            koneksi.readTimeout = 8_000
            koneksi.setRequestProperty("Accept", "application/json")

            if (body != null) {
                val payload = body.toString().toByteArray(Charsets.UTF_8)
                koneksi.doOutput = true
                koneksi.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                koneksi.setFixedLengthStreamingMode(payload.size)
                koneksi.outputStream.use { it.write(payload) }
            }

            val kode = koneksi.responseCode
            val berhasil = kode in 200..299
            val stream = if (berhasil) koneksi.inputStream else koneksi.errorStream
            val teks = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (!berhasil) {
                val pesan = runCatching { JSONObject(teks).getString("pesan") }
                    .getOrDefault("Periksa alamat dan server API.")
                throw IOException("HTTP $kode. $pesan")
            }

            return JSONObject(teks)
        } finally {
            koneksi.disconnect()
        }
    }
}