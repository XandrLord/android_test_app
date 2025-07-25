package com.davidrevolt.core.ble.model.modelmapper

import android.bluetooth.BluetoothGattCharacteristic
import com.davidrevolt.core.ble.model.CustomGattCharacteristic
import com.davidrevolt.core.ble.util.BleNamesResolver.resolveUuid
import com.davidrevolt.core.ble.util.containsProperty
import com.davidrevolt.core.ble.util.getFormatType
import com.davidrevolt.core.ble.util.propertiesAsList

/*
* Convert BluetoothGattCharacteristic to CustomGattCharacteristics which is more readable Characteristic obj
*/

fun BluetoothGattCharacteristic.asCustomDeviceCharacteristics() =
    CustomGattCharacteristic(
        uuid = this.uuid,
        name = this.resolveUuid(),
        properties = this.propertiesAsList(),
        isReadable = this.containsProperty(BluetoothGattCharacteristic.PROPERTY_READ),
        isWritable = this.containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE) ||
                this.containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE),
        isNotifiable = this.containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY),
        isIndicatable = this.containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY),
        readBytes = null,
        formatType = this.getFormatType(),
        descriptors = this.descriptors.map { bluetoothGattDescriptor -> bluetoothGattDescriptor.asCustomGattDescriptor() }
    )

