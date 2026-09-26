package com.cesarinmax.qrlogin   // ✅ TU RUTA, NO LA TOQUES

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    // TUS DATOS — AQUÍ SÍ PONES LOS TUYOS
    private val MIKROTIK_IP = "172.16.1.1"
    private val MIKROTIK_USER = "tu_usuario"       // ← TU USUARIO
    private val MIKROTIK_PASS = "tu_contraseña"    // ← TU CONTRASEÑA
    private val PUERTO_WEB = 80

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
            tvEstado.setTextColor(0xFFFF6600.toInt())
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    100
                )
            } else {
                escanearQR()
            }
        }

        btnConectar.setOnClickListener {
            if (esValido && codigoEscaneado != null) {
                activarTicketEnMikrotik(codigoEscaneado!!)
            } else {
                Toast.makeText(this, "Escanea un código válido primero", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun estaEnRedCorrecta(): Boolean {
        val wifiMgr = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val ip = wifiMgr.connectionInfo.ipAddress
        val ipStr = String.format(
            "%d.%d.%d.%d",
            ip and 0xFF,
            ip shr 8 and 0xFF,
            ip shr 16 and 0xFF,
            ip shr 24 and 0xFF
        )
        return ipStr.startsWith("172.16.1.")
    }

    private fun escanearQR() {
        val opciones = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt("📸 Escanea el código del ticket")
            .setCameraId(0)
            .setBeepEnabled(true)
            .setOrientationLocked(false)
        escanearLauncher.launch(opciones)
    }

    private val escanearLauncher = registerForActivityResult(ScanContract()) { resultado ->
        if (resultado.contents != null) {
            codigoEscaneado = resultado.contents.trim()
            tvCodigo.text = "Código: $codigoEscaneado"
            tvEstado.text = "✅ Código leído, verificando..."
            tvEstado.setTextColor(0xFF4CAF50.toInt())
            verificarEnMikrotik(codigoEscaneado!!)
        } else {
            tvEstado.text = "❌ Escaneo cancelado"
        }
    }

    private fun verificarEnMikrotik(codigo: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("http://$MIKROTIK_IP:$PUERTO_WEB/rest/user/list")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val auth = android.util.Base64.encodeToString(
                    "$MIKROTIK_USER:$MIKROTIK_PASS".toByteArray(),
                    android.util.Base64.NO_WRAP
                )
                conn.setRequestProperty("Authorization", "Basic $auth")

                val lector = BufferedReader(InputStreamReader(conn.inputStream))
                val respuesta = StringBuilder()
                var linea: String?
                while (lector.readLine().also { linea = it } != null) {
                    respuesta.append(linea)
                }
                lector.close()

                val texto = respuesta.toString()
                val existe = texto.contains("\"name\":\"$codigo\"", ignoreCase = true)
                val estaActivo = existe && !texto.contains("\"name\":\"$codigo\"", ignoreCase = true) ||
                                 !texto.contains("\"disabled\":true", ignoreCase = true)

                withContext(Dispatchers.Main) {
                    if (existe && estaActivo) {
                        esValido = true
                        btnConectar.isEnabled = true
                        btnConectar.alpha = 1.0f
                        tvEstado.text = "✅ Ticket válido → Toca CONECTAR"
                    } else if (existe && !estaActivo) {
                        esValido = false
                        btnConectar.isEnabled = false
                        tvEstado.text = "❌ Este ticket ya fue usado"
                        tvEstado.setTextColor(0xFFFF0000.toInt())
                    } else {
                        esValido = false
                        btnConectar.isEnabled = false
                        tvEstado.text = "❌ Ticket no existe"
                        tvEstado.setTextColor(0xFFFF0000.toInt())
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvEstado.text = "⚠️ No se conecta al MikroTik"
                    tvEstado.setTextColor(0xFFFF6600.toInt())
                }
            }
        }
    }

    private fun activarTicketEnMikrotik(codigo: String) {
        tvEstado.text = "🔄 Activando acceso..."
        btnConectar.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("http://$MIKROTIK_IP:$PUERTO_WEB/rest/user/$codigo/enable")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                val auth = android.util.Base64.encodeToString(
                    "$MIKROTIK_USER:$MIKROTIK_PASS".toByteArray(),
                    android.util.Base64.NO_WRAP
                )
                conn.setRequestProperty("Authorization", "Basic $auth")

                val codigoRespuesta = conn.responseCode

                withContext(Dispatchers.Main) {
                    if (codigoRespuesta in 200..299) {
                        tvEstado.text = "🎉 ¡BIENVENIDO! Conexión lista ✅"
                        tvEstado.setTextColor(0xFF4CAF50.toInt())
                    } else {
                        tvEstado.text = "❌ Error al activar: $codigoRespuesta"
                        btnConectar.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvEstado.text = "❌ Error: ${e.message}"
                    btnConectar.isEnabled = true
                }
            }
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
