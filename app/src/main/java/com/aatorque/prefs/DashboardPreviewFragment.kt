package com.aatorque.prefs

import android.app.AlertDialog
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
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
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt


class DashboardPreviewFragment : Fragment() {
    val handler = Handler(Looper.getMainLooper())

    private var selectedGauge = 0

    // Hold references so color button clicks can read current values
    private var currentBgColor = 0
    private var currentAccentColor = 0
    private var currentNeedleColor = 0
    private var currentRedlineColor = 0

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
        val btnAdd = view.findViewById<Button>(R.id.btn_add_gauge)
        val btnRemove = view.findViewById<Button>(R.id.btn_remove_gauge)

        val seekRotation     = view.findViewById<SeekBar>(R.id.seekbar_rotation)
        val textRotation     = view.findViewById<TextView>(R.id.text_rotation_value)
        val switchFlip       = view.findViewById<Switch>(R.id.switch_flip_sweep)
        val seekSize         = view.findViewById<SeekBar>(R.id.seekbar_size)
        val textSizeValue    = view.findViewById<TextView>(R.id.text_size_value)
        val seekTitleSize    = view.findViewById<SeekBar>(R.id.seekbar_title_size)
        val textTitleSize    = view.findViewById<TextView>(R.id.text_title_size_value)

        val shapeSelector    = view.findViewById<RadioGroup>(R.id.gauge_shape_selector)
        val needleSelector   = view.findViewById<RadioGroup>(R.id.needle_style_selector)
        val bgStyleSelector  = view.findViewById<RadioGroup>(R.id.bg_style_selector)
        val labelNeedle      = view.findViewById<TextView>(R.id.label_needle_style)
        val labelBgStyle     = view.findViewById<TextView>(R.id.label_bg_style)

        val btnBg          = view.findViewById<Button>(R.id.btn_bg_color)
        val btnAccent      = view.findViewById<Button>(R.id.btn_accent_color)
        val btnNeedle      = view.findViewById<Button>(R.id.btn_needle_color)
        val btnRedline     = view.findViewById<Button>(R.id.btn_redline_color)
        val btnNeedleImg   = view.findViewById<Button>(R.id.btn_needle_image)
        val btnDialBgImg   = view.findViewById<Button>(R.id.btn_dial_bg_image)
        val btnGaugeIcon   = view.findViewById<Button>(R.id.btn_gauge_icon)
        val presetRow      = view.findViewById<LinearLayout>(R.id.preset_row)

        buildPresetRow(presetRow)

        val gaugeButtons = listOf(btnG1, btnG2, btnG3)

        fun isTextStyle(gaugeStyle: Int) = gaugeStyle == 4

        fun updateNeedleImageVisibility(needleStyle: Int) {
            val isImage = needleStyle == 3
            val imgVis = if (isImage) View.VISIBLE else View.GONE
            btnNeedleImg.visibility = imgVis
            btnDialBgImg.visibility = imgVis
        }

        fun updateStyleVisibility(gaugeStyle: Int) {
            val isText = isTextStyle(gaugeStyle)
            val needleVis = if (isText) View.GONE else View.VISIBLE
            labelNeedle.visibility = needleVis
            needleSelector.visibility = needleVis
            labelBgStyle.visibility = needleVis
            bgStyleSelector.visibility = needleVis
            if (isText) {
                btnNeedleImg.visibility = View.GONE
                btnDialBgImg.visibility = View.GONE
            }
        }

        fun refreshGaugeButtons() {
            lifecycleScope.launch {
                val prefs = requireContext().dataStore.data.first()
                val screenIndex = abs(prefs.currentScreen) % prefs.screensCount
                val screen = prefs.getScreens(screenIndex)
                var enabledCount = 0
                var disabledCount = 0
                gaugeButtons.forEachIndexed { i, btn ->
                    val disabled = if (i < screen.gaugesCount) screen.getGauges(i).disabled else true
                    if (disabled) disabledCount++ else enabledCount++
                    btn.alpha = when {
                        i == selectedGauge -> 1f
                        disabled -> 0.3f
                        else -> 0.6f
                    }
                }
                btnAdd.isEnabled = disabledCount > 0
                btnRemove.isEnabled = enabledCount > 1
            }
        }

        fun loadGaugeSettings(index: Int) {
            lifecycleScope.launch {
                val prefs = requireContext().dataStore.data.first()
                val screenIndex = abs(prefs.currentScreen) % prefs.screensCount
                val display = prefs.getScreens(screenIndex).getGauges(index)

                val rotDeg = display.gaugeRotation.roundToInt()
                seekRotation.progress = rotDeg
                textRotation.text = "${rotDeg}°"
                switchFlip.isChecked = display.reverseSweep

                val sizeProgress = if (display.gaugeSize == 0f) 50
                    else ((display.gaugeSize - 0.5f) * 100f).toInt().coerceIn(0, 150)
                seekSize.progress = sizeProgress
                val scalePct = (0.5f + sizeProgress / 100f) * 100f
                textSizeValue.text = "${scalePct.toInt()}%"

                val titleSp = if (display.titleFontSize == 0) 12 else display.titleFontSize
                seekTitleSize.progress = (titleSp - 8).coerceIn(0, 16)
                textTitleSize.text = "${titleSp}sp"

                when (display.gaugeStyle) {
                    2    -> view.findViewById<RadioButton>(R.id.shape_bar_h).isChecked = true
                    3    -> view.findViewById<RadioButton>(R.id.shape_bar_v).isChecked = true
                    4    -> view.findViewById<RadioButton>(R.id.shape_text).isChecked = true
                    else -> view.findViewById<RadioButton>(R.id.shape_circular).isChecked = true
                }

                when (display.needleStyle) {
                    1    -> view.findViewById<RadioButton>(R.id.needle_line).isChecked = true
                    2    -> view.findViewById<RadioButton>(R.id.needle_triangle).isChecked = true
                    3    -> view.findViewById<RadioButton>(R.id.needle_image).isChecked = true
                    else -> view.findViewById<RadioButton>(R.id.needle_dot).isChecked = true
                }

                when (display.backgroundStyle) {
                    1    -> view.findViewById<RadioButton>(R.id.bg_none).isChecked = true
                    2    -> view.findViewById<RadioButton>(R.id.bg_filled).isChecked = true
                    else -> view.findViewById<RadioButton>(R.id.bg_arc).isChecked = true
                }

                updateStyleVisibility(display.gaugeStyle)
                updateNeedleImageVisibility(display.needleStyle)

                val needleName = display.customNeedle
                btnNeedleImg.text = if (needleName.isNotEmpty()) needleName else "Select Needle Image"
                val bgName = display.customDialBackground
                btnDialBgImg.text = if (bgName.isNotEmpty()) bgName else "Select Dial Background"
                val iconName = display.icon
                btnGaugeIcon.text = if (iconName.isNotEmpty() && iconName != "ic_none") iconName else "Select Icon"

                currentBgColor     = if (display.customBgColor     != 0) display.customBgColor     else prefs.customBackgroundColor
                currentAccentColor = if (display.customAccentColor != 0) display.customAccentColor else prefs.customAccentColor
                currentNeedleColor = if (display.customNeedleColor != 0) display.customNeedleColor else prefs.customNeedleColor
                currentRedlineColor= if (display.customRedlineColor!= 0) display.customRedlineColor else prefs.customRedlineColor

                btnBg.setBackgroundColor(currentBgColor)
                btnAccent.setBackgroundColor(currentAccentColor)
                btnNeedle.setBackgroundColor(currentNeedleColor)
                btnRedline.setBackgroundColor(currentRedlineColor)
            }
        }

        fun selectGauge(index: Int) {
            selectedGauge = index
            gaugeButtons.forEachIndexed { i, btn ->
                btn.alpha = if (i == index) 1f else 0.5f
            }
            loadGaugeSettings(index)
            attachDragListenerToGauge(index)
            refreshGaugeButtons()
        }

        btnG1.setOnClickListener { selectGauge(0) }
        btnG2.setOnClickListener { selectGauge(1) }
        btnG3.setOnClickListener { selectGauge(2) }

        btnAdd.setOnClickListener {
            lifecycleScope.launch {
                val prefs = requireContext().dataStore.data.first()
                val screenIndex = abs(prefs.currentScreen) % prefs.screensCount
                val screen = prefs.getScreens(screenIndex)
                val disabledIndex = (0 until screen.gaugesCount).firstOrNull { screen.getGauges(it).disabled }
                if (disabledIndex != null) {
                    withContext(Dispatchers.IO) {
                        requireContext().dataStore.updateData { p ->
                            val si = abs(p.currentScreen) % p.screensCount
                            val s = p.getScreens(si)
                            val d = s.getGauges(disabledIndex)
                            val updated = d.toBuilder().setDisabled(false).build()
                            val updatedScreen = s.toBuilder().setGauges(disabledIndex, updated).build()
                            p.toBuilder().setScreens(si, updatedScreen).build()
                        }
                    }
                    selectGauge(disabledIndex)
                }
            }
        }

        btnRemove.setOnClickListener {
            lifecycleScope.launch {
                val prefs = requireContext().dataStore.data.first()
                val screenIndex = abs(prefs.currentScreen) % prefs.screensCount
                val screen = prefs.getScreens(screenIndex)
                val enabledCount = (0 until screen.gaugesCount).count { !screen.getGauges(it).disabled }
                if (enabledCount <= 1) return@launch  // keep at least one gauge
                val nextEnabled = (0 until screen.gaugesCount).firstOrNull {
                    it != selectedGauge && !screen.getGauges(it).disabled
                } ?: 0
                withContext(Dispatchers.IO) {
                    requireContext().dataStore.updateData { p ->
                        val si = abs(p.currentScreen) % p.screensCount
                        val s = p.getScreens(si)
                        val d = s.getGauges(selectedGauge)
                        val updated = d.toBuilder().setDisabled(true).build()
                        val updatedScreen = s.toBuilder().setGauges(selectedGauge, updated).build()
                        p.toBuilder().setScreens(si, updatedScreen).build()
                    }
                }
                selectGauge(nextEnabled)
            }
        }

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

        // Size seekbar (0–100 → 50%–150%)
        seekSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                val scalePct = ((0.5f + progress / 100f) * 100f).toInt()
                textSizeValue.text = "${scalePct}%"
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                val scale = 0.5f + sb.progress / 100f
                saveGaugeField(selectedGauge) { it.setGaugeSize(scale) }
            }
        })

        // Title font size seekbar (progress 0-16 → 8sp-24sp)
        seekTitleSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                textTitleSize.text = "${8 + progress}sp"
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                val sp = 8 + sb.progress
                saveGaugeField(selectedGauge) { it.setTitleFontSize(sp) }
            }
        })

        // Gauge shape — per-gauge
        shapeSelector.setOnCheckedChangeListener { _, checkedId ->
            val style = when (checkedId) {
                R.id.shape_bar_h -> 2
                R.id.shape_bar_v -> 3
                R.id.shape_text  -> 4
                else             -> 1
            }
            updateStyleVisibility(style)
            saveGaugeField(selectedGauge) { it.setGaugeStyle(style) }
        }

        // Needle style
        needleSelector.setOnCheckedChangeListener { _, checkedId ->
            val style = when (checkedId) {
                R.id.needle_line     -> 1
                R.id.needle_triangle -> 2
                R.id.needle_image    -> 3
                else                 -> 0
            }
            updateNeedleImageVisibility(style)
            if (style == 3 && view.findViewById<Button>(R.id.btn_needle_image).text == "Select Needle Image") {
                // Auto-open the picker when Image is first selected with no image chosen
                pickImage("Select Needle Image", R.array.needleImages, R.array.needleEntries, R.array.needleValues) { value, label ->
                    btnNeedleImg.text = label
                    saveGaugeField(selectedGauge) { it.setCustomNeedle(value) }
                }
            }
            saveGaugeField(selectedGauge) { it.setNeedleStyle(style) }
        }

        // Background style
        bgStyleSelector.setOnCheckedChangeListener { _, checkedId ->
            val style = when (checkedId) {
                R.id.bg_none   -> 1
                R.id.bg_filled -> 2
                else           -> 0
            }
            saveGaugeField(selectedGauge) { it.setBackgroundStyle(style) }
        }

        // Needle image picker (visible only when needle_image is selected)
        btnNeedleImg.setOnClickListener {
            pickImage("Select Needle Image", R.array.needleImages, R.array.needleEntries, R.array.needleValues) { value, label ->
                btnNeedleImg.text = label
                saveGaugeField(selectedGauge) { it.setCustomNeedle(value) }
            }
        }

        // Dial background image picker (visible only when needle_image is selected)
        btnDialBgImg.setOnClickListener {
            pickImage("Select Dial Background", R.array.dialBgImages, R.array.dialBgEntries, R.array.dialBgValues) { value, label ->
                btnDialBgImg.text = label
                saveGaugeField(selectedGauge) { it.setCustomDialBackground(value) }
            }
        }

        // Gauge icon picker
        btnGaugeIcon.setOnClickListener {
            pickImage("Select Icon", R.array.icons, R.array.iconDesc, R.array.icon_values) { value, label ->
                btnGaugeIcon.text = label
                saveGaugeField(selectedGauge) { it.setIcon(value) }
            }
        }

        // Color pickers — per-gauge
        btnBg.setOnClickListener {
            pickColor("Background Color", currentBgColor) { color ->
                currentBgColor = color
                btnBg.setBackgroundColor(color)
                saveGaugeField(selectedGauge) { it.setCustomBgColor(color) }
            }
        }
        btnAccent.setOnClickListener {
            pickColor("Accent Color", currentAccentColor) { color ->
                currentAccentColor = color
                btnAccent.setBackgroundColor(color)
                saveGaugeField(selectedGauge) { it.setCustomAccentColor(color) }
            }
        }
        btnNeedle.setOnClickListener {
            pickColor("Needle Color", currentNeedleColor) { color ->
                currentNeedleColor = color
                btnNeedle.setBackgroundColor(color)
                saveGaugeField(selectedGauge) { it.setCustomNeedleColor(color) }
            }
        }
        btnRedline.setOnClickListener {
            pickColor("Redline Color", currentRedlineColor) { color ->
                currentRedlineColor = color
                btnRedline.setBackgroundColor(color)
                saveGaugeField(selectedGauge) { it.setCustomRedlineColor(color) }
            }
        }
    }

    private fun pickImage(
        title: String,
        imagesArrayRes: Int,
        entriesArrayRes: Int,
        valuesArrayRes: Int,
        onPicked: (value: String, label: String) -> Unit
    ) {
        val context = requireContext()
        val images = context.resources.obtainTypedArray(imagesArrayRes)
        val entries = context.resources.getStringArray(entriesArrayRes)
        val values = context.resources.getStringArray(valuesArrayRes)

        val dp = context.resources.displayMetrics.density
        val thumbSize = (56 * dp).toInt()
        val padding = (8 * dp).toInt()

        val scrollView = android.widget.ScrollView(context)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }
        scrollView.addView(container)

        entries.forEachIndexed { i, label ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(padding, padding, padding, padding)
            }
            val thumb = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(thumbSize, thumbSize).apply { marginEnd = padding }
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                try { setImageResource(images.getResourceId(i, 0)) } catch (_: Exception) {}
            }
            val lbl = TextView(context).apply {
                text = label
                setTextColor(Color.WHITE)
                textSize = 14f
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            }
            row.addView(thumb)
            row.addView(lbl)
            container.addView(row)
        }
        images.recycle()

        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setView(scrollView)
            .setNegativeButton(android.R.string.cancel) { d, _ -> d.dismiss() }
            .create()

        entries.forEachIndexed { i, label ->
            val row = container.getChildAt(i) as? LinearLayout
            row?.setOnClickListener {
                onPicked(values[i], label)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun attachDragListenerToGauge(gaugeIndex: Int) {
        val dashFrag = childFragmentManager.findFragmentById(R.id.preview_container) as? DashboardFragment
            ?: return
        val gaugeViews = dashFrag.gaugeViews

        gaugeViews.forEach { gv -> gv?.setOnTouchListener(null) }

        val gv = gaugeViews[gaugeIndex] ?: return
        val dashRoot = dashFrag.rootView

        var dragStartRawX = 0f
        var dragStartRawY = 0f
        var viewStartTransX = 0f
        var viewStartTransY = 0f

        gv.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragStartRawX = event.rawX
                    dragStartRawY = event.rawY
                    viewStartTransX = v.translationX
                    viewStartTransY = v.translationY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    v.translationX = viewStartTransX + (event.rawX - dragStartRawX)
                    v.translationY = viewStartTransY + (event.rawY - dragStartRawY)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val rootW = dashRoot.width.toFloat()
                    val rootH = dashRoot.height.toFloat()
                    if (rootW > 0 && rootH > 0) {
                        val rootLoc = IntArray(2)
                        dashRoot.getLocationOnScreen(rootLoc)
                        val gvLoc = IntArray(2)
                        v.getLocationOnScreen(gvLoc)

                        val centerX = (gvLoc[0] - rootLoc[0] + v.width / 2f)
                        val centerY = (gvLoc[1] - rootLoc[1] + v.height / 2f)

                        val fracX = (centerX / rootW).coerceIn(0f, 1f)
                        val fracY = (centerY / rootH).coerceIn(0f, 1f)

                        saveGaugeField(gaugeIndex) {
                            it.setGaugePosX(fracX).setGaugePosY(fracY)
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
                setOnClickListener { applyPreset(preset) }
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
        saveGaugeField(selectedGauge) {
            it.setCustomBgColor(preset.bg)
              .setCustomAccentColor(preset.accent)
              .setCustomNeedleColor(preset.needle)
              .setCustomRedlineColor(preset.redline)
        }
        currentBgColor     = preset.bg
        currentAccentColor = preset.accent
        currentNeedleColor = preset.needle
        currentRedlineColor= preset.redline
        view?.findViewById<Button>(R.id.btn_bg_color)?.setBackgroundColor(preset.bg)
        view?.findViewById<Button>(R.id.btn_accent_color)?.setBackgroundColor(preset.accent)
        view?.findViewById<Button>(R.id.btn_needle_color)?.setBackgroundColor(preset.needle)
        view?.findViewById<Button>(R.id.btn_redline_color)?.setBackgroundColor(preset.redline)
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
