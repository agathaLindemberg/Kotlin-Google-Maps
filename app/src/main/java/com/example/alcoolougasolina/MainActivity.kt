package com.example.alcoolougasolina

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity() {
    private lateinit var etPrecoAlcool: EditText
    private lateinit var etPrecoGasolina: EditText
    private lateinit var btCalc: Button
    private lateinit var btnOpenMaps: Button
    private lateinit var textMsg: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sharedPreferences: SharedPreferences

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var switch: Switch
    private var swPercentual = 70

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initUI()
        loadPreferences()
        setupListeners()
    }

    private fun initUI() {
        btnOpenMaps = findViewById(R.id.btnOpenMaps)
        etPrecoAlcool = findViewById(R.id.edAlcool)
        etPrecoGasolina = findViewById(R.id.edGasolina)
        btCalc = findViewById(R.id.btCalcular)
        textMsg = findViewById(R.id.textMsg)
        switch = findViewById(R.id.swPercentual)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    }

    private fun setupListeners() {
        btnOpenMaps.setOnClickListener { openMaps() }

        btCalc.setOnClickListener { calculateRentability() }

        switch.setOnCheckedChangeListener { _, isChecked ->
            swPercentual = if (isChecked) 75 else 70
            updatePercentualText(swPercentual)
        }
    }

    private fun loadPreferences() {
        val precoAlcool = sharedPreferences.getFloat("precoAlcool", 0.0f)
        val precoGasolina = sharedPreferences.getFloat("precoGasolina", 0.0f)
        val isChecked = sharedPreferences.getBoolean("isChecked", false)
        val textMensagem = sharedPreferences.getString("textMensagem", "")

        etPrecoAlcool.setText(precoAlcool.toString())
        etPrecoGasolina.setText(precoGasolina.toString())
        switch.isChecked = isChecked
        textMsg.text = textMensagem
        swPercentual = if (isChecked) 75 else 70

        updatePercentualText(swPercentual)
    }

    private fun updatePercentualText(swPercentual: Int) {
        switch.text = getString(
            if (swPercentual == 75) R.string.percentual_75 else R.string.percentual_70
        )
    }

    @SuppressLint("MissingPermission")
    private fun openMaps() {
        if (checkLocationPermissions()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val gmmIntentUri = Uri.parse(
                        "geo:$latitude,$longitude?q=postos+de+gasolina&radius=300"
                    )
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    startActivity(mapIntent)
                } else {
                    Toast.makeText(
                        this, "Não foi possível obter a localização atual",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun checkLocationPermissions(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            false
        } else {
            true
        }
    }

    private fun calculateRentability() {
        val precoAlcoolText = etPrecoAlcool.text.toString()
        val precoGasolinaText = etPrecoGasolina.text.toString()

        if (precoAlcoolText.isNotEmpty() && precoGasolinaText.isNotEmpty()) {
            try {
                val precoAlcool = precoAlcoolText.toFloat()
                val precoGasolina = precoGasolinaText.toFloat()
                val percentual = precoAlcool / precoGasolina * 100
                val percentualFormatado = String.format("%.2f", percentual)

                textMsg.text = if (percentual <= swPercentual) {
                    getString(
                        R.string.mensagem_alcool_mais_rentavel,
                        percentualFormatado,
                        swPercentual
                    )
                } else {
                    getString(
                        R.string.mensagem_gasolina_mais_rentavel,
                        percentualFormatado,
                        swPercentual
                    )
                }

                savePreferences(precoAlcool, precoGasolina, textMsg.text.toString())
            } catch (e: Exception) {
                Log.e("MainActivity", "Erro ao calcular rentabilidade", e)
                textMsg.text = getString(R.string.preecha_valores)
            }
        } else {
            textMsg.text = getString(R.string.preecha_valores)
        }
    }

    private fun savePreferences(precoAlcool: Float, precoGasolina: Float, mensagem: String) {
        with(sharedPreferences.edit()) {
            putFloat("precoAlcool", precoAlcool)
            putFloat("precoGasolina", precoGasolina)
            putBoolean("isChecked", switch.isChecked)
            putString("textMensagem", mensagem)
            apply()
        }
    }
}
