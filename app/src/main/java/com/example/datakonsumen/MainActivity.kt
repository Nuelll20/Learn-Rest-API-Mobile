package com.example.datakonsumen

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private val model: KonsumenViewModel by viewModels()
    private val formulir = linkedMapOf<String, EditText>()
    private lateinit var pesan: TextView
    private lateinit var daftar: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var tombolSimpan: Button
    private lateinit var tombolBatal: Button
    private lateinit var tombolHapus: Button
    private lateinit var tombolMuatUlang: Button
    private var revisiFormulirTampil = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gayaBar = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK)
        enableEdgeToEdge(statusBarStyle = gayaBar, navigationBarStyle = gayaBar)

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            isFocusableInTouchMode = true
        }

        scroll = ScrollView(this).apply {
            addView(panel)
            setOnApplyWindowInsetsListener { _, insets ->
                val tepi = insets.getInsets(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout() or
                            WindowInsets.Type.ime()
                )
                val margin = dp(16)
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

        revisiFormulirTampil = model.status.value.revisiFormulir
        buatFormulir(panel)

        tombolSimpan = Button(this).apply {
            text = "Tambah konsumen"
            isAllCaps = false
            setOnClickListener { simpan() }
        }
        panel.addView(tombolSimpan)

        tombolBatal = Button(this).apply {
            text = "Batal edit"
            isAllCaps = false
            visibility = View.GONE
            setOnClickListener { model.batalEdit() }
        }
        panel.addView(tombolBatal)

        tombolHapus = Button(this).apply {
            text = "Hapus konsumen"
            isAllCaps = false
            visibility = View.GONE
            setOnClickListener { konfirmasiHapus() }
        }
        panel.addView(tombolHapus)

        tombolMuatUlang = Button(this).apply {
            text = "Muat ulang daftar"
            isAllCaps = false
            setOnClickListener { model.muatDaftar() }
        }
        panel.addView(tombolMuatUlang)

        pesan = TextView(this).apply { textSize = 15f }
        panel.addView(pesan)

        daftar = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        panel.addView(daftar)
        panel.requestFocus()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.status.collect { tampilkan(it) }
            }
        }
    }

    private fun dp(nilai: Int): Int = (nilai * resources.displayMetrics.density).toInt()

    private fun buatFormulir(panel: LinearLayout) {
        for (kolom in listOf("nama", "email", "telepon", "alamat")) {
            val edit = EditText(this).apply {
                hint = kolom.replaceFirstChar { it.uppercase() }
                setSingleLine()
                inputType = when (kolom) {
                    "email" -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    "telepon" -> InputType.TYPE_CLASS_PHONE
                    else -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                }
                setText(model.draf[kolom].orEmpty())
                doAfterTextChanged { model.draf[kolom] = it?.toString().orEmpty() }
            }

            formulir[kolom] = edit
            panel.addView(edit)
        }
    }

    private fun simpan() {
        val data = JSONObject()

        for ((kolom, edit) in formulir) {
            val nilai = edit.text.toString().trim()
            if (nilai.isEmpty()) {
                edit.error = "Wajib diisi"
                edit.requestFocus()
                return
            }
            data.put(kolom, nilai)
        }

        currentFocus?.let { fokus ->
            getSystemService(InputMethodManager::class.java)
                .hideSoftInputFromWindow(fokus.windowToken, 0)
        }
        model.simpan(data)
    }

    private fun konfirmasiHapus() {
        val konsumen = model.status.value.dipilih ?: return

        AlertDialog.Builder(this)
            .setTitle("Hapus konsumen?")
            .setMessage("#${konsumen.id} · ${konsumen.nama}")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ -> model.hapus(konsumen.id) }
            .show()
    }

    private fun tampilkan(status: StatusKonsumen) {
        val sedangEdit = status.dipilih != null
        tombolSimpan.text = if (sedangEdit) "Simpan perubahan" else "Tambah konsumen"
        tombolSimpan.isEnabled = !status.sibuk
        tombolMuatUlang.isEnabled = !status.sibuk
        tombolBatal.visibility = if (sedangEdit) View.VISIBLE else View.GONE
        tombolHapus.visibility = tombolBatal.visibility
        tombolBatal.isEnabled = !status.sibuk
        tombolHapus.isEnabled = !status.sibuk
        formulir.values.forEach { it.isEnabled = !status.sibuk }
        pesan.text = status.pesan

        if (revisiFormulirTampil != status.revisiFormulir) {
            for ((kolom, edit) in formulir) {
                edit.error = null
                edit.setText(model.draf[kolom].orEmpty())
            }
            revisiFormulirTampil = status.revisiFormulir
        }

        daftar.removeAllViews()
        for (konsumen in status.daftar) {
            daftar.addView(Button(this).apply {
                text = "#${konsumen.id} · ${konsumen.nama}\n${konsumen.email}"
                isAllCaps = false
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                isEnabled = !status.sibuk
                setOnClickListener {
                    model.pilih(konsumen)
                    scroll.smoothScrollTo(0, 0)
                }
            })
        }
    }
}