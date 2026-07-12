package com.aatorque.prefs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aatorque.datastore.UserPreference
import com.aatorque.datastore.Display
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class DashboardEditorComposeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dbIndex = requireArguments().getCharSequence("prefix")?.split("_")?.last()?.toInt() ?: 0

        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme(
                    colorScheme = darkColorScheme(
                        primary = Color(0xFF00BFFF),
                        background = Color(0xFF121212),
                        surface = Color(0xFF1E1E1E)
                    )
                ) {
                    DashboardEditorScreen(dbIndex = dbIndex, onSave = { updatedScreen ->
                        lifecycleScope.launch {
                            requireContext().dataStore.updateData { prefs ->
                                val currentScreens = prefs.screensList.toMutableList()
                                if (dbIndex < currentScreens.size) {
                                    currentScreens[dbIndex] = updatedScreen
                                }
                                prefs.toBuilder().clearScreens().addAllScreens(currentScreens).build()
                            }
                            requireActivity().supportFragmentManager.popBackStack()
                        }
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardEditorScreen(dbIndex: Int, onSave: (com.aatorque.datastore.Screen) -> Unit) {
    // In a real app, read this from DataStore using Flow
    var screenData by remember { mutableStateOf(com.aatorque.datastore.Screen.getDefaultInstance()) }
    var displays by remember { mutableStateOf(listOf<Display>()) }

    // Sidebar items
    val availableGauges = listOf("Speed", "RPM", "Boost", "Coolant Temp", "Intake Temp")

    Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Sidebar Menu
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(200.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Text(
                text = "Gauges",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableGauges) { gaugeName ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Add a new display
                                val newDisplay = Display.newBuilder()
                                    .setLabel(gaugeName)
                                    .setGaugePosX(0.5f)
                                    .setGaugePosY(0.5f)
                                    .setGaugeSize(1.0f)
                                    .build()
                                displays = displays + newDisplay
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(
                            text = gaugeName,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    val updated = screenData.toBuilder().clearDisplays().addAllDisplays(displays).build()
                    onSave(updated)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }

        // Canvas Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .background(Color.Black)
        ) {
            displays.forEachIndexed { index, display ->
                DraggableGauge(
                    display = display,
                    onPositionChanged = { newX, newY ->
                        val mutableList = displays.toMutableList()
                        mutableList[index] = display.toBuilder()
                            .setGaugePosX(newX)
                            .setGaugePosY(newY)
                            .build()
                        displays = mutableList
                    }
                )
            }
        }
    }
}

@Composable
fun DraggableGauge(
    display: Display,
    onPositionChanged: (Float, Float) -> Unit
) {
    var offsetX by remember { mutableStateOf(display.gaugePosX * 800f) } // Naive mapping
    var offsetY by remember { mutableStateOf(display.gaugePosY * 400f) } 

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                    onPositionChanged(offsetX / 800f, offsetY / 400f)
                }
            }
            .size(120.dp)
            .background(Color.DarkGray, shape = RoundedCornerShape(60.dp))
            .border(2.dp, Color.Cyan, shape = RoundedCornerShape(60.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = display.label,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}
