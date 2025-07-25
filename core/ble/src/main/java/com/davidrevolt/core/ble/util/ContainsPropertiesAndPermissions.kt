package com.davidrevolt.core.ble.util

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.davidrevolt.core.ble.model.FormatTypeAsEnum
import com.davidrevolt.core.ble.model.PermissionsAsEnum
import com.davidrevolt.core.ble.model.PropertiesAsEnum

/**
 * Convert Characteristic.properties value to list of all the supported properties as enum
 */
internal fun BluetoothGattCharacteristic.propertiesAsList(): List<PropertiesAsEnum> {
    val propertyList = mutableListOf<PropertiesAsEnum>()
    PropertiesAsEnum.entries.forEach { propertiesAsEnum ->
        if (this.containsProperty(propertiesAsEnum.value))
            propertyList.add(propertiesAsEnum)
    }
    return propertyList
}

/**
 * Convert Characteristic.properties value to list of all the supported properties as enum
 */
internal fun BluetoothGattCharacteristic.getFormatType(): FormatTypeAsEnum {
    FormatTypeAsEnum.entries.forEach { formatTypeAsEnum ->
        if (this.containsProperty(formatTypeAsEnum.value))
            return formatTypeAsEnum

    }
    return FormatTypeAsEnum.FORMAT_UNKNOWN // Default
}

/**
 * Return if Characteristic contains property
 * e.g: BluetoothGattCharacteristic.containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)
 * */
internal fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean =
    properties and property != 0


/**
 * Convert Descriptor.permissions value to list of all the supported permissions as enum
 */
internal fun BluetoothGattDescriptor.permissionsAsList(): List<PermissionsAsEnum> {
    val permissionsList = mutableListOf<PermissionsAsEnum>()
    PermissionsAsEnum.entries.forEach { permissionsAsEnum ->
        if (this.containsPermission(permissionsAsEnum.value))
            permissionsList.add(permissionsAsEnum)
    }
    return permissionsList
}

/**
 * Return if Descriptor contains permission
 * e.g: Descriptor.containsProperty(BluetoothGattDescriptor.PERMISSION_READ)
 * */
internal fun BluetoothGattDescriptor.containsPermission(permission: Int): Boolean =
    permissions and permission != 0

