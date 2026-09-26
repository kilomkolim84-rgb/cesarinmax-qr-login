package com.cesarinmax.qrlogin

import android.Manifest
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var tvEstado: TextView
    private lateinit var btnEscanear: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvEstado = findViewById(R.id.tvEstado)
        btnEscanear = findViewById(R.id.btnEscanear)

        tvEstado.text = "✅ App abierta"
        btnEscanear.setOnClickListener { pedirCamara() }
    }

    private fun pedirCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            escanear()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        }
    }

    private fun escanear() {
        val opt = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt("📸 Escanea QR")
            .setBeepEnabled(true)
        escaner.launch(opt)
    }

    private val escaner = registerForActivityResult(ScanContract()) { res ->
        if (res.contents != null) {
            tvEstado.text = "✅ Código: ${res.contents}"
        } else {
            tvEstado.text = "❌ Cancelado"
        }
    }

    override fun onRequestPermissionsResult(
        c: Int, p: Array<out String>, g: IntArray
    ) {
        super.onRequestPermissionsResult(c, p, g)
        if (c == 100 && g.firstOrNull() == PackageManager.PERMISSION_GRANTED) escanear()
    }
}
