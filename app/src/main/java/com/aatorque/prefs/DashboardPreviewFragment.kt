package com.aatorque.prefs

import android.app.AlertDialog
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.aatorque.stats.DashboardFragment
import com.aatorque.stats.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import com.rarepebble.colorpicker.ColorPickerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt


class DashboardPreviewFragment : Fragment() {

    private var selectedGauge = 0
    private var isLoadingSettings = false

    private var currentBgColor = 0
    private var currentAccentColor = 0
    private var currentNeedleColor = 0
    private var currentRedlineColor = 0

    // View refs needed across helper methods
    private var circleBgColor: ImageView? = null
    private var circleAccentColor: ImageView? = null
    private var circleNeedleColor: ImageView? = null
    private var circleRedlineColor: ImageView? = null
    private var switchApplyAll: SwitchMaterial? = null

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
        forceRotate(true)
        return inflater.inflate(R.layout.fragment_dashboard_preview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ensure theme is switched to Custom asynchronously
        lifecycleScope.launch {
            val prefs = requireContext().dataStore.data.first()
            if (prefs.selectedTheme != "Custom") {
                requireContext().dataStore.updateData {
                    it.toBuilder().setSelectedTheme("Custom").build()
                }
            }
        }

        // ── View references ──
        val btnG1         = view.findViewById<Button>(R.id.btn_gauge_1)
        val btnG2         = view.findViewById<Button>(R.id.btn_gauge_2)
        val btnG3         = view.findViewById<Button>(R.id.btn_gauge_3)
        val btnAdd        = view.findViewById<Button>(R.id.btn_add_gauge)
        val btnRemove     = view.findViewById<Button>(R.id.btn_remove_gauge)

        val seekRotation  = view.findViewById<SeekBar>(R.id.seekbar_rotation)
        val textRotation  = view.findViewById<TextView>(R.id.text_rotation_value)
        val switchFlip    = view.findViewById<SwitchMaterial>(R.id.switch_flip_sweep)
        val seekSize      = view.findViewById<SeekBar>(R.id.seekbar_size)
        val textSizeValue = view.findViewById<TextView>(R.id.text_size_value)
        val seekTitleSize = view.findViewById<SeekBar>(R.id.seekbar_title_size)
        val textTitleSize = view.findViewById<TextView>(R.id.text_title_size_value)

        val shapeSelector  = view.findViewById<ChipGroup>(R.id.gauge_shape_selector)
        val needleSelector = view.findViewById<ChipGroup>(R.id.needle_style_selector)
        val bgStyleSelector= view.findViewById<ChipGroup>(R.id.bg_style_selector)
        val labelNeedle    = view.findViewById<TextView>(R.id.label_needle_style)
        val labelBgStyle   = view.findViewById<TextView>(R.id.label_bg_style)

        val btnNeedleImg   = view.findViewById<Button>(R.id.btn_needle_image)
        val btnDialBgImg   = view.findViewById<Button>(R.id.btn_dial_bg_image)
        val btnGaugeIcon   = view.findViewById<Button>(R.id.btn_gauge_icon)
        val presetRow      = view.findViewById<LinearLayout>(R.id.preset_row)

        val swatchBg       = view.findViewById<LinearLayout>(R.id.swatch_bg)
        val swatchAccent   = view.findViewById<LinearLayout>(R.id.swatch_accent)
        val swatchNeedle   = view.findViewById<LinearLayout>(R.id.swatch_needle)
        val swatchRedline  = view.findViewById<LinearLayout>(R.id.swatch_redline)

        circleBgColor      = view.findViewById(R.id.circle_bg_color)
        circleAccentColor  = view.findViewById(R.id.circle_accent_color)
        circleNeedleColor  = view.findViewById(R.id.circle_needle_color)
        circleRedlineColor = view.findViewById(R.id.circle_redline_color)
        switchApplyAll     = view.findViewById(R.id.switch_apply_all)

        val textScreenLabel  = view.findViewById<TextView>(R.id.text_screen_label)
        val editScreenTitle  = view.findViewById<EditText>(R.id.edit_screen_title)
        val btnPrevScreen    = view.findViewById<ImageButton>(R.id.btn_prev_screen)
        val btnNextScreen    = view.findViewById<ImageButton>(R.id.btn_next_screen)
        val btnFullscreen    = view.findViewById<ImageButton>(R.id.btn_fullscreen)
        val btnBackgroundPicker = view.findViewById<Button>(R.id.btn_background_picker)
        val btnFontPicker    = view.findViewById<Button>(R.id.btn_font_picker)

        val gaugeButtons = listOf(btnG1, btnG2, btnG3)

        // ── Helpers ──

        fun isTextStyle(gaugeStyle: Int) = gaugeStyle == 4

        fun updateNeedleImageVisibility(needleStyle: Int) {
            val vis = if (needleStyle == 3) View.VISIBLE else View.GONE
            btnNeedleImg.visibility = vis
            btnDialBgImg.visibility = vis
        }

        fun updateStyleVisibility(gaugeStyle: Int) {
            val needleVis = if (isTextStyle(gaugeStyle)) View.GONE else View.VISIBLE
            labelNeedle.visibility = needleVis
            needleSelector.visibility = needleVis
            labelBgStyle.visibility = needleVis
            bgStyleSelector.visibility = needleVis
            if (isTextStyle(gaugeStyle)) {
                btnNeedleImg.visibility = View.GONE
                btnDialBgImg.visibility = View.GONE
            }
        }

        fun refreshGaugeButtons() {
            lifecycleScope.launch {
                val prefs = requireContext().dataStore.data.first()
                val si = abs(prefs.currentScreen) % prefs.screensCount
                val screen = prefs.getScreens(si)
                var enabledCount = 0
                var disabledCount = 0
                gaugeButtons.forEachIndexed { i, btn ->
                    val disabled = if (i < screen.gaugesCount) screen.getGauges(i).disabled else true
                    if (disabled) disabledCount++ else enabledCount++
                    btn.alpha = when {
                        i == selectedGauge -> 1f
                        disabled           -> 0.3f
                        else               -> 0.6f
                    }
                }
                btnAdd.isEnabled = disabledCount > 0
                btnRemove.isEnabled = enabledCount > 1
            }
        }

        fun loadGaugeSettings(index: Int) {
            lifecycleScope.launch {
                val prefs = requireContext().dataStore.data.first()
                val si = abs(prefs.currentScreen) % prefs.screensCount
                val display = prefs.getScreens(si).getGauges(index)

                isLoadingSettings = true

                val rotDeg = display.gaugeRotation.roundToInt()
                seekRotation.progress = rotDeg
                textRotation.text = "${rotDeg}°"
                switchFlip.isChecked = display.reverseSweep

                val sizeProgress = if (display.gaugeSize == 0f) 50
                    else ((display.gaugeSize - 0.5f) * 100f).toInt().coerceIn(0, 150)
                seekSize.progress = sizeProgress
                textSizeValue.text = "${(0.5f + sizeProgress / 100f).times(100f).toInt()}%"

                val titleSp = if (display.titleFontSize == 0) 12 else display.titleFontSize
                seekTitleSize.progress = (titleSp - 8).coerceIn(0, 16)
                textTitleSize.text = "${titleSp}sp"

                when (display.gaugeStyle) {
                    2    -> view.findViewById<Chip>(R.id.chip_shape_bar_h).isChecked = true
                    3    -> view.findViewById<Chip>(R.id.chip_shape_bar_v).isChecked = true
                    4    -> view.findViewById<Chip>(R.id.chip_shape_text).isChecked = true
                    else -> view.findViewById<Chip>(R.id.chip_shape_circular).isChecked = true
                }

                when (display.needleStyle) {
                    1    -> view.findViewById<Chip>(R.id.chip_needle_line).isChecked = true
                    2    -> view.findViewById<Chip>(R.id.chip_needle_triangle).isChecked = true
                    3    -> view.findViewById<Chip>(R.id.chip_needle_image).isChecked = true
                    else -> view.findViewById<Chip>(R.id.chip_needle_dot).isChecked = true
                }

                when (display.backgroundStyle) {
                    1    -> view.findViewById<Chip>(R.id.chip_bg_none).isChecked = true
                    2    -> view.findViewById<Chip>(R.id.chip_bg_filled).isChecked = true
                    else -> view.findViewById<Chip>(R.id.chip_bg_arc).isChecked = true
                }

                isLoadingSettings = false

                updateStyleVisibility(display.gaugeStyle)
                updateNeedleImageVisibility(display.needleStyle)

                btnNeedleImg.text = display.customNeedle.takeIf { it.isNotEmpty() } ?: "Select Needle Image"
                btnDialBgImg.text = display.customDialBackground.takeIf { it.isNotEmpty() } ?: "Select Dial Background"
                btnGaugeIcon.text = display.icon.takeIf { it.isNotEmpty() && it != "ic_none" } ?: "Select Icon"

                currentBgColor      = if (display.customBgColor     != 0) display.customBgColor     else prefs.customBackgroundColor
                currentAccentColor  = if (display.customAccentColor != 0) display.customAccentColor else prefs.customAccentColor
                currentNeedleColor  = if (display.customNeedleColor != 0) display.customNeedleColor else prefs.customNeedleColor
                currentRedlineColor = if (display.customRedlineColor != 0) display.customRedlineColor else prefs.customRedlineColor
                updateColorSwatches()
            }
        }

        fun selectGauge(index: Int) {
            selectedGauge = index
            gaugeButtons.forEachIndexed { i, btn -> btn.alpha = if (i == index) 1f else 0.5f }
            loadGaugeSettings(index)
            attachDragListenerToGauge(index)
            refreshGaugeButtons()
        }

        // ── Screen bar ──

        fun refreshScreenBar() {
            lifecycleScope.launch {
                val prefs = requireContext().dataStore.data.first()
                val si = abs(prefs.currentScreen) % prefs.screensCount
                val screen = prefs.getScreens(si)
                textScreenLabel.text = "Screen ${si + 1} of ${prefs.screensCount}"
                editScreenTitle.setText(screen.title)
            }
        }

        fun navigateScreen(delta: Int) {
            lifecycleScope.launch {
                requireContext().dataStore.updateData { prefs ->
                    val newIndex = (abs(prefs.currentScreen) + delta + prefs.screensCount) % prefs.screensCount
                    prefs.toBuilder().setCurrentScreen(newIndex).build()
                }
                refreshScreenBar()
                loadGaugeSettings(selectedGauge)
                refreshGaugeButtons()
            }
        }

        btnPrevScreen.setOnClickListener { navigateScreen(-1) }
        btnNextScreen.setOnClickListener { navigateScreen(+1) }

        editScreenTitle.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                saveScreenTitle(editScreenTitle.text.toString())
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(editScreenTitle.windowToken, 0)
                editScreenTitle.clearFocus()
                true
            } else false
        }

        editScreenTitle.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) saveScreenTitle(editScreenTitle.text.toString())
        }

        // ── Fullscreen preview button ──

        btnFullscreen.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.settings_fragment, FullscreenPreviewFragment(), "fullscreen_preview")
                .addToBackStack(null)
                .commit()
        }

        // ── Dashboard card: background + font pickers ──

        btnBackgroundPicker.setOnClickListener {
            pickImage("Select Background", R.array.thumbs_backgrounds, R.array.backgroundstrings, R.array.backgrounds) { value, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    requireContext().dataStore.updateData { it.toBuilder().setSelectedBackground(value).build() }
                }
            }
        }

        btnFontPicker.setOnClickListener {
            pickImage("Select Font", R.array.FontThumbs, R.array.fontEntries, R.array.fontValues) { value, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    requireContext().dataStore.updateData { it.toBuilder().setSelectedFont(value).build() }
                }
            }
        }

        // ── Gauge selector ──

        btnG1.setOnClickListener { selectGauge(0) }
        btnG2.setOnClickListener { selectGauge(1) }
        btnG3.setOnClickListener { selectGauge(2) }

        btnAdd.setOnClickListener {
            lifecycleScope.launch {
                val prefs = requireContext().dataStore.data.first()
                val si = abs(prefs.currentScreen) % prefs.screensCount
                val screen = prefs.getScreens(si)
                val disabledIndex = (0 until screen.gaugesCount).firstOrNull { screen.getGauges(it).disabled }
                if (disabledIndex != null) {
                    requireContext().dataStore.updateData { p ->
                        val idx = abs(p.currentScreen) % p.screensCount
                        val s = p.getScreens(idx)
                        val updated = s.getGauges(disabledIndex).toBuilder().setDisabled(false).build()
                        p.toBuilder().setScreens(idx, s.toBuilder().setGauges(disabledIndex, updated).build()).build()
                    }
                    selectGauge(disabledIndex)
                }
            }
        }

        btnRemove.setOnClickListener {
            lifecycleScope.launch {
                val prefs = requireContext().dataStore.data.first()
                val si = abs(prefs.currentScreen) % prefs.screensCount
                val screen = prefs.getScreens(si)
                val enabledCount = (0 until screen.gaugesCount).count { !screen.getGauges(it).disabled }
                if (enabledCount <= 1) return@launch
                val nextEnabled = (0 until screen.gaugesCount).firstOrNull {
                    it != selectedGauge && !screen.getGauges(it).disabled
                } ?: 0
                requireContext().dataStore.updateData { p ->
                    val idx = abs(p.currentScreen) % p.screensCount
                    val s = p.getScreens(idx)
                    val updated = s.getGauges(selectedGauge).toBuilder().setDisabled(true).build()
                    p.toBuilder().setScreens(idx, s.toBuilder().setGauges(selectedGauge, updated).build()).build()
                }
                selectGauge(nextEnabled)
            }
        }

        // ── Gauge Style chip group ──

        shapeSelector.setOnCheckedStateChangeListener { _, checkedIds ->
            if (isLoadingSettings) return@setOnCheckedStateChangeListener
            val style = when (checkedIds.firstOrNull()) {
                R.id.chip_shape_bar_h -> 2
                R.id.chip_shape_bar_v -> 3
                R.id.chip_shape_text  -> 4
                else                  -> 1
            }
            updateStyleVisibility(style)
            saveGaugeField(selectedGauge) { it.setGaugeStyle(style) }
        }

        // ── Rotation seekbar ──

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

        // ── Flip sweep toggle ──

        switchFlip.setOnCheckedChangeListener { _, checked ->
            if (!isLoadingSettings) saveGaugeField(selectedGauge) { it.setReverseSweep(checked) }
        }

        // ── Size seekbar (0–150 → 50%–200%) ──

        seekSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                textSizeValue.text = "${((0.5f + progress / 100f) * 100f).toInt()}%"
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                saveGaugeField(selectedGauge) { it.setGaugeSize(0.5f + sb.progress / 100f) }
            }
        })

        // ── Needle Style chip group ──

        needleSelector.setOnCheckedStateChangeListener { _, checkedIds ->
            if (isLoadingSettings) return@setOnCheckedStateChangeListener
            val style = when (checkedIds.firstOrNull()) {
                R.id.chip_needle_line     -> 1
                R.id.chip_needle_triangle -> 2
                R.id.chip_needle_image    -> 3
                else                      -> 0
            }
            updateNeedleImageVisibility(style)
            if (style == 3 && btnNeedleImg.text == "Select Needle Image") {
                pickImage("Select Needle Image", R.array.needleImages, R.array.needleEntries, R.array.needleValues) { value, label ->
                    btnNeedleImg.text = label
                    saveGaugeField(selectedGauge) { it.setCustomNeedle(value) }
                }
            }
            saveGaugeField(selectedGauge) { it.setNeedleStyle(style) }
        }

        // ── Background Style chip group ──

        bgStyleSelector.setOnCheckedStateChangeListener { _, checkedIds ->
            if (isLoadingSettings) return@setOnCheckedStateChangeListener
            val style = when (checkedIds.firstOrNull()) {
                R.id.chip_bg_none   -> 1
                R.id.chip_bg_filled -> 2
                else                -> 0
            }
            saveGaugeField(selectedGauge) { it.setBackgroundStyle(style) }
        }

        // ── Title Font Size seekbar ──

        seekTitleSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                textTitleSize.text = "${8 + progress}sp"
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                saveGaugeField(selectedGauge) { it.setTitleFontSize(8 + sb.progress) }
            }
        })

        // ── Image pickers ──

        btnNeedleImg.setOnClickListener {
            pickImage("Select Needle Image", R.array.needleImages, R.array.needleEntries, R.array.needleValues) { value, label ->
                btnNeedleImg.text = label
                saveGaugeField(selectedGauge) { it.setCustomNeedle(value) }
            }
        }

        btnDialBgImg.setOnClickListener {
            pickImage("Select Dial Background", R.array.dialBgImages, R.array.dialBgEntries, R.array.dialBgValues) { value, label ->
                btnDialBgImg.text = label
                saveGaugeField(selectedGauge) { it.setCustomDialBackground(value) }
            }
        }

        btnGaugeIcon.setOnClickListener {
            pickImage("Select Icon", R.array.icons, R.array.iconDesc, R.array.icon_values) { value, label ->
                btnGaugeIcon.text = label
                saveGaugeField(selectedGauge) { it.setIcon(value) }
            }
        }

        // ── Color presets ──

        buildPresetRow(presetRow)

        // ── Color swatch pickers ──

        swatchBg.setOnClickListener {
            pickColor("Background Color", currentBgColor) { color ->
                currentBgColor = color
                updateColorSwatches()
                val indices = if (switchApplyAll?.isChecked == true) listOf(0, 1, 2) else listOf(selectedGauge)
                indices.forEach { i -> saveGaugeField(i) { it.setCustomBgColor(color) } }
            }
        }

        swatchAccent.setOnClickListener {
            pickColor("Accent Color", currentAccentColor) { color ->
                currentAccentColor = color
                updateColorSwatches()
                val indices = if (switchApplyAll?.isChecked == true) listOf(0, 1, 2) else listOf(selectedGauge)
                indices.forEach { i -> saveGaugeField(i) { it.setCustomAccentColor(color) } }
            }
        }

        swatchNeedle.setOnClickListener {
            pickColor("Needle Color", currentNeedleColor) { color ->
                currentNeedleColor = color
                updateColorSwatches()
                val indices = if (switchApplyAll?.isChecked == true) listOf(0, 1, 2) else listOf(selectedGauge)
                indices.forEach { i -> saveGaugeField(i) { it.setCustomNeedleColor(color) } }
            }
        }

        swatchRedline.setOnClickListener {
            pickColor("Redline Color", currentRedlineColor) { color ->
                currentRedlineColor = color
                updateColorSwatches()
                val indices = if (switchApplyAll?.isChecked == true) listOf(0, 1, 2) else listOf(selectedGauge)
                indices.forEach { i -> saveGaugeField(i) { it.setCustomRedlineColor(color) } }
            }
        }

        // ── Initial load ──

        refreshScreenBar()
        selectGauge(0)
    }

    private fun updateColorSwatches() {
        fun oval(color: Int) = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
        circleBgColor?.background      = oval(currentBgColor)
        circleAccentColor?.background  = oval(currentAccentColor)
        circleNeedleColor?.background  = oval(currentNeedleColor)
        circleRedlineColor?.background = oval(currentRedlineColor)
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
            (container.getChildAt(i) as? LinearLayout)?.setOnClickListener {
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
                        val fracX = ((gvLoc[0] - rootLoc[0] + v.width / 2f) / rootW).coerceIn(0f, 1f)
                        val fracY = ((gvLoc[1] - rootLoc[1] + v.height / 2f) / rootH).coerceIn(0f, 1f)
                        saveGaugeField(gaugeIndex) { it.setGaugePosX(fracX).setGaugePosY(fracY) }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun saveGaugeField(
        gaugeIndex: Int,
        update: (com.aatorque.datastore.Display.Builder) -> com.aatorque.datastore.Display.Builder
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            requireContext().dataStore.updateData { prefs ->
                val si = abs(prefs.currentScreen) % prefs.screensCount
                val screen = prefs.getScreens(si)
                val updatedDisplay = update(screen.getGauges(gaugeIndex).toBuilder()).build()
                prefs.toBuilder().setScreens(si, screen.toBuilder().setGauges(gaugeIndex, updatedDisplay).build()).build()
            }
        }
    }

    private fun saveScreenTitle(title: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            requireContext().dataStore.updateData { prefs ->
                val si = abs(prefs.currentScreen) % prefs.screensCount
                val screen = prefs.getScreens(si)
                prefs.toBuilder().setScreens(si, screen.toBuilder().setTitle(title).build()).build()
            }
        }
    }

    private fun buildPresetRow(presetRow: LinearLayout) {
        val dp = requireContext().resources.displayMetrics.density
        val swatchSize = (56 * dp).toInt()
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
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            }
            container.addView(swatch)
            container.addView(label)
            presetRow.addView(container)
        }
    }

    private fun applyPreset(preset: ColorPreset) {
        val indices = if (switchApplyAll?.isChecked == true) listOf(0, 1, 2) else listOf(selectedGauge)
        indices.forEach { i ->
            saveGaugeField(i) {
                it.setCustomBgColor(preset.bg)
                  .setCustomAccentColor(preset.accent)
                  .setCustomNeedleColor(preset.needle)
                  .setCustomRedlineColor(preset.redline)
            }
        }
        currentBgColor      = preset.bg
        currentAccentColor  = preset.accent
        currentNeedleColor  = preset.needle
        currentRedlineColor = preset.redline
        updateColorSwatches()
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
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(hideBars)
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onPause() {
        super.onPause()
        forceRotate(false)
        val window = requireActivity().window
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
