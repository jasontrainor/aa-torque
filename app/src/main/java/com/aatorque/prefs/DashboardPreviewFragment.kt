package com.aatorque.prefs

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StyleRes
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.aatorque.stats.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import android.widget.Button
import android.widget.LinearLayout
import android.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.rarepebble.colorpicker.ColorPickerView
import kotlinx.coroutines.launch


class DashboardPreviewFragment: Fragment() {
    val handler = Handler(Looper.getMainLooper())
    companion object {
        val hideBars = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.navigationBars()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard_preview, container, false)
        val data = runBlocking {
            requireContext().dataStore.data.first()
        }
        inflater.context.setTheme(mapTheme(requireContext(), data.selectedTheme))
        forceRotate(true)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val panel = view.findViewById<LinearLayout>(R.id.theme_builder_panel)
        val btnBg = view.findViewById<Button>(R.id.btn_bg_color)
        val btnAccent = view.findViewById<Button>(R.id.btn_accent_color)
        val btnNeedle = view.findViewById<Button>(R.id.btn_needle_color)
        val btnRedline = view.findViewById<Button>(R.id.btn_redline_color)
        
        lifecycleScope.launch {
            requireContext().dataStore.data.collect { prefs ->
                panel.visibility = if (prefs.selectedTheme == "Custom") View.VISIBLE else View.GONE
                
                btnBg.setOnClickListener {
                    pickColor("Background Color", prefs.customBackgroundColor) { color ->
                        lifecycleScope.launch {
                            requireContext().dataStore.updateData { it.toBuilder().setCustomBackgroundColor(color).build() }
                        }
                    }
                }
                btnAccent.setOnClickListener {
                    pickColor("Accent Color", prefs.customAccentColor) { color ->
                        lifecycleScope.launch {
                            requireContext().dataStore.updateData { it.toBuilder().setCustomAccentColor(color).build() }
                        }
                    }
                }
                btnNeedle.setOnClickListener {
                    pickColor("Needle Color", prefs.customNeedleColor) { color ->
                        lifecycleScope.launch {
                            requireContext().dataStore.updateData { it.toBuilder().setCustomNeedleColor(color).build() }
                        }
                    }
                }
                btnRedline.setOnClickListener {
                    pickColor("Redline Color", prefs.customRedlineColor) { color ->
                        lifecycleScope.launch {
                            requireContext().dataStore.updateData { it.toBuilder().setCustomRedlineColor(color).build() }
                        }
                    }
                }
            }
        }
    }

    private fun pickColor(title: String, initialColor: Int, onColorSelected: (Int) -> Unit) {
        val picker = ColorPickerView(requireContext())
        picker.showHex(false)
        picker.color = initialColor
        
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(picker)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                onColorSelected(picker.color)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    fun forceRotate(isOn: Boolean) {
        requireActivity().requestedOrientation = if(isOn) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    override fun onResume() {
        super.onResume()
        forceRotate(true)

        val window = requireActivity().window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).hide(hideBars)
        WindowCompat.getInsetsController(window, window.decorView).systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onPause() {
        super.onPause()
        val window = requireActivity().window
        forceRotate(false)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).show(hideBars)
    }

}

@StyleRes
fun mapTheme(context: Context, theme: String?):  Int {
    val findIndex = context.resources.getStringArray(R.array.Themes).indexOf(theme)
    if (findIndex != -1) {
        val arr = context.resources.obtainTypedArray(R.array.ThemeList)
        try{
            return arr.getResourceId(findIndex, R.style.AppTheme_AudiVC)
        } finally {
            arr.recycle()
        }
    }
    return R.style.AppTheme_AudiVC
}