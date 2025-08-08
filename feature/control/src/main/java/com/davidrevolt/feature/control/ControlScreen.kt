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
    var white1Value by remember { mutableStateOf("") }
    var white2Value by remember { mutableStateOf("") }
    var red1Value by remember { mutableStateOf("") }
    var green1Value by remember { mutableStateOf("") }
    var blue1Value by remember { mutableStateOf("") }
    var red2Value by remember { mutableStateOf("") }
    var green2Value by remember { mutableStateOf("") }
    var blue2Value by remember { mutableStateOf("") }
    var regimeValue by remember { mutableStateOf("") }

    // Функция для парсинга данных при чтении
    LaunchedEffect(characteristic.readBytes) {
        characteristic.readBytes?.let {
            val dataValue = it.toString(Charsets.UTF_8)
            val values = dataValue.split(",")
            if (values.size >= 10) {
                speedValue = values[0]
                white1Value = values[1]
                white2Value = values[2]
                red1Value = values[3]
                green1Value = values[4]
                blue1Value = values[5]
                red2Value = values[6]
                green2Value = values[7]
                blue2Value = values[8]
                regimeValue = values[9]
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

            if (values.size >= 10) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Текущие значения:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))
                    Text("Speed: ${values[0]}", style = MaterialTheme.typography.bodySmall)
                    Text("White1: ${values[1]}", style = MaterialTheme.typography.bodySmall)
                    Text("White2: ${values[2]}", style = MaterialTheme.typography.bodySmall)
                    Text("Red1: ${values[3]}", style = MaterialTheme.typography.bodySmall)
                    Text("Green1: ${values[4]}", style = MaterialTheme.typography.bodySmall)
                    Text("Blue1: ${values[5]}", style = MaterialTheme.typography.bodySmall)
                    Text("Red2: ${values[6]}", style = MaterialTheme.typography.bodySmall)
                    Text("Green2: ${values[7]}", style = MaterialTheme.typography.bodySmall)
                    Text("Blue2: ${values[8]}", style = MaterialTheme.typography.bodySmall)
                    Text("Regime: ${values[9]}", style = MaterialTheme.typography.bodySmall)
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
                Text("Вентиляторы", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))
                // Speed
                OutlinedTextField(
                    value = speedValue,
                    onValueChange = {
                        val filtered = it.filter { char -> char.isDigit() }
                        if (filtered.isEmpty() || filtered.toIntOrNull()?.let { it <= 2 } == true) {
                            speedValue = filtered
                        }
                    },
                    label = { Text("Speed (0-2)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // White каналы
                Text("Белые каналы", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))

                OutlinedTextField(
                    value = white1Value,
                    onValueChange = {
                        val filtered = it.filter { char -> char.isDigit() }
                        if (filtered.isEmpty() || filtered.toIntOrNull()?.let { it <= 100 } == true) {
                            white1Value = filtered
                        }
                    },
                    label = { Text("White1 (0-100)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = white2Value,
                    onValueChange = {
                        val filtered = it.filter { char -> char.isDigit() }
                        if (filtered.isEmpty() || filtered.toIntOrNull()?.let { it <= 100 } == true) {
                            white2Value = filtered
                        }
                    },
                    label = { Text("White2 (0-100)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // RGB каналы 1
                Text("RGB канал 1", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))

                OutlinedTextField(
                    value = red1Value,
                    onValueChange = {
                        val filtered = it.filter { char -> char.isDigit() }
                        if (filtered.isEmpty() || filtered.toIntOrNull()?.let { it <= 255 } == true) {
                            red1Value = filtered
                        }
                    },
                    label = { Text("Red1 (0-255)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = green1Value,
                    onValueChange = {
                        val filtered = it.filter { char -> char.isDigit() }
                        if (filtered.isEmpty() || filtered.toIntOrNull()?.let { it <= 255 } == true) {
                            green1Value = filtered
                        }
                    },
                    label = { Text("Green1 (0-255)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = blue1Value,
                    onValueChange = {
                        val filtered = it.filter { char -> char.isDigit() }
                        if (filtered.isEmpty() || filtered.toIntOrNull()?.let { it <= 255 } == true) {
                            blue1Value = filtered
                        }
                    },
                    label = { Text("Blue1 (0-255)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // RGB каналы 2
                Text("RGB канал 2", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))

                OutlinedTextField(
                    value = red2Value,
                    onValueChange = {
                        val filtered = it.filter { char -> char.isDigit() }
                        if (filtered.isEmpty() || filtered.toIntOrNull()?.let { it <= 255 } == true) {
                            red2Value = filtered
                        }
                    },
                    label = { Text("Red2 (0-255)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = green2Value,
                    onValueChange = {
                        val filtered = it.filter { char -> char.isDigit() }
                        if (filtered.isEmpty() || filtered.toIntOrNull()?.let { it <= 255 } == true) {
                            green2Value = filtered
                        }
                    },
                    label = { Text("Green2 (0-255)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = blue2Value,
                    onValueChange = {
                        val filtered = it.filter { char -> char.isDigit() }
                        if (filtered.isEmpty() || filtered.toIntOrNull()?.let { it <= 255 } == true) {
                            blue2Value = filtered
                        }
                    },
                    label = { Text("Blue2 (0-255)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Text("Режим подсветки", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))

                // Regime в самом конце
                OutlinedTextField(
                    value = regimeValue,
                    onValueChange = { regimeValue = it.filter { char -> char.isDigit() } },
                    label = { Text("Regime (0-6)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        // Кнопки Read, Write и OFF
        Text("Управление устройством", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (characteristic.isReadable) {
                Button(
                    onClick = { onReadCharacteristic(characteristic.uuid) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("READ")
                }
            }

            if (characteristic.isWritable) {
                Button(
                    onClick = {
                        // Объединяем все 10 значений в строку через запятую
                        val combinedValue = "$speedValue,$white1Value,$white2Value,$red1Value,$green1Value,$blue1Value,$red2Value,$green2Value,$blue2Value,$regimeValue"
                        val bytes = combinedValue.toByteArray(Charsets.UTF_8)
                        onWriteCharacteristic(characteristic.uuid, bytes)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = speedValue.isNotBlank() && white1Value.isNotBlank() && white2Value.isNotBlank() &&
                            red1Value.isNotBlank() && green1Value.isNotBlank() && blue1Value.isNotBlank() &&
                            red2Value.isNotBlank() && green2Value.isNotBlank() && blue2Value.isNotBlank() &&
                            regimeValue.isNotBlank()
                ) {
                    Text("WRITE")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (characteristic.isWritable) {
                Button(
                    onClick = {
                        // Отправляем команду выключения: 0,0,0,0,0,0,0,0,0,0 (10 нулей)
                        val offCommand = "0,0,0,0,0,0,0,0,0,0"
                        val bytes = offCommand.toByteArray(Charsets.UTF_8)
                        onWriteCharacteristic(characteristic.uuid, bytes)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("ВЫКЛ")
                }
            }
        }

        if (characteristic.isWritable) {
            // Speed кнопки
            Text("Скорость вентиляторов", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val command = "0,$white1Value,$white2Value,$red1Value,$green1Value,$blue1Value,$red2Value,$green2Value,$blue2Value,$regimeValue"
                        val bytes = command.toByteArray(Charsets.UTF_8)
                        onWriteCharacteristic(characteristic.uuid, bytes)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("СКОРОСТЬ 0")
                }

                Button(
                    onClick = {
                        val command = "1,$white1Value,$white2Value,$red1Value,$green1Value,$blue1Value,$red2Value,$green2Value,$blue2Value,$regimeValue"
                        val bytes = command.toByteArray(Charsets.UTF_8)
                        onWriteCharacteristic(characteristic.uuid, bytes)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("СКОРОСТЬ 1")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val command = "2,$white1Value,$white2Value,$red1Value,$green1Value,$blue1Value,$red2Value,$green2Value,$blue2Value,$regimeValue"
                        val bytes = command.toByteArray(Charsets.UTF_8)
                        onWriteCharacteristic(characteristic.uuid, bytes)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("СКОРОСТЬ 2")
                }
            }

            // Color кнопки
            Text("Пресеты цветов", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        // Purple: RGB1 = (128,0,128), RGB2 = (128,0,128)
                        val command = "$speedValue,$white1Value,$white2Value,128,0,128,128,0,128,$regimeValue"
                        val bytes = command.toByteArray(Charsets.UTF_8)
                        onWriteCharacteristic(characteristic.uuid, bytes)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("ФИОЛЕТОВЫЙ")
                }

                Button(
                    onClick = {
                        // Blue: RGB1 = (0,0,255), RGB2 = (0,0,255)
                        val command = "$speedValue,$white1Value,$white2Value,0,0,255,0,0,255,$regimeValue"
                        val bytes = command.toByteArray(Charsets.UTF_8)
                        onWriteCharacteristic(characteristic.uuid, bytes)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("СИНИЙ")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        // Красный: RGB1 = (255,0,0), RGB2 = (255,0,0)
                        val command = "$speedValue,$white1Value,$white2Value,255,0,0,255,0,0,$regimeValue"
                        val bytes = command.toByteArray(Charsets.UTF_8)
                        onWriteCharacteristic(characteristic.uuid, bytes)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("КРАСНЫЙ")
                }

                Button(
                    onClick = {
                        // Оранжевый: RGB1 = (255,165,0), RGB2 = (255,165,0)
                        val command = "$speedValue,$white1Value,$white2Value,255,165,0,255,165,0,$regimeValue"
                        val bytes = command.toByteArray(Charsets.UTF_8)
                        onWriteCharacteristic(characteristic.uuid, bytes)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("ОРАНЖЕВЫЙ")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        // Желтый: RGB1 = (255,255,0), RGB2 = (255,255,0)
                        val command = "$speedValue,$white1Value,$white2Value,255,255,0,255,255,0,$regimeValue"
                        val bytes = command.toByteArray(Charsets.UTF_8)
                        onWriteCharacteristic(characteristic.uuid, bytes)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("ЖЁЛТЫЙ")
                }

                Button(
                    onClick = {
                        // Зеленый: RGB1 = (0,255,0), RGB2 = (0,255,0)
                        val command = "$speedValue,$white1Value,$white2Value,0,255,0,0,255,0,$regimeValue"
                        val bytes = command.toByteArray(Charsets.UTF_8)
                        onWriteCharacteristic(characteristic.uuid, bytes)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("ЗЕЛЁНЫЙ")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        // Голубой: RGB1 = (0,255,255), RGB2 = (0,255,255)
                        val command = "$speedValue,$white1Value,$white2Value,0,255,255,0,255,255,$regimeValue"
                        val bytes = command.toByteArray(Charsets.UTF_8)
                        onWriteCharacteristic(characteristic.uuid, bytes)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("ГОЛУБОЙ")
                }
            }

            // White кнопки
            Text("Управление белым", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val command = "$speedValue,0,0,$red1Value,$green1Value,$blue1Value,$red2Value,$green2Value,$blue2Value,$regimeValue"
                        val bytes = command.toByteArray(Charsets.UTF_8)
                        onWriteCharacteristic(characteristic.uuid, bytes)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("БЕЛЫЙ ВЫКЛ")
                }

                Button(
                    onClick = {
                        val command = "$speedValue,50,50,$red1Value,$green1Value,$blue1Value,$red2Value,$green2Value,$blue2Value,$regimeValue"
                        val bytes = command.toByteArray(Charsets.UTF_8)
                        onWriteCharacteristic(characteristic.uuid, bytes)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("БЕЛЫЙ 50%")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val command = "$speedValue,100,100,$red1Value,$green1Value,$blue1Value,$red2Value,$green2Value,$blue2Value,$regimeValue"
                        val bytes = command.toByteArray(Charsets.UTF_8)
                        onWriteCharacteristic(characteristic.uuid, bytes)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("БЕЛЫЙ 100%")
                }
            }

            // Regime кнопки
            Text("Управление режимами", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val command = "$speedValue,$white1Value,$white2Value,$red1Value,$green1Value,$blue1Value,$red2Value,$green2Value,$blue2Value,0"
                        val bytes = command.toByteArray(Charsets.UTF_8)
                        onWriteCharacteristic(characteristic.uuid, bytes)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("РЕЖИМ 0")
                }

                Button(
                    onClick = {
                        val command = "$speedValue,$white1Value,$white2Value,$red1Value,$green1Value,$blue1Value,$red2Value,$green2Value,$blue2Value,1"
                        val bytes = command.toByteArray(Charsets.UTF_8)
                        onWriteCharacteristic(characteristic.uuid, bytes)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("РЕЖИМ 1")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val command = "$speedValue,$white1Value,$white2Value,$red1Value,$green1Value,$blue1Value,$red2Value,$green2Value,$blue2Value,2"
                        val bytes = command.toByteArray(Charsets.UTF_8)
                        onWriteCharacteristic(characteristic.uuid, bytes)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("РЕЖИМ 2")
                }

                Button(
                    onClick = {
                        val command = "$speedValue,$white1Value,$white2Value,$red1Value,$green1Value,$blue1Value,$red2Value,$green2Value,$blue2Value,3"
                        val bytes = command.toByteArray(Charsets.UTF_8)
                        onWriteCharacteristic(characteristic.uuid, bytes)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("РЕЖИМ 3")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val command = "$speedValue,$white1Value,$white2Value,$red1Value,$green1Value,$blue1Value,$red2Value,$green2Value,$blue2Value,4"
                        val bytes = command.toByteArray(Charsets.UTF_8)
                        onWriteCharacteristic(characteristic.uuid, bytes)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("РЕЖИМ 4")
                }

                Button(
                    onClick = {
                        val command = "$speedValue,$white1Value,$white2Value,$red1Value,$green1Value,$blue1Value,$red2Value,$green2Value,$blue2Value,5"
                        val bytes = command.toByteArray(Charsets.UTF_8)
                        onWriteCharacteristic(characteristic.uuid, bytes)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("РЕЖИМ 5")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val command = "$speedValue,$white1Value,$white2Value,$red1Value,$green1Value,$blue1Value,$red2Value,$green2Value,$blue2Value,6"
                        val bytes = command.toByteArray(Charsets.UTF_8)
                        onWriteCharacteristic(characteristic.uuid, bytes)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("РЕЖИМ 6")
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