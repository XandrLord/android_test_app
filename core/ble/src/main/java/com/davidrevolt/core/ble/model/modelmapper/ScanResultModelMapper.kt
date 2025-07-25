package com.davidrevolt.core.ble.model.modelmapper

import android.Manifest
import android.bluetooth.le.ScanResult
import androidx.annotation.RequiresPermission
import com.davidrevolt.core.ble.model.CustomScanResult
import com.davidrevolt.core.ble.util.BleNamesResolver.getManufacturerName

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun ScanResult.asCustomScanResult() =
    CustomScanResult(
        name = this.device.name ?: "Unknown Device",
        address = this.device.address,
        rssi = this.rssi,
        manufacturer = getManufacturerName(this.scanRecord?.manufacturerSpecificData)
    )
