package com.davidrevolt.core.ble.model

// Used to describe Gatt Characteristics value format type as enum
enum class FormatTypeAsEnum(val value: Int) {
    FORMAT_FLOAT(52),   // 32-bit float (0x34)
    FORMAT_SFLOAT(50),  // 16-bit float (0x32)
    FORMAT_SINT16(34),  // Signed 16-bit integer (0x22)
    FORMAT_SINT32(36),  // Signed 32-bit integer (0x24)
    FORMAT_SINT8(33),   // Signed 8-bit integer (0x21)
    FORMAT_UINT16(18),  // Unsigned 16-bit integer (0x12)
    FORMAT_UINT32(20),  // Unsigned 32-bit integer (0x14)
    FORMAT_UINT8(17),
    FORMAT_UNKNOWN(-1);   // Unsigned 8-bit integer (0x11)

    /**
     * Converts this enum instance's value to its format type name (without 'FORMAT_' prefix).
     * @return The format type name in uppercase (e.g., "FLOAT", "UINT32").
     */
    fun toReadableName(): String {
        return name.removePrefix("FORMAT_").uppercase()
    }
}

