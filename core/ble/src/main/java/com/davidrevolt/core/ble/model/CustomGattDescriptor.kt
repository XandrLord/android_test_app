package com.davidrevolt.core.ble.model

import java.util.UUID

data class CustomGattDescriptor(
    val uuid: UUID,
    val name: String,
    val permissions: List<PermissionsAsEnum>,
    val readable: Boolean,
    val writable: Boolean,
    val readBytes: ByteArray?
    )