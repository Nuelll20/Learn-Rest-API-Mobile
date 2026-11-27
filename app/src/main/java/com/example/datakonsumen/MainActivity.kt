package com.example.datakonsumen

import android.graphics.Color
import android.os.Bundle
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    private lateinit var status: TextView
    private lateinit var daftar: LinearLayout
    private lateinit var tombolMuatUlang: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gayaBar = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK)
        enableEdgeToEdge(statusBarStyle = gayaBar, navigationBarStyle = gayaBar)

        val margin = (16 * resources.displayMetrics.density).toInt()
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val scroll = ScrollView(this).apply {
            addView(panel)
            setOnApplyWindowInsetsListener { _, insets ->
                val tepi = insets.getInsets(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
                )
                panel.setPadding(
                    margin + tepi.left, margin + tepi.top,
                    margin + tepi.right, margin + tepi.bottom
                )
                insets
            }
        }
        setContentView(scroll)

        panel.addView(TextView(this).apply {
            text = "Data Konsumen"
            textSize = 26f
            isAccessibilityHeading = true
        })
        tombolMuatUlang = Button(this).apply {
            text = "Muat ulang daftar"
            isAllCaps = false
            setOnClickListener { muatDaftar() }
        }
        panel.addView(tombolMuatUlang)

        status = TextView(this).apply { textSize = 15f }
        panel.addView(status)
        daftar = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        panel.addView(daftar)
        muatDaftar()
    }

    private fun muatDaftar() {
        tombolMuatUlang.isEnabled = false
        status.text = "Memuat data…"

        thread {
            val hasil = runCatching {
                val data = KonsumenApi.ambilDaftar()
                List(data.length()) { indeks ->
                    val baris = data.getJSONObject(indeks)
                    "#${baris.getLong("id")} · ${baris.getString("nama")}\n" +
                            baris.getString("email")
                }
            }

            runOnUiThread {
                if (isDestroyed || isFinishing) return@runOnUiThread
                tombolMuatUlang.isEnabled = true

                hasil.onSuccess { pelanggan ->
                    daftar.removeAllViews()
                    status.text = if (pelanggan.isEmpty()) "Belum ada konsumen."
                    else "${pelanggan.size} konsumen"

                    pelanggan.forEach { ringkasan ->
                        daftar.addView(TextView(this).apply {
                            text = ringkasan
                            textSize = 16f
                            val spasi = (8 * resources.displayMetrics.density).toInt()
                            setPadding(0, spasi, 0, spasi)
                        })
                    }
                }.onFailure { error ->
                    status.text = "Gagal memuat data. ${error.message.orEmpty()}"
                }
            }
        }
    }
}