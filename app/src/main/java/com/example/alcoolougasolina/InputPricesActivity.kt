package com.example.alcoolougasolina

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import android.location.Geocoder
import android.util.Log
import android.widget.Switch
import android.widget.TextView
import java.util.Locale

class InputPricesActivity : AppCompatActivity() {
    private lateinit var etPrecoAlcool: EditText
    private lateinit var etPrecoGasolina: EditText
    private lateinit var btCalc: Button
    private lateinit var textMsg: TextView

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var switch: Switch
    private var swPercentual = 70

    private var latitude: Double? = null
    private var longitude: Double? = null
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input_prices)

        initUI()
        loadPreferences()
        setupListeners()
    }

    private fun initUI() {
        latitude = intent.getDoubleExtra("latitude", 0.0)
        longitude = intent.getDoubleExtra("longitude", 0.0)

        etPrecoAlcool = findViewById(R.id.edAlcool)
        etPrecoGasolina = findViewById(R.id.edGasolina)
        btCalc = findViewById(R.id.btCalcular)
        textMsg = findViewById(R.id.textMsg)
        switch = findViewById(R.id.swPercentual)

        btnSave = findViewById(R.id.btnSave)
    }

    private fun loadPreferences() {
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

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

        btCalc.setOnClickListener {
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

                    with(sharedPreferences.edit()) {
                        putFloat("precoAlcool", precoAlcool)
                        putFloat("precoGasolina", precoGasolina)
                        putInt("swPercentual", swPercentual)
                        putBoolean("isChecked", switch.isChecked)
                        putString("textMensagem", textMsg.text.toString())
                        apply()
                    }
                } catch (e: Exception) {
                    Log.e("InputPricesActivity", "Error updating text message", e)
                    textMsg.text = getString(R.string.preecha_valores)
                }
            } else {
                textMsg.text = getString(R.string.preecha_valores)
            }
        }
    }

    private fun updatePercentualText(swPercentual: Int) {
        switch.text = getString(
            if (swPercentual == 75) R.string.percentual_75 else R.string.percentual_70
        )
    }

    private fun setupListeners() {
        switch.setOnCheckedChangeListener { _, isChecked ->
            swPercentual = if (isChecked) 75 else 70
            updatePercentualText(swPercentual)
        }
        btnSave.setOnClickListener { savePrices() }
    }

    private fun savePrices() {
        val precoAlcool = etPrecoAlcool.text.toString().replace(",", ".")
        val precoGasolina = etPrecoGasolina.text.toString().replace(",", ".")

        if (precoAlcool.isNotEmpty() && precoGasolina.isNotEmpty()) {
            val geocoder = Geocoder(this, Locale.getDefault())
            var address = "Localização desconhecida"
            try {
                val addresses = geocoder.getFromLocation(latitude ?: 0.0, longitude ?: 0.0, 1)
                if (addresses != null) {
                    if (addresses.isNotEmpty()) {
                        address = addresses[0].getAddressLine(0)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val history =
                "📍 Localização: $address/ 💧 Álcool: R$ $precoAlcool /⛽ Gasolina: R$ $precoGasolina"

            val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            var existingHistory = sharedPreferences.getString("history", "") ?: ""
            existingHistory += "$history\n\n"

            editor.putString("history", existingHistory)
            editor.apply()

            Toast.makeText(this, "Preços salvos!", Toast.LENGTH_SHORT).show()
            btnSave.isEnabled = false
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            Toast.makeText(this, "Por favor, preencha os preços corretamente!", Toast.LENGTH_SHORT)
                .show()
        }
    }
}
