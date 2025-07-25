package com.davidrevolt.core.ble.model.modelmapper

import android.bluetooth.BluetoothGattDescriptor
import com.davidrevolt.core.ble.model.CustomGattDescriptor
import com.davidrevolt.core.ble.util.BleNamesResolver.resolveUuid

/*
* Android SDK’s getPermissions() method always returns false when checking for readability and writability,
* hence the permission flags oftentimes don’t reflect the actual readability or writability of the descriptors.
* because of that we sets permissions list as emptyList, readable and writable to true
* when getPermissions() fixed, actual methods are ready to use
* */

fun BluetoothGattDescriptor.asCustomGattDescriptor() =
    CustomGattDescriptor(
        uuid = this.uuid,
        name = this.resolveUuid(),
        permissions = emptyList() /*this.permissionsAsList()*/,
        readable = true /*|| this.containsPermission(BluetoothGattDescriptor.PERMISSION_READ) || this.containsPermission(
            BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED) || this.containsPermission(
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM)*/
        ,
        writable = true /*|| this.containsPermission(BluetoothGattDescriptor.PERMISSION_WRITE) || this.containsPermission(
            BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED) || this.containsPermission(
                BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM) || this.containsPermission(
            BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED)|| this.containsPermission(
            BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM)*/
        ,
        readBytes = null,
    )
