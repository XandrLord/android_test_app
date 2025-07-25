package com.davidrevolt.core.ble.model


// Used to describe Gatt Descriptor permissions [the int value] as enum
enum class PermissionsAsEnum(val value: Int) {
    PERMISSION_READ(1),
    PERMISSION_READ_ENCRYPTED(2),
    PERMISSION_READ_ENCRYPTED_MITM(4),
    PERMISSION_WRITE(16),
    PERMISSION_WRITE_ENCRYPTED(32),
    PERMISSION_WRITE_ENCRYPTED_MITM(64),
    PERMISSION_WRITE_SIGNED(128),
    PERMISSION_WRITE_SIGNED_MITM(256);


    fun toReadableName(): String {
        return when (this) {
            PERMISSION_READ -> "Read"
            PERMISSION_READ_ENCRYPTED -> "Read Encrypted"
            PERMISSION_READ_ENCRYPTED_MITM -> "Read Encrypted MITM"
            PERMISSION_WRITE -> "Write"
            PERMISSION_WRITE_ENCRYPTED -> "Write Encrypted"
            PERMISSION_WRITE_ENCRYPTED_MITM -> "Write Encrypted MITM"
            PERMISSION_WRITE_SIGNED -> "Write Signed"
            PERMISSION_WRITE_SIGNED_MITM -> "Write Signed MITM"
        }
    }
}

