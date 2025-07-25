package com.davidrevolt.core.ble.manger


import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.ADDRESS_TYPE_PUBLIC
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.davidrevolt.core.ble.model.CustomGattService
import com.davidrevolt.core.ble.model.modelmapper.asCustomGattService
import com.davidrevolt.core.ble.util.containsProperty
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject


/*
* Online Guide: https://punchthrough.com/android-ble-guide/
* IMPORTANT: You should always stop your BLE scan before connecting to a BLE device.
*/

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class BluetoothLeConnect @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _bluetoothManager: BluetoothManager? =
        ContextCompat.getSystemService(context, BluetoothManager::class.java)
    private val _bluetoothAdapter: BluetoothAdapter? = _bluetoothManager?.adapter

    private var _bluetoothGatt: BluetoothGatt? = null
    private val _connectionState = MutableStateFlow(BluetoothProfile.STATE_DISCONNECTED)

    // Convert the Device BluetoothGattService and BluetoothGattCharacteristic to custom DATA class
    private val _deviceServices = MutableStateFlow<List<CustomGattService>>(emptyList())
    fun getConnectionState() = _connectionState.asStateFlow()
    fun getDeviceServices() = _deviceServices.asStateFlow()

    // Holds all the BluetoothGattCharacteristic of the device assuming all uuids are unique
    private val _availableCharacteristics = mutableMapOf<UUID, BluetoothGattCharacteristic>()


    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT])
    fun connectToDeviceGatt(deviceAddress: String) {
        _connectionState.value = BluetoothProfile.STATE_CONNECTING
        val device = _bluetoothAdapter?.getRemoteLeDevice(deviceAddress, ADDRESS_TYPE_PUBLIC)
        // device?.createBond() //Automatically created
        if (device?.bondState == BluetoothDevice.BOND_BONDED) {
            Log.i(TAG, "Device is bonded!")
        } else {
            Log.e(TAG, "Device is not bonded, attempting to bond...")
            device?.createBond()
        }
        _bluetoothGatt = device?.connectGatt(context, false, gattCallback, TRANSPORT_LE)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnectFromGatt() {
        _connectionState.value = BluetoothProfile.STATE_DISCONNECTING
        val deviceName = _bluetoothGatt?.device?.name ?: _bluetoothGatt?.device?.address
        Log.i(TAG, "Disconnected from $deviceName")
        _bluetoothGatt?.disconnect()
        _bluetoothGatt?.close()
        _connectionState.value = BluetoothProfile.STATE_DISCONNECTED
        _deviceServices.value = emptyList()
        _availableCharacteristics.clear()
        _bluetoothGatt = null
    }


    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT])
    fun readCharacteristic(characteristicUUID: UUID) {
        val characteristic = _availableCharacteristics[characteristicUUID]
        val successfulOp = _bluetoothGatt?.readCharacteristic(characteristic)
        if (successfulOp == false) {
            val msg = "Reading from Characteristic: $characteristicUUID went wrong"
            Log.i(TAG, msg)
            throw Exception(msg)
        }
    }


    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT])
    fun writeCharacteristic(characteristicUUID: UUID, value: ByteArray) {
        val characteristic = _availableCharacteristics[characteristicUUID]
        if (characteristic != null) {
            val writeType = when {
                characteristic.containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE) -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                characteristic.containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) -> BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                characteristic.containsProperty(BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) -> BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE
                else -> {
                    val msg = "Characteristic ${characteristic.uuid} cannot be written to"
                    Log.e(TAG, msg)
                    throw Exception(msg)
                }
            }
            val statusCode = _bluetoothGatt?.writeCharacteristic(characteristic, value, writeType)
            if (statusCode != 0) {
                val msg =
                    "Writing to characteristic: $characteristicUUID went wrong\nBluetoothStatusCode: $statusCode"
                Log.i(TAG, msg)
                throw Exception(msg)
            }
        } else {
            val msg = "Characteristic uuid $characteristicUUID not exists in device"
            Log.e(TAG, msg)
            throw Exception(msg)
        }
    }


    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT])
    fun readDescriptor(characteristicUUID: UUID, descriptorUUID: UUID) {
        val characteristic = _availableCharacteristics[characteristicUUID]
        if (characteristic != null) {
            val descriptor = characteristic.getDescriptor(descriptorUUID)
            val successfulOp = _bluetoothGatt?.readDescriptor(descriptor)
            if (successfulOp == false) {
                val msg = "Reading from descriptor: $descriptorUUID went wrong"
                Log.i(TAG, msg)
                throw Exception(msg)
            }
        } else {
            val msg = "Characteristic uuid $characteristicUUID not exists in device"
            Log.e(TAG, msg)
            throw Exception(msg)
        }

    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT])
    fun writeDescriptor(characteristicUUID: UUID, descriptorUUID: UUID, value: ByteArray) {
        val characteristic = _availableCharacteristics[characteristicUUID]
        if (characteristic != null) {
            val descriptor = characteristic.getDescriptor(descriptorUUID)
            val statusCode = _bluetoothGatt?.writeDescriptor(descriptor, value)
            if (statusCode != 0) {
                val msg =
                    "Writing to descriptor: $descriptorUUID went wrong\nBluetoothStatusCode: $statusCode"
                Log.i(TAG, msg)
                throw Exception(msg)
            }
        } else {
            val msg = "Characteristic uuid $characteristicUUID not exists in device"
            Log.e(TAG, msg)
            throw Exception(msg)
        }
    }

    // Enable notifications for a given characteristic.
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT])
    fun enableCharacteristicNotifications(characteristicUUID: UUID) {
        val characteristic = _availableCharacteristics[characteristicUUID]
        if (characteristic != null) {
            // setCharacteristicNotification enables or disables notifications locally on the Android device.
            // It does not communicate with the BLE peripheral.
            // Instead, it tells Android to listen for notifications if the peripheral decides to send them.
            val op = _bluetoothGatt?.setCharacteristicNotification(characteristic, true)
            if (op == false) {
                val msg = "Device unable to listen for notifications"
                Log.e(TAG, msg)
                throw Exception(msg)
            }
            val defaultDescriptor = "00002902-0000-1000-8000-00805f9b34fb"
            writeDescriptor(
                characteristicUUID,
                UUID.fromString(defaultDescriptor),
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            )
        } else {
            val msg = "Characteristic uuid $characteristicUUID not exists in device"
            Log.e(TAG, msg)
            throw Exception(msg)
        }
    }

    // This callback is gets triggered after using the public methods of this class
    private val gattCallback = object : BluetoothGattCallback() {
        @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT])
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceName = gatt.device.name ?: gatt.device.address
            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTING -> {
                        Log.i(TAG, "Connecting to $deviceName")
                        _connectionState.value = BluetoothProfile.STATE_CONNECTING
                    }

                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.i(TAG, "Successfully connected to $deviceName")
                        _connectionState.value = BluetoothProfile.STATE_CONNECTED
                        _bluetoothGatt?.discoverServices()
                    }

                    BluetoothProfile.STATE_DISCONNECTING -> {
                        Log.i(TAG, "disconnecting from $deviceName")
                        _connectionState.value = BluetoothProfile.STATE_DISCONNECTING
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.i(TAG, "Disconnected from $deviceName")
                        _connectionState.value = BluetoothProfile.STATE_DISCONNECTED
                    }
                }
            } else {
                Log.e(
                    TAG,
                    "Error [status: $status] encountered for $deviceName! Disconnecting..."
                )
                _connectionState.value = BluetoothProfile.STATE_DISCONNECTED
                gatt.close()
            }
        }


        // Find out device Services and Characteristics and convert them to MY Local models
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            _deviceServices.value = emptyList()
            _availableCharacteristics.clear()
            val deviceName = gatt.device.name ?: gatt.device.address
            Log.i(TAG, "Discovered ${gatt.services.size} services for $deviceName:")

            _deviceServices.value = gatt.services.map(BluetoothGattService::asCustomGattService)
            gatt.services.forEach { service ->
                // Fill availableCharacteristics map[UUID,Characteristic]
                service.characteristics.forEach { characteristic ->
                    _availableCharacteristics[characteristic.uuid] = characteristic
                }

                val characteristicsTable = service.characteristics.joinToString(
                    separator = "\n|--",
                    prefix = "|--"
                ) { it.uuid.toString() }

                Log.i(
                    TAG, "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable"
                )
            }
        }


        @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT])
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(
                    TAG,
                    "Successfully read characteristic ${characteristic.uuid}:\n${value.toHexString()}"
                )
                val newList = _deviceServices.value.map { customGattService ->
                    customGattService.copy(characteristics = customGattService.characteristics.map {
                        if (it.uuid == characteristic.uuid)
                            it.copy(readBytes = value)
                        else
                            it
                    })
                }
                _deviceServices.value = newList
            } else {
                val msg = "Characteristic read failed for ${characteristic.uuid}, status: $status"
                Log.e(TAG, msg)
                throw Exception(msg)
            }
        }


        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Successfully wrote to characteristic ${characteristic.uuid}")
            } else {
                val msg = "Characteristic write failed for ${characteristic.uuid}, status: $status"
                Log.e(TAG, msg)
                throw Exception(msg)
            }
        }


        // Triggered when the BLE peripheral sends a notification or indication
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            Log.i(
                TAG,
                "Characteristic ${characteristic.uuid} changed its value to:\n${value.toHexString()}"
            )


            val newList = _deviceServices.value.map { customGattService ->
                customGattService.copy(characteristics = customGattService.characteristics.map {
                    if (it.uuid == characteristic.uuid)
                        it.copy(readBytes = value)
                    else
                        it
                })
            }
            _deviceServices.value = newList
        }


        override fun onDescriptorRead(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
            value: ByteArray
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(
                    TAG,
                    "Successfully read descriptor ${descriptor.uuid}:\n${value.toHexString()}"
                )
                val newList = _deviceServices.value.map { customGattService ->
                    customGattService.copy(characteristics = customGattService.characteristics.map { customGattCharacteristics ->
                        customGattCharacteristics.copy(descriptors = customGattCharacteristics.descriptors.map {
                            if ((it.uuid == descriptor.uuid) && (descriptor.characteristic.uuid == customGattCharacteristics.uuid))
                                it.copy(readBytes = value)
                            else
                                it
                        })
                    })
                }
                _deviceServices.value = newList
            } else {
                val msg = "Descriptor read failed for ${descriptor.uuid}, status: $status"
                Log.e(TAG, msg)
                throw Exception(msg)
            }
        }


        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Successfully wrote to descriptor ${descriptor.uuid}")
            } else {
                val msg = "Descriptor write failed for ${descriptor.uuid}, status: $status"
                Log.e(TAG, msg)
                throw Exception(msg)
            }
        }
    }


    companion object {
        const val TAG = "BleLink-Log"
        fun ByteArray.toHexString(): String =
            joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }
    }
}