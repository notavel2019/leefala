package com.example.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.model.AudioRouteInfo
import com.example.model.AudioRouteKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors public Android audio output routes without forcing Bluetooth SCO / call mode.
 * Notifies immediately when headphones or Bluetooth audio disconnect (or becoming noisy).
 */
class AudioRouteMonitor(
    context: Context,
    private val onHeadsetDisconnected: () -> Unit,
) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _routeInfo = MutableStateFlow(AudioRouteInfo())
    val routeInfo: StateFlow<AudioRouteInfo> = _routeInfo.asStateFlow()

    private var wasHeadsetConnected = false
    private var isStarted = false

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            refreshRoute(triggerDisconnectIfLost = false)
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refreshRoute(triggerDisconnectIfLost = true)
        }
    }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                Log.i(TAG, "ACTION_AUDIO_BECOMING_NOISY recebido; pausando guia")
                refreshRoute(triggerDisconnectIfLost = false)
                onHeadsetDisconnected()
            }
        }
    }

    fun start() {
        if (isStarted) return
        isStarted = true
        refreshRoute(triggerDisconnectIfLost = false)
        try {
            audioManager.registerAudioDeviceCallback(deviceCallback, mainHandler)
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao registrar AudioDeviceCallback: ${e.javaClass.simpleName}")
        }
        try {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(noisyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                appContext.registerReceiver(noisyReceiver, filter)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao registrar receiver becoming noisy: ${e.javaClass.simpleName}")
        }
    }

    fun stop() {
        if (!isStarted) return
        isStarted = false
        try {
            audioManager.unregisterAudioDeviceCallback(deviceCallback)
        } catch (_: Exception) {
        }
        try {
            appContext.unregisterReceiver(noisyReceiver)
        } catch (_: Exception) {
        }
    }

    fun refreshRoute(triggerDisconnectIfLost: Boolean = false) {
        val detected = detectCurrentRoute()
        val previouslySafe = wasHeadsetConnected
        wasHeadsetConnected = detected.isHeadsetSafe
        _routeInfo.value = detected

        if (triggerDisconnectIfLost && previouslySafe && !detected.isHeadsetSafe) {
            Log.i(TAG, "Fone desconectado detectado via AudioDeviceCallback")
            onHeadsetDisconnected()
        }
    }

    private fun detectCurrentRoute(): AudioRouteInfo {
        return try {
            val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            if (outputs.isEmpty()) {
                return AudioRouteInfo(
                    kind = AudioRouteKind.UNCONFIRMED,
                    isHeadsetSafe = false,
                    description = "Rota de saída não confirmada pelo sistema",
                )
            }

            // Priority 1: Bluetooth A2DP or BLE Audio (media route — preferred, avoids SCO call mode)
            val btMedia = outputs.firstOrNull { device ->
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        (device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                            device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER))
            }
            if (btMedia != null) {
                val label = safeDeviceName(btMedia, "Fone Bluetooth")
                return AudioRouteInfo(
                    kind = AudioRouteKind.BLUETOOTH_MEDIA,
                    deviceName = label,
                    isHeadsetSafe = true,
                    description = "Fone Bluetooth (mídia): $label",
                )
            }

            // Priority 2: Wired / USB headphones
            val wired = outputs.firstOrNull { device ->
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                        (device.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                            device.type == AudioDeviceInfo.TYPE_USB_DEVICE))
            }
            if (wired != null) {
                val label = safeDeviceName(wired, "Fone com fio / USB")
                return AudioRouteInfo(
                    kind = AudioRouteKind.WIRED_HEADSET,
                    deviceName = label,
                    isHeadsetSafe = true,
                    description = "Fone conectado: $label",
                )
            }

            // Priority 3: Bluetooth SCO only (warn that media/A2DP wasn't detected)
            val btSco = outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
            if (btSco != null) {
                val label = safeDeviceName(btSco, "Bluetooth (perfil chamada)")
                return AudioRouteInfo(
                    kind = AudioRouteKind.BLUETOOTH_CALL_SCO,
                    deviceName = label,
                    isHeadsetSafe = true,
                    description = "Bluetooth detectado ($label) — rota de mídia não confirmada",
                )
            }

            // Priority 4: Built-in phone speaker
            val speaker = outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            if (speaker != null) {
                return AudioRouteInfo(
                    kind = AudioRouteKind.PHONE_SPEAKER,
                    deviceName = "Alto-falante do celular",
                    isHeadsetSafe = false,
                    description = "Sem fone detectado — saída no alto-falante do celular",
                )
            }

            AudioRouteInfo(
                kind = AudioRouteKind.UNCONFIRMED,
                isHeadsetSafe = false,
                description = "Rota de áudio não confirmada",
            )
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao consultar dispositivos de áudio: ${e.javaClass.simpleName}")
            AudioRouteInfo(
                kind = AudioRouteKind.UNCONFIRMED,
                isHeadsetSafe = false,
                description = "Rota de áudio não confirmada",
            )
        }
    }

    private fun safeDeviceName(device: AudioDeviceInfo, fallback: String): String {
        val raw = device.productName?.toString()?.trim().orEmpty()
        return if (raw.isNotEmpty() && !raw.equals("Android", ignoreCase = true)) raw else fallback
    }

    companion object {
        private const val TAG = "AudioRouteMonitor"
    }
}
