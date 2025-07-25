package com.davidrevolt.core.ble.model

import java.util.UUID

data class CustomGattCharacteristic(
    val uuid: UUID,
    val name: String,
    val properties: List<PropertiesAsEnum>, // Doesn't mean we have permission to use the prop
    val isReadable: Boolean,
    val isWritable: Boolean,
    val isNotifiable: Boolean,
    val isIndicatable: Boolean,
    val readBytes: ByteArray?,
    val formatType: FormatTypeAsEnum,
    val descriptors: List<CustomGattDescriptor>
)


