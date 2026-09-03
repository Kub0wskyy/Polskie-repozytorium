package com.polskie

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class ZaluknijPlugin : Plugin() {
    override fun load(context: Context) {
        val provider = ZaluknijProvider(context)
        registerMainAPI(provider)

        this.openSettings = { ctx ->
            val prefs = ctx.getSharedPreferences("ZaluknijPrefs", Context.MODE_PRIVATE)
            val layout = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 20, 40, 20)
            }

            val domainField = EditText(ctx).apply {
                hint = "Domena (np. https://zaluknij.cc)"
                setText(prefs.getString("zaluknij_domain", "https://zaluknij.cc"))
            }
            val userField = EditText(ctx).apply {
                hint = "Login / E-mail"
                setText(prefs.getString("zaluknij_username", ""))
            }
            val passField = EditText(ctx).apply {
                hint = "Hasło"
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                setText(prefs.getString("zaluknij_password", ""))
            }
            val cookieField = EditText(ctx).apply {
                hint = "Ciasteczka (gdy wymagana autoryzacja)"
                setText(prefs.getString("zaluknij_cookie", ""))
            }

            layout.addView(domainField)
            layout.addView(userField)
            layout.addView(passField)
            layout.addView(cookieField)

            AlertDialog.Builder(ctx)
                .setTitle("Ustawienia Zaluknij.cc")
                .setView(layout)
                .setPositiveButton("Zapisz") { _, _ ->
                    prefs.edit()
                        .putString("zaluknij_domain", domainField.text.toString().trim())
                        .putString("zaluknij_username", userField.text.toString().trim())
                        .putString("zaluknij_password", passField.text.toString().trim())
                        .putString("zaluknij_cookie", cookieField.text.toString().trim())
                        .apply()
                    Toast.makeText(ctx, "Zapisano ustawienia Zaluknij", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Anuluj", null)
                .show()
        }
    }
}
