package com.cesarinmax.qrlogin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : AppCompatActivity() {

    private lateinit var etCodigo: EditText
    
    // ✅ TU IP Y TU DOMINIO — SIN RESTRICCIÓN DE RED
    private val IP_MIKROTIK = "172.16.1.1"
    private val DOMINIO = "cesarinmax.paoyhan2027.net"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etCodigo = findViewById(R.id.etCodigo)
        val btnEscanear = findViewById<Button>(R.id.btnEscanear)
        val btnConectar = findViewById<Button>(R.id.btnConectar)

        // ✅ PEDIR PERMISO DE CÁMARA — SIEMPRE
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), 100
            )
        }

        // ✅ ESCANEAR QR — FUNCIONA SIN INTERNET
        btnEscanear.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                escanearQR()
            } else {
                Toast.makeText(this, "Permiso de cámara obligatorio", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ CONECTAR MANUAL — SIN INTERNET, DIRECTO AL MIKROTIK
        btnConectar.setOnClickListener {
            val cod = etCodigo.text.toString().trim()
            if (cod.length < 6) {
                Toast.makeText(this, "Escribe los 6 dígitos completos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            conectar(cod)
        }
    }

    // ✅ LEER QR → LLENAR → CONECTAR SOLO
    private fun escanearQR() {
        val opciones = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt("Acerca el código QR")
            .setCameraId(0) // CÁMARA TRASERA SIEMPRE
            .setBeepEnabled(true)
            .setOrientationLocked(false)
        escanearLauncher.launch(opciones)
    }

    private val escanearLauncher = registerForActivityResult(ScanContract()) { resultado ->
        if (resultado.contents != null) {
            val soloNumeros = resultado.contents.replace(Regex("\\D"), "")
            val codigo = soloNumeros.take(6)
            etCodigo.setText(codigo)
            Toast.makeText(this, "✅ Código leído", Toast.LENGTH_SHORT).show()
            conectar(codigo) // ✅ CONECTA AUTOMÁTICO DESPUÉS DE LEER
        }
    }

    // ✅ CONECTAR — PRUEBA IP PRIMERO, LUEGO DOMINIO
    private fun conectar(codigo: String) {
        val url = "http://$IP_MIKROTIK/login?username=$codigo&password=$codigo"
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        Toast.makeText(this, "🔌 Conectando...", Toast.LENGTH_SHORT).show()
    }
}
