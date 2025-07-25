package com.davidrevolt.feature.control

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidrevolt.core.ble.model.CustomGattCharacteristic
import com.davidrevolt.core.ble.model.CustomGattDescriptor
import com.davidrevolt.core.ble.model.CustomGattService
import com.davidrevolt.feature.control.components.DisconnectedScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.compose.material3.ButtonDefaults

private val TARGET_SERVICE_UUID = UUID.fromString("dea420e6-c951-4807-b874-5b5b8ed6769d")
private val TARGET_CHARACTERISTIC_UUID = UUID.fromString("4568c4b2-e8a4-43dd-b903-b43b1892adf0")

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun ControlScreen(
    onBackClick: () -> Unit,
    viewModel: ControlViewModel = hiltViewModel()
) {
    val uiState by viewModel.controlUiState.collectAsStateWithLifecycle()
    val connectToDeviceGatt = viewModel::connectToDeviceGatt
    val onReadCharacteristic = viewModel::readCharacteristic
    val onWriteCharacteristic = viewModel::writeCharacteristic
    val onReadDescriptor = viewModel::readDescriptor
    val onWriteDescriptor = viewModel::writeDescriptor
    val onEnableCharacteristicNotifications = viewModel::enableCharacteristicNotifications
    val onDisconnectClick = viewModel::disconnectFromDevice

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = null,
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        }
    }
    when (uiState) {
        is ControlUiState.Data -> {
            val data = (uiState as ControlUiState.Data)

            ControlScreenContent(
                deviceName = data.deviceName,
                deviceAddress = data.deviceAddress,
                connectionState = data.connectionState,
                deviceServices = data.deviceServices,
                onReadCharacteristic = onReadCharacteristic,
                onWriteCharacteristic = onWriteCharacteristic,
                onReadDescriptor = onReadDescriptor,
                onWriteDescriptor = onWriteDescriptor,
                onEnableCharacteristicNotifications = onEnableCharacteristicNotifications,
                onReconnectClick = connectToDeviceGatt,
                onBackClick = onBackClick,
                onDisconnectClick = onDisconnectClick,
                snackbarHostState = snackbarHostState,
            )
        }

        is ControlUiState.Loading -> {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator() }
        }

    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreenContent(
    deviceName: String,
    deviceAddress: String,
    connectionState: Int,
    deviceServices: List<CustomGattService>,
    onReadCharacteristic: (UUID) -> Unit,
    onWriteCharacteristic: (UUID, ByteArray) -> Unit,
    onReadDescriptor: (UUID, UUID) -> Unit,
    onWriteDescriptor: (UUID, UUID, ByteArray) -> Unit,
    onEnableCharacteristicNotifications: (UUID) -> Unit,
    onReconnectClick: () -> Unit,
    onBackClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val coroutineScope = rememberCoroutineScope() // For launching snackbar

    Scaffold(
        topBar = {
            TopAppBar(
//                modifier = Modifier.padding(top = 32.dp),
                title = {
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Device Control - $deviceName",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = deviceAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onDisconnectClick) {
                        Icon(
                            Icons.Default.Close,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            contentDescription = "Disconnect"
                        )
                    }
                },
            )
        }, snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.safeDrawingPadding()
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.bluetooth),
                        contentDescription = "Connection",
                        tint = when (connectionState) {
                            2 -> MaterialTheme.colorScheme.primary
                            0 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = when (connectionState) {
                            ConnectionState.DISCONNECTED -> "Disconnected"
                            ConnectionState.CONNECTING -> "Connecting..."
                            ConnectionState.CONNECTED -> "Connected"
                            ConnectionState.DISCONNECTING -> "Disconnecting..."
                            else -> "Unknown connection state!!"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            when (connectionState) {
                ConnectionState.DISCONNECTED -> {
                    DisconnectedScreen(
                        deviceName = deviceName,
                        deviceAddress = deviceAddress,
                        onReconnectClick = onReconnectClick
                    )
                }

                ConnectionState.CONNECTED -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Найти нужный сервис и характеристику
                        val targetService = deviceServices.find { it.uuid == TARGET_SERVICE_UUID }
                        val targetCharacteristic = targetService?.characteristics?.find {
                            it.uuid == TARGET_CHARACTERISTIC_UUID
                        }

                        targetCharacteristic?.let { characteristic ->
                            item {
                                // Отображать характеристику напрямую с кастомным названием
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    elevation = CardDefaults.cardElevation(4.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Characteristic", // Кастомное название
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        // Отображать характеристику без раскрывающегося списка
                                        CharacteristicItem(
                                            characteristic = characteristic,
                                            onReadCharacteristic = onReadCharacteristic,
                                            onWriteCharacteristic = onWriteCharacteristic,
                                            onReadDescriptor = onReadDescriptor,
                                            onWriteDescriptor = onWriteDescriptor,
                                            onEnableCharacteristicNotifications = onEnableCharacteristicNotifications,
                                            snackbarHostState = snackbarHostState,
                                            coroutineScope = coroutineScope
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {}
            }

        }
    }
}

@Composable
fun ServiceItem(
    service: CustomGattService,
    onReadCharacteristic: (characteristicUUID: UUID) -> Unit,
    onWriteCharacteristic: (characteristicUUID: UUID, value: ByteArray) -> Unit,
    onReadDescriptor: (characteristicUUID: UUID, descriptorUUID: UUID) -> Unit,
    onWriteDescriptor: (characteristicUUID: UUID, descriptorUUID: UUID, value: ByteArray) -> Unit,
    onEnableCharacteristicNotifications: (characteristicUUID: UUID) -> Unit,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface, // Bold and prominent
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = if (expanded) painterResource(R.drawable.expand_less) else painterResource(
                        R.drawable.expand_more
                    ),
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (expanded) {
                val targetCharacteristic = service.characteristics.find {
                    it.uuid == TARGET_CHARACTERISTIC_UUID
                }
                targetCharacteristic?.let { characteristic ->
                    CharacteristicItem(
                        characteristic = characteristic,
                        onReadCharacteristic = onReadCharacteristic,
                        onWriteCharacteristic = onWriteCharacteristic,
                        onReadDescriptor = onReadDescriptor,
                        onWriteDescriptor = onWriteDescriptor,
                        onEnableCharacteristicNotifications = onEnableCharacteristicNotifications,
                        snackbarHostState = snackbarHostState,
                        coroutineScope = coroutineScope
                    )
                }
            }
        }
    }
}


/*
* Custom enum for input mode to send characteristic/descriptor bytearray data
* Text mode: "Hello" → [72, 101, 108, 108, 111] (UTF-8).
* Hex mode: "0200" → [0x02, 0x00].
* */

enum class CustomInputMode {
    TEXT, HEX
}

@Composable
fun CharacteristicItem(
    characteristic: CustomGattCharacteristic,
    onReadCharacteristic: (characteristicUUID: UUID) -> Unit,
    onWriteCharacteristic: (characteristicUUID: UUID, value: ByteArray) -> Unit,
    onReadDescriptor: (characteristicUUID: UUID, descriptorUUID: UUID) -> Unit,
    onWriteDescriptor: (characteristicUUID: UUID, descriptorUUID: UUID, value: ByteArray) -> Unit,
    onEnableCharacteristicNotifications: (characteristicUUID: UUID) -> Unit,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope
) {
    // Отдельные состояния для каждого значения
    var speedValue by remember { mutableStateOf("") }
    var brightnessValue by remember { mutableStateOf("") }
    var redValue by remember { mutableStateOf("") }
    var greenValue by remember { mutableStateOf("") }
    var blueValue by remember { mutableStateOf("") }

    // Функция для парсинга данных при чтении
    LaunchedEffect(characteristic.readBytes) {
        characteristic.readBytes?.let {
            val dataValue = it.toString(Charsets.UTF_8)
            val values = dataValue.split(",")
            if (values.size >= 5) {
                speedValue = values[0]
                brightnessValue = values[1]
                redValue = values[2]
                greenValue = values[3]
                blueValue = values[4]
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Отображение текущих данных
        characteristic.readBytes?.let {
            val dataValue = it.toString(Charsets.UTF_8)
            val values = dataValue.split(",")

            if (values.size >= 5) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Speed: ${values[0]}", style = MaterialTheme.typography.bodySmall)
                    Text("Brightness: ${values[1]}", style = MaterialTheme.typography.bodySmall)
                    Text("Red: ${values[2]}", style = MaterialTheme.typography.bodySmall)
                    Text("Green: ${values[3]}", style = MaterialTheme.typography.bodySmall)
                    Text("Blue: ${values[4]}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Поля ввода для каждого значения
        if (characteristic.isWritable) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = speedValue,
                    onValueChange = { speedValue = it.filter { char -> char.isDigit() } },
                    label = { Text("Speed (0-100)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = brightnessValue,
                    onValueChange = { brightnessValue = it.filter { char -> char.isDigit() } },
                    label = { Text("Brightness (0-100)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = redValue,
                    onValueChange = { redValue = it.filter { char -> char.isDigit() } },
                    label = { Text("Red (0-255)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = greenValue,
                    onValueChange = { greenValue = it.filter { char -> char.isDigit() } },
                    label = { Text("Green (0-255)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = blueValue,
                    onValueChange = { blueValue = it.filter { char -> char.isDigit() } },
                    label = { Text("Blue (0-255)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        // Кнопки Read, Write и OFF
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (characteristic.isReadable) {
                Button(
                    onClick = { onReadCharacteristic(characteristic.uuid) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Read")
                }
            }

            if (characteristic.isWritable) {
                Button(
                    onClick = {
                        // Объединяем значения в строку через запятую
                        val combinedValue = "$speedValue,$brightnessValue,$redValue,$greenValue,$blueValue"
                        val bytes = combinedValue.toByteArray(Charsets.UTF_8)
                        onWriteCharacteristic(characteristic.uuid, bytes)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = speedValue.isNotBlank() && brightnessValue.isNotBlank() &&
                            redValue.isNotBlank() && greenValue.isNotBlank() && blueValue.isNotBlank()
                ) {
                    Text("Write")
                }
            }
            if (characteristic.isWritable) {
                Button(
                    onClick = {
                        // Отправляем команду выключения: 0,0,0,0,0
                        val offCommand = "0,0,0,0,0"
                        val bytes = offCommand.toByteArray(Charsets.UTF_8)
                        onWriteCharacteristic(characteristic.uuid, bytes)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("OFF")
                }
            }
        }
    }
}

@Composable
fun DescriptorItem(
    characteristic: CustomGattCharacteristic,
    descriptor: CustomGattDescriptor,
    onReadDescriptor: (characteristicUUID: UUID, descriptorUUID: UUID) -> Unit,
    onWriteDescriptor: (characteristicUUID: UUID, descriptorUUID: UUID, value: ByteArray) -> Unit,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope
) {
    var expanded by remember { mutableStateOf(false) }
    var writeValue by remember { mutableStateOf("") }
    var inputMode by remember { mutableStateOf(CustomInputMode.TEXT) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, end = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Descriptor: ${descriptor.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = if (expanded) painterResource(R.drawable.expand_less) else painterResource(
                    R.drawable.expand_more
                ),
                contentDescription = "Expand",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        if (expanded) {
            Text(
                text = "UUID: ${descriptor.uuid}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Text(
                text = "Permissions: ${descriptor.permissions.joinToString { it.toReadableName() }}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            descriptor.readBytes?.let {
                Text(
                    text = "Value: ${
                        it.joinToString(" ") { byte ->
                            String.format(
                                "%02X",
                                byte
                            )
                        }
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (descriptor.writable) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = writeValue,
                            onValueChange = {
                                writeValue = if (inputMode == CustomInputMode.HEX) {
                                    it.filter { char ->
                                        char.isDigit() || "abcdefABCDEF".contains(
                                            char
                                        )
                                    }
                                } else {
                                    it
                                }
                            },
                            label = {
                                Text(if (inputMode == CustomInputMode.TEXT) "Enter text" else "Enter hex (e.g., 0200)")
                            },
                            placeholder = {
                                Text(if (inputMode == CustomInputMode.TEXT) "e.g., Hello" else "e.g., 0200")
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .padding(vertical = 4.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (inputMode == CustomInputMode.HEX) KeyboardType.Ascii else KeyboardType.Text
                            )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SingleChoiceSegmentedButtonRow {
                                SegmentedButton(
                                    selected = inputMode == CustomInputMode.TEXT,
                                    onClick = { inputMode = CustomInputMode.TEXT },
                                    shape = RoundedCornerShape(
                                        topStart = 8.dp,
                                        bottomStart = 8.dp
                                    )
                                ) {
                                    Text("Text")
                                }
                                SegmentedButton(
                                    selected = inputMode == CustomInputMode.HEX,
                                    onClick = { inputMode = CustomInputMode.HEX },
                                    shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                                ) {
                                    Text("Hex")
                                }
                            }
                            Button(
                                onClick = {
                                    val bytes = when (inputMode) {
                                        CustomInputMode.TEXT -> writeValue.toByteArray(Charsets.UTF_8)
                                        CustomInputMode.HEX -> {
                                            try {
                                                hexStringToByteArray(writeValue)
                                            } catch (e: Exception) {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = "Failed to parse hex: ${e.message}"
                                                    )
                                                }
                                                byteArrayOf()
                                            }
                                        }
                                    }
                                    if (bytes.isNotEmpty()) {
                                        onWriteDescriptor(
                                            characteristic.uuid,
                                            descriptor.uuid,
                                            bytes
                                        )
                                        writeValue = ""
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                enabled = writeValue.isNotBlank()
                            ) {
                                Text("Write")
                            }
                        }
                    }
                }
                if (descriptor.readable) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                onReadDescriptor(
                                    characteristic.uuid,
                                    descriptor.uuid
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Text("Read")
                        }
                    }
                }
            }
        }
    }
}


// Helper function to convert hex string to ByteArray
fun hexStringToByteArray(hex: String): ByteArray {
    val cleanHex = hex.replace(" ", "").trim()
    require(cleanHex.length % 2 == 0) { "Hex string must have an even number of characters" }
    require(cleanHex.all { it.isDigit() || "abcdefABCDEF".contains(it) }) { "Invalid hex characters" }
    return ByteArray(cleanHex.length / 2) { i ->
        cleanHex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}

private object ConnectionState {
    const val DISCONNECTED = 0
    const val CONNECTING = 1
    const val CONNECTED = 2
    const val DISCONNECTING = 3
}