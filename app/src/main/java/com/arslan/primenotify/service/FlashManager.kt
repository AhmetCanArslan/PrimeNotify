package com.arslan.primenotify.service

import android.content.Context
import android.hardware.camera2.CameraManager
import com.arslan.primenotify.data.FlashPattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FlashManager(context: Context) {
    
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val cameraId: String? = try {
        cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    } catch (e: Exception) {
        null
    }
    
    private var flashJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    
    fun executePattern(pattern: FlashPattern) {
        if (cameraId == null) return
        
        flashJob?.cancel()
        flashJob = scope.launch {
            try {
                when (pattern) {
                    FlashPattern.HEARTBEAT -> performHeartbeat()
                    FlashPattern.PING_PONG -> performPingPong()
                }
            } catch (e: Exception) {
                // Ignore interruption exceptions
            } finally {
                turnOffFlash()
            }
        }
    }
    
    private suspend fun performHeartbeat() {
        repeat(3) { // 3 cycles
            turnOnFlash()
            delay(100) // sharp pulse
            turnOffFlash()
            delay(150) // gap
            turnOnFlash()
            delay(100) // sharp pulse
            turnOffFlash()
            
            delay(700) // wait before next beat
        }
    }
    
    private suspend fun performPingPong() {
        repeat(4) { // 4 volleys
            turnOnFlash()
            delay(200)
            turnOffFlash()
            delay(100)
            
            turnOnFlash()
            delay(50)
            turnOffFlash()
            delay(50)
            
            turnOnFlash()
            delay(50)
            turnOffFlash()
            delay(400) // break 
        }
    }
    
    private fun turnOnFlash() {
        cameraId?.let { 
            try {
                cameraManager.setTorchMode(it, true)
            } catch (e: Exception) { /* IGNORE */ }
        }
    }
    
    private fun turnOffFlash() {
        cameraId?.let { 
            try {
                cameraManager.setTorchMode(it, false)
            } catch (e: Exception) { /* IGNORE */ }
        }
    }
    
    fun stop() {
        flashJob?.cancel()
        turnOffFlash()
    }
}
