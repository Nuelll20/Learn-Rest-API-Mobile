package com.example.datakonsumen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class StatusKonsumen(
    val daftar: List<String> = emptyList(),
    val pesan: String = "",
    val sibuk: Boolean = false,
    val idTersimpan: Long? = null
)

class KonsumenViewModel : ViewModel() {
    private val _status = MutableStateFlow(StatusKonsumen())
    val status = _status.asStateFlow()
    val draf = mutableMapOf<String, String>()

    init {
        muatDaftar()
    }

    fun muatDaftar() {
        if (_status.value.sibuk) return
        viewModelScope.launch { ambilDaftar() }
    }

    fun simpan(data: JSONObject) {
        if (_status.value.sibuk) return
        _status.update { it.copy(sibuk = true, pesan = "Menyimpan konsumen…") }

        viewModelScope.launch {
            val id = try {
                withContext(Dispatchers.IO) { KonsumenApi.tambahKonsumen(data) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _status.update {
                    it.copy(sibuk = false, pesan = "Gagal menyimpan. ${error.message.orEmpty()}")
                }
                return@launch
            }

            draf.clear()
            _status.update { it.copy(idTersimpan = id) }
            ambilDaftar("Konsumen #$id tersimpan. ")
        }
    }

    private suspend fun ambilDaftar(awalan: String = "") {
        _status.update { it.copy(sibuk = true, pesan = awalan + "Memuat data…") }

        try {
            val pelanggan = withContext(Dispatchers.IO) {
                val data = KonsumenApi.ambilDaftar()
                List(data.length()) { indeks ->
                    val baris = data.getJSONObject(indeks)
                    "#${baris.getLong("id")} · ${baris.getString("nama")}\n" +
                            baris.getString("email")
                }
            }
            val ringkasan = if (pelanggan.isEmpty()) "Belum ada konsumen."
            else "${pelanggan.size} konsumen"

            _status.update { it.copy(daftar = pelanggan, pesan = awalan + ringkasan, sibuk = false) }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            _status.update {
                it.copy(
                    pesan = awalan + "Gagal memuat data. ${error.message.orEmpty()}",
                    sibuk = false
                )
            }
        }
    }
}