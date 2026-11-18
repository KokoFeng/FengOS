package com.feng.fengos

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.provider.Settings
import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout

class MainActivity : Activity() {

    private lateinit var root: FrameLayout
    private var home: HomeView? = null
    private var setup: SetupView? = null
    private var started = false
    private var topI = 0
    private var botI = 0

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        U.init(this)

        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        if (Build.VERSION.SDK_INT >= 28) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }

        root = FrameLayout(this)
        root.setBackgroundColor(Color.BLACK)
        setContentView(root)

        root.setOnApplyWindowInsetsListener { _, ins ->
            val top: Int
            val bot: Int
            var ime = 0
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = ins.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                top = bars.top
                bot = bars.bottom
                ime = ins.getInsets(WindowInsets.Type.ime()).bottom
            } else {
                top = ins.systemWindowInsetTop
                bot = ins.systemWindowInsetBottom
            }
            if (!started) {
                started = true
                topI = top
                botI = bot
                begin()
            } else {
                val keyboard = if (Build.VERSION.SDK_INT >= 30) ime else bot
                setup?.setImeInset(maxOf(0, keyboard - botI))
            }
            ins
        }
        root.requestApplyInsets()
    }

    private fun prefs() = getSharedPreferences("fengos", Context.MODE_PRIVATE)

    private fun begin() {
        if (prefs().getBoolean("setup_done", false)) {
            showHome()
        } else {
            root.addView(
                BootView(this) { root.post { showSetup() } },
                FrameLayout.LayoutParams(MATCH, MATCH)
            )
        }
    }

    private fun showSetup() {
        root.removeAllViews()
        val s = SetupView(this, topI, botI) {
            prefs().edit().putBoolean("setup_done", true).apply()
            setup = null
            root.removeAllViews()
            showHome()
        }
        setup = s
        root.addView(s, FrameLayout.LayoutParams(MATCH, MATCH))
    }

    private fun showHome() {
        val h = HomeView(this, topI, botI) { requestFiles() }
        home = h
        h.alpha = 0f
        root.addView(h, FrameLayout.LayoutParams(MATCH, MATCH))
        h.animate().alpha(1f).setDuration(500).start()
    }

    private fun requestFiles() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                startActivity(
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName"))
                )
            } catch (e: Exception) {
                try {
                    startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                } catch (e2: Exception) {
                }
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        home?.reindex(false)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        home?.reindex(true)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) home?.onHomePressed()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        // launcher: jangan keluar; hanya tutup spotlight / mode goyang
        home?.onBack()
    }
}
