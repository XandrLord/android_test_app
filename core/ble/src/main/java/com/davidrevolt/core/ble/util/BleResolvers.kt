package com.davidrevolt.core.ble.util

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.util.SparseArray
import java.util.UUID


object BleNamesResolver {
    private var knownServices: Map<UUID, String>
    private var knownCharacteristics: Map<UUID, String>
    private var knownDescriptors: Map<UUID, String>
    private var knownManufacturersIds: Map<Int, String>

    init {

        // Xiaomi Mi Band 4c Services:
        knownServices = mapOf(
            UUID.fromString("00001800-0000-1000-8000-00805f9b34fb") to "Generic Access",
            UUID.fromString("00001801-0000-1000-8000-00805f9b34fb") to "Generic Attribute",
            UUID.fromString("16187f00-0000-1000-8000-00807f9b34fb") to "Xiaomi Wear Service - Mi Smart Watch 4C/Redmi Band",
        )


        // Xiaomi Mi Band 4c Characteristic:
        knownCharacteristics = mapOf(
            UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb") to "Device Name",
            UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb") to "Appearance",
            UUID.fromString("00002a04-0000-1000-8000-00805f9b34fb") to "Peripheral Preferred Connection Parameters",
            UUID.fromString("00002aa6-0000-1000-8000-00805f9b34fb") to "Central Address Resolution",

            UUID.fromString("16187f01-0000-1000-8000-00807f9b34fb") to "Likely the authentication/control characteristic",
            UUID.fromString("16187f02-0000-1000-8000-00807f9b34fb") to "Might be for encrypted communication",
            UUID.fromString("16187f03-0000-1000-8000-00807f9b34fb") to "Could be used for notifications/responses",
            UUID.fromString("16187f04-0000-1000-8000-00807f9b34fb") to "Might be related to data retrieval",
        )



        knownDescriptors =
            mapOf(UUID.fromString("00002902-0000-1000-8000-00807f9b34fb") to " enable or disable notifications and indications")


        knownManufacturersIds = mapOf(
            0x004C to "Apple Inc.",
            0x0006 to "Microsoft",
            0x000F to "Broadcom",
            0x0075 to "Samsung",
            0x0059 to "Nordic Semiconductor",
            0x038F to "Xiaomi Inc.",
            0x00E0 to "Google",
            0x0171 to "Huawei Technologies",
            0x0131 to "Sony Corporation",
            0x029A to "OnePlus Technology",
            0x00D2 to "Motorola Mobility LLC",
            0x0426 to "Realtek Semiconductor Corp.",
            0x016D to "Fitbit Inc."
            // Add more manufacturer IDs as needed
        )
    }

    /**
     * Convert The BluetoothGattService.UUID TO Readable Name
     * */
    fun BluetoothGattService.resolveUuid() =
        knownServices[uuid] ?: "Unknown Service"

    /**
     * Convert The Characteristic.UUID TO Readable Name
     * */
    fun BluetoothGattCharacteristic.resolveUuid() =
        knownCharacteristics[uuid] ?: "Unknown Characteristic"

    /**
     * Convert The Descriptor.UUID TO Readable Name
     * */
    fun BluetoothGattDescriptor.resolveUuid() =
        knownDescriptors[uuid] ?: "Unknown Descriptor"


    /**
     * Convert ScanResult.scanRecord?.manufacturerSpecificData to Manufacturer Name
     * */
    fun getManufacturerName(manufacturerSpecificData: SparseArray<ByteArray>?): String {
        if (manufacturerSpecificData == null || manufacturerSpecificData.size() == 0) {
            return "Unknown Manufacturer [Manufacturer Specific Data unavailable]"
        }
        for (i in 0 until manufacturerSpecificData.size()) {
            val manufacturerId = manufacturerSpecificData.keyAt(i)
            knownManufacturersIds[manufacturerId]?.let { return it }
        }

        return "Unknown Manufacturer"
    }
}
