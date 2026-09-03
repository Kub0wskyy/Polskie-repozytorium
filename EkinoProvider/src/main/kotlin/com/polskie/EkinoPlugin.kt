package com.polskie

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class EkinoPlugin : Plugin() {
    override fun load(context: Context) {
        val provider = EkinoProvider(context)
        registerMainAPI(provider)

        this.openSettings = { ctx ->
            val prefs = ctx.getSharedPreferences("EkinoPrefs", Context.MODE_PRIVATE)
            val layout = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 20, 40, 20)
            }

            val userField = EditText(ctx).apply {
                hint = "Login Ekino"
                setText(prefs.getString("ekino_username", ""))
            }
            val passField = EditText(ctx).apply {
                hint = "Hasło Ekino"
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                setText(prefs.getString("ekino_password", ""))
            }
            val cookieField = EditText(ctx).apply {
                hint = "Cookie (PHPSESSID itp.)"
                setText(prefs.getString("ekino_cookie", ""))
            }

            layout.addView(userField)
            layout.addView(passField)
            layout.addView(cookieField)

            AlertDialog.Builder(ctx)
                .setTitle("Logowanie Ekino-tv.pl")
                .setView(layout)
                .setPositiveButton("Zapisz") { _, _ ->
                    prefs.edit()
                        .putString("ekino_username", userField.text.toString().trim())
                        .putString("ekino_password", passField.text.toString().trim())
                        .putString("ekino_cookie", cookieField.text.toString().trim())
                        .apply()
                    Toast.makeText(ctx, "Zapisano dane Ekino-tv", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Anuluj", null)
                .show()
        }
    }
}
