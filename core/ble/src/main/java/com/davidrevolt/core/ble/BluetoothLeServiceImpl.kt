package com.davidrevolt.core.ble

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission

import com.davidrevolt.core.ble.manger.BluetoothLeConnect
import com.davidrevolt.core.ble.manger.BluetoothLeScan
import com.davidrevolt.core.ble.model.CustomGattService
import com.davidrevolt.core.ble.model.CustomScanResult
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class BluetoothLeServiceImpl @Inject constructor(
    private val bluetoothLeScan: BluetoothLeScan,
    private val bluetoothLeConnect: BluetoothLeConnect
) :
    BluetoothLeService {

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN])
    override fun startBluetoothLeScan() =
        bluetoothLeScan.startBluetoothLeScan()


    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN])
    override fun stopBluetoothLeScan() = bluetoothLeScan.stopBluetoothLeScan()

    override fun isScanning(): Flow<Boolean> = bluetoothLeScan.isScanning()
    override fun getScanResults(): Flow<List<CustomScanResult>> =
        bluetoothLeScan.getScanResults()


    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    override fun connectToDeviceGatt(deviceAddress: String) {
        // Always stop BLE scan before connecting to a BLE device.
        bluetoothLeScan.stopBluetoothLeScan()
        bluetoothLeConnect.connectToDeviceGatt(deviceAddress = deviceAddress)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun disconnectFromGatt() =
        bluetoothLeConnect.disconnectFromGatt()

    override fun getConnectionState(): Flow<Int> =
        bluetoothLeConnect.getConnectionState()

    override fun getDeviceServices(): Flow<List<CustomGattService>> =
        bluetoothLeConnect.getDeviceServices()


    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT])
    override fun readCharacteristic(characteristicUUID: UUID) =
        bluetoothLeConnect.readCharacteristic(characteristicUUID = characteristicUUID)

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT])
    override fun writeCharacteristic(characteristicUUID: UUID, value: ByteArray) =
        bluetoothLeConnect.writeCharacteristic(
            characteristicUUID = characteristicUUID,
            value = value
        )

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT])
    override fun readDescriptor(characteristicUUID: UUID, descriptorUUID: UUID) =
        bluetoothLeConnect.readDescriptor(
            characteristicUUID = characteristicUUID,
            descriptorUUID = descriptorUUID
        )


    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT])
    override fun writeDescriptor(characteristicUUID: UUID, descriptorUUID: UUID, value: ByteArray) =
        bluetoothLeConnect.writeDescriptor(
            characteristicUUID = characteristicUUID,
            descriptorUUID = descriptorUUID,
            value = value
        )

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT])
    override fun enableCharacteristicNotifications(characteristicUUID: UUID) =
        bluetoothLeConnect.enableCharacteristicNotifications(characteristicUUID = characteristicUUID)

}