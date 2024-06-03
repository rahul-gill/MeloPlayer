package meloplayer.app.playback.utils

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat

class BluetoothConnectedEventHandler(private val context: Context, onDeviceConnected: () -> Unit) {

    private var isRegistered = false

    private val bluetoothConnectedIntentFilter = IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED)

    private val bluetoothReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action != null) {

                if (BluetoothDevice.ACTION_ACL_CONNECTED == action) {
                    val device = IntentCompat.getParcelableExtra(
                        intent,
                        BluetoothDevice.EXTRA_DEVICE,
                        BluetoothDevice::class.java
                    )
                    if (device != null) {
                        onDeviceConnected()
                    }
                }
            }
        }
    }


    val isBluetoothConnected: Boolean
        get() {
            val audioManager =
                ContextCompat.getSystemService(context, AudioManager::class.java)!!
            val audioDeviceInfos =
                audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

            val allBluetoothDeviceTypesSet: Set<Int> = getAllBluetoothDeviceTypes()
            for (audioDeviceInfo in audioDeviceInfos) {
                if (allBluetoothDeviceTypesSet.contains(audioDeviceInfo.type)) {
                    return true
                }
            }
            return false
        }

    fun register() {

        if (isRegistered) {
            return
        }
        ContextCompat.registerReceiver(
            context,
            bluetoothReceiver,
            bluetoothConnectedIntentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
        isRegistered = true
    }

    fun unregister() {
        if (isRegistered) {
            context.unregisterReceiver(bluetoothReceiver)
            isRegistered = false
        }
    }


    private fun getAllBluetoothDeviceTypes(): Set<Int> {
        val allBluetoothDeviceTypes = mutableSetOf<Int>()
        allBluetoothDeviceTypes.add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
        allBluetoothDeviceTypes.add(AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            allBluetoothDeviceTypes.add(
                AudioDeviceInfo.TYPE_BLE_HEADSET
            )
            allBluetoothDeviceTypes.add(
                AudioDeviceInfo.TYPE_BLE_SPEAKER
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            allBluetoothDeviceTypes.add(AudioDeviceInfo.TYPE_BLE_BROADCAST)
        }

        return allBluetoothDeviceTypes.toSet()
    }
}
