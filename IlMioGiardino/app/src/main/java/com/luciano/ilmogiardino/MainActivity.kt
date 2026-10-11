package com.luciano.ilmogiardino

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Notifier.ensureChannel(this)
        if (!Notifier.hasPermission(this)) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        if (savedInstanceState == null) show(PlantsFragment())
    }

    fun show(f: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.container, f).addToBackStack(null).commit()
    }
}
