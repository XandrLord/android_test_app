package com.davidrevolt.feature.control

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidrevolt.core.ble.BluetoothLeService
import com.davidrevolt.core.ble.model.CustomGattService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import android.bluetooth.BluetoothProfile
import kotlinx.coroutines.flow.distinctUntilChanged

private val TARGET_SERVICE_UUID = UUID.fromString("dea420e6-c951-4807-b874-5b5b8ed6769d")
private val TARGET_CHARACTERISTIC_UUID = UUID.fromString("4568c4b2-e8a4-43dd-b903-b43b1892adf0")

@HiltViewModel
class ControlViewModel @Inject constructor(
    private val bluetoothLeService: BluetoothLeService,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Get it from NavGraph
    private val bleDeviceName: String = checkNotNull(savedStateHandle[DEVICE_NAME])
    private val bleDeviceAddress: String = checkNotNull(savedStateHandle[DEVICE_ADDRESS])

    // Used to send msg to snackbar
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val controlUiState = combine(
        bluetoothLeService.getConnectionState(),
        bluetoothLeService.getDeviceServices()
    ) { connectionState, deviceServices ->
//        if (connectionState == BluetoothProfile.STATE_CONNECTED && deviceServices.isNotEmpty()) {
//            autoReadTargetCharacteristic()
//        }
        ControlUiState.Data(
            deviceName = bleDeviceName,
            deviceAddress = bleDeviceAddress,
            connectionState = connectionState,
            deviceServices = deviceServices
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ControlUiState.Loading
        )

    init {
        connectToDeviceGatt()

        // Отдельное отслеживание для одноразового чтения при подключении
        viewModelScope.launch {
            bluetoothLeService.getConnectionState()
                .distinctUntilChanged() // Ключевое - срабатывает только при изменении состояния
                .collect { connectionState ->
                    if (connectionState == BluetoothProfile.STATE_CONNECTED) {
                        delay(1000) // Ждем стабилизации соединения
                        try {
                            bluetoothLeService.readCharacteristic(TARGET_CHARACTERISTIC_UUID)
                        } catch (e: Exception) {
                            _uiEvent.emit(UiEvent.ShowSnackbar("Auto-read failed: ${e.message}"))
                        }
                    }
                }
        }
    }

    fun connectToDeviceGatt() {
        viewModelScope.launch {
            try {
                bluetoothLeService.connectToDeviceGatt(deviceAddress = bleDeviceAddress)
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowSnackbar("${e.message}"))
            }
        }
    }

    fun readCharacteristic(characteristicUUID: UUID) {
        viewModelScope.launch {
            try {
                bluetoothLeService.readCharacteristic(characteristicUUID)
            } catch (e: Exception) {
                //_uiEvent.emit(UiEvent.ShowSnackbar("${e.message}"))
            }
        }
    }

    fun writeCharacteristic(characteristicUUID: UUID, value: ByteArray) {
        viewModelScope.launch {
            try {
                bluetoothLeService.writeCharacteristic(characteristicUUID, value)

                // Первая попытка чтения через 200ms
                delay(200)
                try {
                    bluetoothLeService.readCharacteristic(characteristicUUID)
                } catch (e: Exception) {
                    // Если первая попытка не удалась, пробуем через 5 секунд
                    delay(5000)
                    try {
                        bluetoothLeService.readCharacteristic(characteristicUUID)
                    } catch (e2: Exception) {
                        // Если вторая попытка не удалась, пробуем через 5 секунд
                        delay(2000)
                        try {
                            bluetoothLeService.readCharacteristic(characteristicUUID)
                        } catch (e3: Exception) {
                            _uiEvent.emit(UiEvent.ShowSnackbar("Write failed: ${e3.message}"))
                        }
                    }
                }
            } catch (e: Exception) {
                // Показываем ошибку только если не удалась сама запись
                _uiEvent.emit(UiEvent.ShowSnackbar("Write failed: ${e.message}"))
            }
        }
    }

    fun readDescriptor(characteristicUUID: UUID, descriptorUUID: UUID) {
        viewModelScope.launch {
            try {
                bluetoothLeService.readDescriptor(characteristicUUID, descriptorUUID)
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowSnackbar("${e.message}"))
            }
        }
    }

    fun writeDescriptor(characteristicUUID: UUID, descriptorUUID: UUID, value: ByteArray) {
        viewModelScope.launch {
            try {
                bluetoothLeService.writeDescriptor(characteristicUUID, descriptorUUID, value)
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowSnackbar("${e.message}"))
            }
        }
    }


    fun enableCharacteristicNotifications(characteristicUUID: UUID) {
        viewModelScope.launch {
            try {
                bluetoothLeService.enableCharacteristicNotifications(characteristicUUID)
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowSnackbar("${e.message}"))
            }
        }
    }

    fun disconnectFromDevice() {
        viewModelScope.launch {
            try {
                bluetoothLeService.disconnectFromGatt()
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowSnackbar("${e.message}"))
            }
        }
    }

    private fun autoReadTargetCharacteristic() {
        viewModelScope.launch {
            delay(1000) // Небольшая задержка после подключения
            try {
                bluetoothLeService.readCharacteristic(TARGET_CHARACTERISTIC_UUID)
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Auto-read failed: ${e.message}"))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothLeService.disconnectFromGatt()
    }

}

sealed interface ControlUiState {
    data class Data(
        val deviceName: String,
        val deviceAddress: String,
        val connectionState: Int,
        val deviceServices: List<CustomGattService>
    ) :
        ControlUiState

    data object Loading : ControlUiState
}

sealed interface UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent
}
