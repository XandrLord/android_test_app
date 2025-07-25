package com.davidrevolt.feature.home


import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidrevolt.core.ble.model.CustomScanResult
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun HomeScreen(
    onConnectClick: (deviceName: String, deviceAddress: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.homeUiState.collectAsStateWithLifecycle()
    val startBluetoothLeScan = viewModel::startBluetoothLeScan
    val stopBluetoothLeScan = viewModel::stopBluetoothLeScan

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
        is HomeUiState.Data -> {
            val data = (uiState as HomeUiState.Data)
            HomeScreenContent(
                isScanning = data.isScanning,
                scanResults = data.scanResults,
                startBluetoothLeScan = startBluetoothLeScan,
                stopBluetoothLeScan = stopBluetoothLeScan,
                onConnectClick = onConnectClick,
                snackbarHostState = snackbarHostState
            )
        }

        is HomeUiState.Loading -> Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) { CircularProgressIndicator() }
    }

}


@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun HomeScreenContent(
    isScanning: Boolean,
    scanResults: List<CustomScanResult>,
    startBluetoothLeScan: () -> Unit,
    stopBluetoothLeScan: () -> Unit,
    onConnectClick: (deviceName: String, deviceAddress: String) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val bluetoothAdapter: BluetoothAdapter? =
        getSystemService(LocalContext.current, BluetoothManager::class.java)?.adapter

    val blePermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    )
    // Enable Bluetooth Intent
    val onBluetoothEnableLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // If Bluetooth enabled: Activate scan Method HERE!
            if (result.resultCode == Activity.RESULT_OK) {
                startBluetoothLeScan.invoke()
            }
        }

    // Request BLE Permissions Intent
    val onBlePermissionsGrantedLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            if (blePermissionsState.allPermissionsGranted) {
                // If Permission Granted: Activate scan Method HERE!
                if (bluetoothAdapter == null) {
                    Toast.makeText(context, "Device doesn't support Bluetooth", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    //Activate Bluetooth
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    onBluetoothEnableLauncher.launch(enableBtIntent) // GO TO Enable Bluetooth Intent
                }
            }
        }

    val onPermissionsCheckAndScan =
        { // Request BLE Permissions -> Request to Enable Bluetooth -> SCAN!
            onBlePermissionsGrantedLauncher.launch(blePermissionsState.permissions.map { it.permission }
                .toTypedArray())
        }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.safeDrawingPadding()
            )
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(
                onClick = if (isScanning) stopBluetoothLeScan else onPermissionsCheckAndScan,
                containerColor = if (isScanning) MaterialTheme.colorScheme.error else FloatingActionButtonDefaults.containerColor,
                modifier = Modifier
                    .padding(16.dp)
                    .size(70.dp),
                shape = CircleShape
            ) {
                Icon(
                    if (isScanning) Icons.Rounded.Close else Icons.Rounded.Search,
                    "Floating action button",
                    modifier = Modifier.size(36.dp)
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            // Home Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BLE Scanner",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    if (isScanning) {
                        Text(
                            text = "Scanning...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            // Permissions Rationale Section
            if (!blePermissionsState.allPermissionsGranted) {
                PermissionsRationale(
                    blePermissionsState = blePermissionsState,
                    onSettingsClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        ContextCompat.startActivity(context, intent, null)
                    },
                    modifier = Modifier.padding(16.dp)
                )
            } else if (!blePermissionsState.allPermissionsGranted && !blePermissionsState.shouldShowRationale)
                Text("You should allow permissions...")

            if (scanResults.isEmpty() && !isScanning) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    scanResults.forEach { result ->
                        item {
                            ScanResultItem(
                                scanResult = result,
                                onConnectClick = { onConnectClick(result.name, result.address) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScanResultItem(
    scanResult: CustomScanResult,
    onConnectClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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
                contentDescription = "Device",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = scanResult.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = scanResult.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = scanResult.manufacturer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "RSSI: ${scanResult.rssi} dBm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onConnectClick,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Connect")
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.wifi),
            contentDescription = "No devices",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No devices found",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Press the scan button to find BLE devices",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsRationale(
    blePermissionsState: MultiplePermissionsState,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Permission warning",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (blePermissionsState.shouldShowRationale) {
                Text(
                    text = "Permissions Required",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Bluetooth permissions were denied before. Please grant them in settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onSettingsClick,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Go to Settings")
                }
            } else {
                Text(
                    text = "Permissions Needed",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This app requires Bluetooth permissions to scan for devices. Please allow them.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { blePermissionsState.launchMultiplePermissionRequest() },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Request Permissions")
                }
            }
        }
    }
}