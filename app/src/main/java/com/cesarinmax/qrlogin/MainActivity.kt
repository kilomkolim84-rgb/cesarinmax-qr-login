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

class MainActivity : AppCompatActivity() {

    private val MIKROTIK_IP = "172.16.1.1"
    private val MIKROTIK_USER = "tu_usuario"
    private val MIKROTIK_PASS = "tu_contraseña"

    private lateinit var tvCodigo: TextView
    private lateinit var btnConectar: Button
    private lateinit var tvEstado: TextView
    private var codigoEscaneado: String? = null
    private var esValido = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvCodigo = findViewById(R.id.tvCodigo)
        btnConectar = findViewById(R.id.btnConectar)
        tvEstado = findViewById(R.id.tvEstado)

        btnConectar.isEnabled = false
        btnConectar.alpha = 0.5f

        if (!estaEnRedCorrecta()) {
            tvEstado.text = "⚠️ Conéctate a la red CESARINMAX"
        } else {
            pedirPermisoCamara()
        }

        btnConectar.setOnClickListener {
            if (esValido && codigoEscaneado != null) {
                Toast.makeText(this, "Conectando: $codigoEscaneado", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Escanea un código primero", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun estaEnRedCorrecta(): Boolean {
        return try {
            val wifiMgr = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            val ip = wifiMgr.connectionInfo.ipAddress
            val ipStr = String.format(
                "%d.%d.%d.%d",
                ip and 0xFF, ip shr 8 and 0xFF, ip shr 16 and 0xFF, ip shr 24 and 0xFF
            )
            ipStr.startsWith("172.16.1.")
        } catch (e: Exception) {
            true
        }
    }

    private fun pedirPermisoCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), 100
            )
        } else {
            escanearQR()
        }
    }

    private fun escanearQR() {
        val opciones = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt("📸 Escanea el código del ticket")
            .setCameraId(0)
            .setBeepEnabled(true)
        escanearLauncher.launch(opciones)
    }

    private val escanearLauncher = registerForActivityResult(ScanContract()) { resultado ->
        if (resultado.contents != null) {
            codigoEscaneado = resultado.contents.trim()
            tvCodigo.text = "Código: $codigoEscaneado"
            tvEstado.text = "✅ Código leído"
            esValido = true
            btnConectar.isEnabled = true
            btnConectar.alpha = 1.0f
        } else {
            tvEstado.text = "❌ Escaneo cancelado"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            escanearQR()
        } else {
            tvEstado.text = "⚠️ Se necesita permiso de cámara"
        }
    }
}
