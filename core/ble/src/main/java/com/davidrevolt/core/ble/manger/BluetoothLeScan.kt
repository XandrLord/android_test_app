package com.davidrevolt.core.ble.manger

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.davidrevolt.core.ble.model.CustomScanResult
import com.davidrevolt.core.ble.model.modelmapper.asCustomScanResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/*
Online Guide: https://punchthrough.com/android-ble-guide/

* IMPORTANT: Make sure user have permission before using methods.
* IMPORTANT: Make sure user Device Bluetooth is enabled otherwise adapter.scanner is null.
* IMPORTANT: You should always stop your BLE scan before connecting to a BLE device.

* Rssi - signal strength of the advertising BluetoothLe device, measured in dBm.
Sorting scan results by descending order of signal strength is a good way
to find the peripheral closest to the Android device

* Warning: a device implementing Bluetooth 4.2’s LE Privacy feature will
randomize its public MAC address periodically

* Scan settings:
Most apps that are scanning in the foreground should use SCAN_MODE_BALANCED [30sec scan].
SCAN_MODE_LOW_LATENCY is recommended if the app will only be scanning for a brief period of time,
typically to find a very specific type of device.
SCAN_MODE_LOW_POWER is used for extremely long-duration scans, or for scans that take place in the background
 */

class BluetoothLeScan @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _bluetoothManager: BluetoothManager? =
        ContextCompat.getSystemService(context, BluetoothManager::class.java)
    private val _bluetoothAdapter: BluetoothAdapter? = _bluetoothManager?.adapter

    private var _isScanning = MutableStateFlow(false)
    private val _scanResult = MutableStateFlow<List<CustomScanResult>>(emptyList())
    fun getScanResults() = _scanResult.asStateFlow()
    fun isScanning() = _isScanning.asStateFlow()



    /*
    * onScanResult callback is flooded by ScanResults belonging to the same set of devices,
    * because signal strength (RSSI) readings constantly changing.
    */
    // Holds devices found through scanning - scanning keep returning the same device with dif rssi.
    private val _checkIfExists = mutableMapOf<String, Int>() // MAC to _scanResult List Ind

    private val _scanCallback = object : ScanCallback() {
        @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT])
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            _scanResult.update { currentList ->
                // Check if device with same address already exists
                val existingDeviceInd = _checkIfExists[result.device.address]
                if (existingDeviceInd != null) {
                    // Update RSSI of existing device
                    currentList.map {
                        if (it.address == result.device.address) it.copy(rssi = result.rssi)
                        else it
                    }
                } else {
                    // Add new device if it doesn't exist
                    Log.i(
                        TAG,
                        "Found BLE device! Name: ${result.device.name}, address: ${result.device.address}"
                    )
                    _checkIfExists[result.device.address] = _scanResult.value.size - 1
                    currentList + result.asCustomScanResult()
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            Log.e(TAG, "onScanFailed: code $errorCode")
            throw Exception("Scan Failed: code $errorCode")
        }
    }


    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN])
    fun startBluetoothLeScan() {
        if (_bluetoothAdapter != null && _bluetoothAdapter.isEnabled) {
            if (_isScanning.value)
                stopBluetoothLeScan()
            _scanResult.value = emptyList()
            _checkIfExists.clear()
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            val bleScanner = _bluetoothAdapter.bluetoothLeScanner
            bleScanner.startScan(null, scanSettings, _scanCallback)
            _isScanning.value = true
            Log.i(TAG, "Bluetooth scan start")
        } else {
            Log.e(TAG, "Device doesn't support Bluetooth or Bluetooth is disabled")
            throw Exception("Device doesn't support Bluetooth or Bluetooth is disabled")
        }
    }

    // Will throw exception if bluetooth isn't enable -> cause scanner will get null
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN])
    fun stopBluetoothLeScan() {
        if (_isScanning.value){
            _isScanning.value = false
            _bluetoothAdapter?.let { bluetoothAdapter ->
                val bleScanner = bluetoothAdapter.bluetoothLeScanner
                bleScanner.stopScan(_scanCallback)
                Log.i(TAG, "Bluetooth scanning stopped")
            }
        }
    }
    companion object {
         const val TAG = "BleLink-Log"
    }

}
