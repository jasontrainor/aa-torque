package com.aatorque.prefs

import android.app.AlertDialog
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aatorque.stats.DashboardFragment
import com.aatorque.stats.R
import com.rarepebble.colorpicker.ColorPickerView
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.math.roundToInt


class DashboardPreviewFragment : Fragment() {
    val handler = Handler(Looper.getMainLooper())

    private var selectedGauge = 0

    // Snap grid: 6 columns × 3 rows
    private val snapXFractions = floatArrayOf(1f/12, 3f/12, 5f/12, 7f/12, 9f/12, 11f/12)
    private val snapYFractions = floatArrayOf(1f/6, 3f/6, 5f/6)

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

        val btnG1 = view.findViewById<Button>(R.id.btn_gauge_1)
        val btnG2 = view.findViewById<Button>(R.id.btn_gauge_2)
        val btnG3 = view.findViewById<Button>(R.id.btn_gauge_3)
        val seekRotation = view.findViewById<SeekBar>(R.id.seekbar_rotation)
        val textRotation = view.findViewById<TextView>(R.id.text_rotation_value)
        val switchFlip = view.findViewById<Switch>(R.id.switch_flip_sweep)

        val btnBg = view.findViewById<Button>(R.id.btn_bg_color)
        val btnAccent = view.findViewById<Button>(R.id.btn_accent_color)
        val btnNeedle = view.findViewById<Button>(R.id.btn_needle_color)
        val btnRedline = view.findViewById<Button>(R.id.btn_redline_color)
        val shapeSelector = view.findViewById<RadioGroup>(R.id.gauge_shape_selector)
        val presetRow = view.findViewById<LinearLayout>(R.id.preset_row)

        // Build preset swatches
        buildPresetRow(presetRow)

        // Gauge selector
        fun selectGauge(index: Int) {
            selectedGauge = index
            listOf(btnG1, btnG2, btnG3).forEachIndexed { i, btn ->
                btn.alpha = if (i == index) 1f else 0.5f
            }
            // Load per-gauge settings for selected gauge
            lifecycleScope.launch {
                val prefs = requireContext().dataStore.data.first()
                val screenIndex = prefs.currentScreen.let { abs(it) % prefs.screensCount }
                val display = prefs.getScreens(screenIndex).getGauges(index)
                val rotDeg = display.gaugeRotation.roundToInt()
                seekRotation.progress = rotDeg
                textRotation.text = "${rotDeg}°"
                switchFlip.isChecked = display.reverseSweep
            }
            // Attach drag listener for this gauge
            attachDragListenerToGauge(index)
        }

        btnG1.setOnClickListener { selectGauge(0) }
        btnG2.setOnClickListener { selectGauge(1) }
        btnG3.setOnClickListener { selectGauge(2) }

        // Default to G1 selected
        selectGauge(0)

        // Rotation seekbar — snap to nearest 45° on stop
        seekRotation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                textRotation.text = "${progress}°"
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                val snapped = ((sb.progress.toFloat() / 45f).roundToInt() * 45) % 360
                sb.progress = snapped
                textRotation.text = "${snapped}°"
                saveGaugeField(selectedGauge) { it.setGaugeRotation(snapped.toFloat()) }
            }
        })

        // Flip sweep toggle
        switchFlip.setOnCheckedChangeListener { _, checked ->
            saveGaugeField(selectedGauge) { it.setReverseSweep(checked) }
        }

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

    private fun attachDragListenerToGauge(gaugeIndex: Int) {
        val dashFrag = childFragmentManager.findFragmentById(R.id.preview_container) as? DashboardFragment
            ?: return
        val gaugeViews = dashFrag.gaugeViews

        // Remove any existing touch listeners from all gauge views
        gaugeViews.forEach { gv -> gv?.setOnTouchListener(null) }

        val gv = gaugeViews[gaugeIndex] ?: return

        // The preview is inside a RotateLayout rotated -90°. Touch coordinates from the
        // gauge view arrive in the rotated space. We map them back to the logical dashboard
        // space by computing the inverse transform of the RotateLayout.
        var dragStartX = 0f
        var dragStartY = 0f
        var viewStartTransX = 0f
        var viewStartTransY = 0f

        gv.setOnTouchListener { v, event ->
            // Convert touch point from gauge-view local space to dashboard root space
            val dashRoot = dashFrag.rootView
            val touchInRoot = FloatArray(2)
            touchInRoot[0] = event.rawX
            touchInRoot[1] = event.rawY

            // Map raw screen coords into the dashboard fragment's coordinate space.
            // The RotateLayout applies a -90° rotation around its centre — invert that.
            val rotateLayout = view?.findViewById<View>(R.id.preview_container)?.parent as? View
            val matrix = Matrix()
            if (rotateLayout != null) {
                val cx = rotateLayout.x + rotateLayout.width / 2f
                val cy = rotateLayout.y + rotateLayout.height / 2f
                matrix.postTranslate(-cx, -cy)
                matrix.postRotate(90f)
                matrix.postTranslate(cx, cy)
            }
            matrix.mapPoints(touchInRoot)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragStartX = touchInRoot[0]
                    dragStartY = touchInRoot[1]
                    viewStartTransX = v.translationX
                    viewStartTransY = v.translationY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    v.translationX = viewStartTransX + (touchInRoot[0] - dragStartX)
                    v.translationY = viewStartTransY + (touchInRoot[1] - dragStartY)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Snap to nearest grid point
                    val rootW = dashRoot.width.toFloat()
                    val rootH = dashRoot.height.toFloat()
                    if (rootW > 0 && rootH > 0) {
                        val currentCenterX = (v.left + v.width / 2f + v.translationX)
                        val currentCenterY = (v.top + v.height / 2f + v.translationY)
                        val fracX = currentCenterX / rootW
                        val fracY = currentCenterY / rootH

                        val snappedFracX = snapXFractions.minByOrNull { abs(it - fracX) }!!
                        val snappedFracY = snapYFractions.minByOrNull { abs(it - fracY) }!!

                        val defaultCenterX = v.left + v.width / 2f
                        val defaultCenterY = v.top + v.height / 2f
                        v.translationX = snappedFracX * rootW - defaultCenterX
                        v.translationY = snappedFracY * rootH - defaultCenterY

                        saveGaugeField(gaugeIndex) {
                            it.setGaugePosX(snappedFracX).setGaugePosY(snappedFracY)
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun saveGaugeField(gaugeIndex: Int, update: (com.aatorque.datastore.Display.Builder) -> com.aatorque.datastore.Display.Builder) {
        val context = requireContext()
        GlobalScope.launch(Dispatchers.IO) {
            context.dataStore.updateData { prefs ->
                val screenIndex = abs(prefs.currentScreen) % prefs.screensCount
                val screen = prefs.getScreens(screenIndex)
                val display = screen.getGauges(gaugeIndex)
                val updatedDisplay = update(display.toBuilder()).build()
                val updatedScreen = screen.toBuilder().setGauges(gaugeIndex, updatedDisplay).build()
                prefs.toBuilder().setScreens(screenIndex, updatedScreen).build()
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
