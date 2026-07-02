package com.aatorque.prefs

import android.app.AlertDialog
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aatorque.stats.R
import com.rarepebble.colorpicker.ColorPickerView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class DashboardPreviewFragment : Fragment() {
    val handler = Handler(Looper.getMainLooper())

    companion object {
        val hideBars = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.navigationBars()

        data class ColorPreset(
            val name: String,
            val bg: Int,
            val accent: Int,
            val needle: Int,
            val redline: Int,
        )

        val PRESETS = listOf(
            ColorPreset("Dark",       0xFF1A1A1A.toInt(), 0xFF00BFFF.toInt(), 0xFFFF4444.toInt(), 0xFFFF0000.toInt()),
            ColorPreset("Audi",       0xFF0A0A0A.toInt(), 0xFFFF3333.toInt(), 0xFFFFFFFF.toInt(), 0xFFCC0000.toInt()),
            ColorPreset("Green",      0xFF0D1A0D.toInt(), 0xFF00FF44.toInt(), 0xFF00FF44.toInt(), 0xFFFF0000.toInt()),
            ColorPreset("Amber",      0xFF0D0800.toInt(), 0xFFFFA500.toInt(), 0xFFFFD700.toInt(), 0xFFFF4500.toInt()),
            ColorPreset("Blue Ice",   0xFF010B12.toInt(), 0xFF4FC3F7.toInt(), 0xFFFFFFFF.toInt(), 0xFF0077FF.toInt()),
            ColorPreset("Monochrome", 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFF888888.toInt()),
            ColorPreset("Red Alert",  0xFF120000.toInt(), 0xFFFF2222.toInt(), 0xFFFFAAAA.toInt(), 0xFFFF0000.toInt()),
            ColorPreset("Purple",     0xFF0D000D.toInt(), 0xFFBB44FF.toInt(), 0xFFDD88FF.toInt(), 0xFFFF0044.toInt()),
        )
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

        // Ensure the designer always uses the custom (programmatic) gauge renderer
        if (data.selectedTheme != "Custom") {
            lifecycleScope.launch {
                requireContext().dataStore.updateData {
                    it.toBuilder().setSelectedTheme("Custom").build()
                }
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBg = view.findViewById<Button>(R.id.btn_bg_color)
        val btnAccent = view.findViewById<Button>(R.id.btn_accent_color)
        val btnNeedle = view.findViewById<Button>(R.id.btn_needle_color)
        val btnRedline = view.findViewById<Button>(R.id.btn_redline_color)
        val shapeSelector = view.findViewById<RadioGroup>(R.id.gauge_shape_selector)
        val presetRow = view.findViewById<LinearLayout>(R.id.preset_row)

        // Build preset swatches
        buildPresetRow(presetRow)

        // Shape selector
        lifecycleScope.launch {
            val currentShape = requireContext().dataStore.data.first().gaugeShape
            when (currentShape) {
                1 -> view.findViewById<RadioButton>(R.id.shape_bar_h).isChecked = true
                2 -> view.findViewById<RadioButton>(R.id.shape_bar_v).isChecked = true
                else -> view.findViewById<RadioButton>(R.id.shape_circular).isChecked = true
            }
        }
        shapeSelector.setOnCheckedChangeListener { _, checkedId ->
            val shape = when (checkedId) {
                R.id.shape_bar_h -> 1
                R.id.shape_bar_v -> 2
                else -> 0
            }
            lifecycleScope.launch {
                requireContext().dataStore.updateData {
                    it.toBuilder().setGaugeShape(shape).build()
                }
            }
        }

        // Color pickers
        lifecycleScope.launch {
            requireContext().dataStore.data.collect { prefs ->
                btnBg.setBackgroundColor(prefs.customBackgroundColor)
                btnAccent.setBackgroundColor(prefs.customAccentColor)
                btnNeedle.setBackgroundColor(prefs.customNeedleColor)
                btnRedline.setBackgroundColor(prefs.customRedlineColor)

                btnBg.setOnClickListener {
                    pickColor("Background Color", prefs.customBackgroundColor) { color ->
                        lifecycleScope.launch {
                            requireContext().dataStore.updateData {
                                it.toBuilder().setCustomBackgroundColor(color).build()
                            }
                        }
                    }
                }
                btnAccent.setOnClickListener {
                    pickColor("Accent Color", prefs.customAccentColor) { color ->
                        lifecycleScope.launch {
                            requireContext().dataStore.updateData {
                                it.toBuilder().setCustomAccentColor(color).build()
                            }
                        }
                    }
                }
                btnNeedle.setOnClickListener {
                    pickColor("Needle Color", prefs.customNeedleColor) { color ->
                        lifecycleScope.launch {
                            requireContext().dataStore.updateData {
                                it.toBuilder().setCustomNeedleColor(color).build()
                            }
                        }
                    }
                }
                btnRedline.setOnClickListener {
                    pickColor("Redline Color", prefs.customRedlineColor) { color ->
                        lifecycleScope.launch {
                            requireContext().dataStore.updateData {
                                it.toBuilder().setCustomRedlineColor(color).build()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun buildPresetRow(presetRow: LinearLayout) {
        val dp = requireContext().resources.displayMetrics.density
        val swatchSize = (48 * dp).toInt()
        val swatchMargin = (8 * dp).toInt()

        PRESETS.forEach { preset ->
            val container = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(swatchSize + swatchMargin, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = swatchMargin
                }
            }

            val swatch = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(swatchSize, swatchSize)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(preset.bg)
                    setStroke((2 * dp).toInt(), preset.accent)
                }
                setOnClickListener {
                    applyPreset(preset)
                }
            }

            val label = TextView(requireContext()).apply {
                text = preset.name
                textSize = 9f
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            }

            container.addView(swatch)
            container.addView(label)
            presetRow.addView(container)
        }
    }

    private fun applyPreset(preset: ColorPreset) {
        lifecycleScope.launch {
            requireContext().dataStore.updateData {
                it.toBuilder()
                    .setCustomBackgroundColor(preset.bg)
                    .setCustomAccentColor(preset.accent)
                    .setCustomNeedleColor(preset.needle)
                    .setCustomRedlineColor(preset.redline)
                    .build()
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
        requireActivity().requestedOrientation = if (isOn) {
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
fun mapTheme(context: android.content.Context, theme: String?): Int {
    val findIndex = context.resources.getStringArray(R.array.Themes).indexOf(theme)
    if (findIndex != -1) {
        val arr = context.resources.obtainTypedArray(R.array.ThemeList)
        try {
            return arr.getResourceId(findIndex, R.style.AppTheme_AudiVC)
        } finally {
            arr.recycle()
        }
    }
    return R.style.AppTheme_AudiVC
}
