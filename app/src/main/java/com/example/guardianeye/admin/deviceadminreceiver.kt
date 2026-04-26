package com.example.guardianeye.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class DeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(
            context,
            "Protection Guardian Eye activée",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDisableRequested(
        context: Context,
        intent: Intent
    ): CharSequence {
        return "Entrez le code secret pour désactiver la protection"
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(
            context,
            "Protection désactivée",
            Toast.LENGTH_SHORT
        ).show()
    }
}