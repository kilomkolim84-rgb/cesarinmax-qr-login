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
    private var codigoEscaneado: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvEstado = findViewById(R.id.tvEstado)
        btnEscanear = findViewById(R.id.btnEscanear)

        if (!estaEnRedCorrecta()) {
            tvEstado.text = "⚠️ Conéctate a la red CESARINMAX"
        } else {
            tvEstado.text = "✅ Conectado — Listo para escanear"
        }

        btnEscanear.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                escanearQR()
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.CAMERA), 1001
                )
            }
        }
    }

    private fun estaEnRedCorrecta(): Boolean {
        return try {
            val wifi = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            val ip = wifi.connectionInfo.ipAddress
            if (ip == 0) return false
            val ipStr = String.format(
                Locale.getDefault(),
                "%d.%d.%d.%d",
                ip and 0xFF, ip shr 8 and 0xFF, ip shr 16 and 0xFF, ip shr 24 and 0xFF
            )
            ipStr.startsWith("172.16.1.")
        } catch (e: Exception) {
            false
        }
    }

    private fun escanearQR() {
        val opciones = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt("📸 Escanea el código")
            .setBeepEnabled(true)
        escanearLauncher.launch(opciones)
    }

    private val escanearLauncher = registerForActivityResult(ScanContract()) { resultado ->
        if (resultado.contents != null) {
            codigoEscaneado = resultado.contents
            tvEstado.text = "✅ Código: $codigoEscaneado"
            Toast.makeText(this, "Leído: $codigoEscaneado", Toast.LENGTH_LONG).show()
        } else {
            tvEstado.text = "❌ Escaneo cancelado"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            escanearQR()
        } else {
            tvEstado.text = "❌ Se necesita permiso de cámara"
        }
    }
}
