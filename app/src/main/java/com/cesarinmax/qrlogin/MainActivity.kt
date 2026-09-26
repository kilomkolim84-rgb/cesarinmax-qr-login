package com.cesarinmax.qrlogin

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val tvEstado = findViewById<TextView>(R.id.tvEstado)
        val btnConectar = findViewById<Button>(R.id.btnConectar)
        
        tvEstado.text = "✅ La app abre bien"
        
        btnConectar.setOnClickListener {
            Toast.makeText(this, "Botón funciona", Toast.LENGTH_SHORT).show()
        }
    }
}
