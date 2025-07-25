package com.davidrevolt.core.ble.model

// Used to describe Gatt Characteristics properties [the int value] as enum
enum class PropertiesAsEnum(val value: Int) {
    PROPERTY_BROADCAST(1),
    PROPERTY_EXTENDED_PROPS(128),
    PROPERTY_INDICATE(32),
    PROPERTY_NOTIFY(16),
    PROPERTY_READ(2),
    PROPERTY_SIGNED_WRITE(64),
    PROPERTY_WRITE(8),
    PROPERTY_WRITE_NO_RESPONSE(4);

    fun toReadableName(): String {
        return when (this) {
            PROPERTY_BROADCAST -> "Broadcast"
            PROPERTY_EXTENDED_PROPS -> "Extended props"
            PROPERTY_INDICATE -> "Indicate"
            PROPERTY_NOTIFY -> "Notify"
            PROPERTY_READ -> "Read"
            PROPERTY_SIGNED_WRITE -> "Signed Write"
            PROPERTY_WRITE -> "Write"
            PROPERTY_WRITE_NO_RESPONSE -> "Write No Response"
        }
    }
}