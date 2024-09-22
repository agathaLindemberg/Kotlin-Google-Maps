package com.example.alcoolougasolina

import android.content.Context
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HistoryActivity : AppCompatActivity() {

    private lateinit var linearLayoutHistory: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        linearLayoutHistory = findViewById(R.id.linearLayoutHistory)

        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val history = sharedPreferences.getString("history", "")

        displayHistory(history)
    }

    private fun displayHistory(history: String?) {
        val entries = history?.lines()?.filter { it.isNotBlank() } ?: return

        if (entries.isEmpty()) {
            addTextView("Nenhum histórico disponível.")
            return
        }

        entries.forEach { entry ->
            val formattedEntry = formatEntry(entry)
            addTextView(formattedEntry)
        }
    }

    private fun formatEntry(entry: String): String {
        val parts = entry.split("\n")
        return parts.joinToString("\n") {
            it.trim()
        }
    }

    private fun addTextView(text: String) {
        val textView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 8, 8, 8)
            }
            setPadding(16, 16, 16, 16)
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.black))
            setBackgroundResource(R.drawable.entry_background)

            this.text = text
        }

        linearLayoutHistory.addView(textView)
    }
}
