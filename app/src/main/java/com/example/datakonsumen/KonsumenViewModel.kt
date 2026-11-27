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

data class Konsumen(
    val id: Long,
    val nama: String,
    val email: String,
    val telepon: String,
    val alamat: String
)

data class StatusKonsumen(
    val daftar: List<Konsumen> = emptyList(),
    val pesan: String = "",
    val sibuk: Boolean = false,
    val dipilih: Konsumen? = null,
    val revisiFormulir: Long = 0
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

    fun pilih(konsumen: Konsumen) {
        if (_status.value.sibuk) return
        aturFormulir(konsumen)
        _status.update { it.copy(pesan = "Mengedit konsumen #${konsumen.id}.") }
    }

    fun batalEdit() {
        if (_status.value.sibuk) return
        aturFormulir()
        _status.update { it.copy(pesan = "Edit dibatalkan.") }
    }

    fun simpan(data: JSONObject) {
        val id = _status.value.dipilih?.id

        ubahData("Menyimpan konsumen…", "Gagal menyimpan.") {
            if (id == null) {
                val idBaru = KonsumenApi.tambahKonsumen(data)
                "Konsumen #$idBaru tersimpan."
            } else {
                KonsumenApi.ubahKonsumen(id, data)
                "Konsumen #$id diperbarui."
            }
        }
    }

    fun hapus(id: Long) {
        if (_status.value.dipilih?.id != id) return

        ubahData("Menghapus konsumen…", "Gagal menghapus.") {
            KonsumenApi.hapusKonsumen(id)
            "Konsumen #$id dihapus."
        }
    }

    private fun aturFormulir(konsumen: Konsumen? = null) {
        draf.clear()
        if (konsumen != null) {
            draf["nama"] = konsumen.nama
            draf["email"] = konsumen.email
            draf["telepon"] = konsumen.telepon
            draf["alamat"] = konsumen.alamat
        }

        _status.update {
            it.copy(dipilih = konsumen, revisiFormulir = it.revisiFormulir + 1)
        }
    }

    private fun ubahData(pesanMulai: String, pesanGagal: String, kerja: () -> String) {
        if (_status.value.sibuk) return
        _status.update { it.copy(sibuk = true, pesan = pesanMulai) }

        viewModelScope.launch {
            val pesanSukses = try {
                withContext(Dispatchers.IO) { kerja() }
            } catch (error: Exception) {
                if (error is CancellationException) throw error

                _status.update {
                    it.copy(sibuk = false, pesan = "$pesanGagal ${error.message.orEmpty()}")
                }
                return@launch
            }

            aturFormulir()
            ambilDaftar("$pesanSukses ")
        }
    }

    private suspend fun ambilDaftar(awalan: String = "") {
        _status.update { it.copy(sibuk = true, pesan = awalan + "Memuat data…") }

        try {
            val pelanggan = withContext(Dispatchers.IO) {
                val data = KonsumenApi.ambilDaftar()
                List(data.length()) { indeks ->
                    val baris = data.getJSONObject(indeks)
                    Konsumen(
                        id = baris.getLong("id"),
                        nama = baris.getString("nama"),
                        email = baris.getString("email"),
                        telepon = baris.getString("telepon"),
                        alamat = baris.getString("alamat")
                    )
                }
            }

            val ringkasan = if (pelanggan.isEmpty()) "Belum ada konsumen."
            else "${pelanggan.size} konsumen"

            _status.update {
                it.copy(daftar = pelanggan, pesan = awalan + ringkasan, sibuk = false)
            }
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