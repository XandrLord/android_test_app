package com.davidrevolt.core.ble

import com.davidrevolt.core.ble.model.CustomGattService
import com.davidrevolt.core.ble.model.CustomScanResult
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface BluetoothLeService{
    fun startBluetoothLeScan()
    fun stopBluetoothLeScan()
    fun isScanning(): Flow<Boolean>
    fun getScanResults(): Flow<List<CustomScanResult>>

    // Always stop BLE scan before connecting to a BLE device.
    fun connectToDeviceGatt(deviceAddress: String)
    fun disconnectFromGatt()

    fun getConnectionState(): Flow<Int>
    fun getDeviceServices(): Flow<List<CustomGattService>>
    fun readCharacteristic(characteristicUUID: UUID)
    fun writeCharacteristic(characteristicUUID: UUID, value: ByteArray)
    fun readDescriptor(characteristicUUID: UUID, descriptorUUID: UUID)
    fun writeDescriptor(characteristicUUID: UUID, descriptorUUID: UUID, value: ByteArray)
    fun enableCharacteristicNotifications(characteristicUUID: UUID)
}