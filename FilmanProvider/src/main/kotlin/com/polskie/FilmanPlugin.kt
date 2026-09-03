package com.polskie

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class FilmanPlugin : Plugin() {
    override fun load(context: Context) {
        val provider = FilmanProvider(context)
        registerMainAPI(provider)

        this.openSettings = { ctx ->
            val prefs = ctx.getSharedPreferences("FilmanPrefs", Context.MODE_PRIVATE)
            val layout = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 20, 40, 20)
            }

            val userField = EditText(ctx).apply {
                hint = "Login / E-mail"
                setText(prefs.getString("filman_username", ""))
            }
            val passField = EditText(ctx).apply {
                hint = "Hasło"
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                setText(prefs.getString("filman_password", ""))
            }
            val cookieField = EditText(ctx).apply {
                hint = "Opcjonalnie: Cookie sesji (gdy Cloudflare)"
                setText(prefs.getString("filman_cookie", ""))
            }

            layout.addView(userField)
            layout.addView(passField)
            layout.addView(cookieField)

            AlertDialog.Builder(ctx)
                .setTitle("Logowanie Filman.cc")
                .setView(layout)
                .setPositiveButton("Zapisz") { _, _ ->
                    prefs.edit()
                        .putString("filman_username", userField.text.toString().trim())
                        .putString("filman_password", passField.text.toString().trim())
                        .putString("filman_cookie", cookieField.text.toString().trim())
                        .apply()
                    Toast.makeText(ctx, "Zapisano ustawienia Filman", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Anuluj", null)
                .show()
        }
    }
}
