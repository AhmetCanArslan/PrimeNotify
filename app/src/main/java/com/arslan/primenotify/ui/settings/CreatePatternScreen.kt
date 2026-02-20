package com.arslan.primenotify.ui.settings

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arslan.primenotify.data.CustomPattern
import com.arslan.primenotify.data.RulesManager
import com.arslan.primenotify.service.FlashManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePatternScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val rulesManager = remember { RulesManager(context) }
    val flashManager = remember { FlashManager(context) }
    
    var isRecording by remember { mutableStateOf(false) }
    var recordingTimeLeft by remember { mutableLongStateOf(5000L) }
    var showNameDialog by remember { mutableStateOf(false) }
    
    var events by remember { mutableStateOf(mutableListOf<Pair<Long, Boolean>>()) }
    var startTime by remember { mutableLongStateOf(0L) }
    var patternName by remember { mutableStateOf("") }
    
    var isFlashOn by remember { mutableStateOf(false) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            startTime = SystemClock.uptimeMillis()
            var currentEvents = mutableListOf<Pair<Long, Boolean>>()
            currentEvents.add(Pair(startTime, false))
            events = currentEvents
            
            var elapsed = 0L
            while (elapsed < 5000L) {
                delay(16)
                elapsed = SystemClock.uptimeMillis() - startTime
                recordingTimeLeft = maxOf(0L, 5000L - elapsed)
                if (elapsed >= 5000L) {
                    isRecording = false
                    val newEvents = ArrayList(events)
                    newEvents.add(Pair(SystemClock.uptimeMillis(), false))
                    events = newEvents
                    showNameDialog = true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Pattern") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isRecording) "Recording... ${recordingTimeLeft / 1000}.${(recordingTimeLeft % 1000) / 100}s" else "Press Start to Record (Max 5s)",
                style = MaterialTheme.typography.titleLarge
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(
                        color = if (isFlashOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
                    .pointerInput(isRecording) {
                        if (isRecording) {
                            awaitPointerEventScope {
                                while (true) {
                                    val down = awaitFirstDown()
                                    isFlashOn = true
                                    flashManager.turnOnFlash()
                                    val newEventsOn = ArrayList(events)
                                    newEventsOn.add(Pair(SystemClock.uptimeMillis(), true))
                                    events = newEventsOn
                                    
                                    val up = waitForUpOrCancellation()
                                    isFlashOn = false
                                    flashManager.turnOffFlash()
                                    val newEventsOff = ArrayList(events)
                                    newEventsOff.add(Pair(SystemClock.uptimeMillis(), false))
                                    events = newEventsOff
                                }
                            }
                        } else {
                            // reset
                            isFlashOn = false
                            flashManager.turnOffFlash()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRecording) "TAP/HOLD" else "FLASH",
                    color = if (isFlashOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (!isRecording) {
                    Button(
                        onClick = {
                            recordingTimeLeft = 5000L
                            isRecording = true
                        },
                        modifier = Modifier.fillMaxWidth(0.5f)
                    ) {
                        Text("Start")
                    }
                } else {
                    Button(
                        onClick = {
                            isRecording = false
                            val newEvents = ArrayList(events)
                            newEvents.add(Pair(SystemClock.uptimeMillis(), false))
                            events = newEvents
                            showNameDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(0.5f)
                    ) {
                        Text("Stop")
                    }
                }
            }
        }
    }
    
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Save Pattern") },
            text = {
                OutlinedTextField(
                    value = patternName,
                    onValueChange = { patternName = it },
                    label = { Text("Pattern Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (patternName.isNotBlank()) {
                            val intervals = mutableListOf<Long>()
                            var lastState = false
                            var lastTime = startTime
                            
                            for (event in events) {
                                if (event.first <= lastTime) continue
                                if (event.second != lastState) {
                                    intervals.add(event.first - lastTime)
                                    lastState = event.second
                                    lastTime = event.first
                                }
                            }
                            
                            // If ends on 'on', we should stop it (though events should always end with false)
                            
                            if (intervals.isEmpty()) {
                                intervals.add(100L) // fallback
                            }
                            
                            val customPattern = CustomPattern(
                                name = patternName.trim(),
                                intervals = intervals
                            )
                            rulesManager.saveCustomPattern(customPattern)
                            showNameDialog = false
                            onNavigateBack()
                        }
                    },
                    enabled = patternName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
