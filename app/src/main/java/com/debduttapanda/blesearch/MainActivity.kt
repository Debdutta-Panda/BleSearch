package com.debduttapanda.blesearch

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.debduttapanda.blesearch.ui.theme.BleSearchTheme
/*import com.welie.blessed.BluetoothCentralManager
import com.welie.blessed.BluetoothCentralManagerCallback
import com.welie.blessed.BluetoothPeripheral*/
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
/*import no.nordicsemi.android.kotlin.ble.core.scanner.BleScanFilter
import no.nordicsemi.android.kotlin.ble.core.scanner.BleScannerCallbackType
import no.nordicsemi.android.kotlin.ble.core.scanner.BleScannerSettings
import no.nordicsemi.android.kotlin.ble.core.scanner.FilteredServiceUuid
import no.nordicsemi.android.kotlin.ble.scanner.BleScanner*/




class MainActivity : ComponentActivity() {
    private val ble = Ble(this)
    /////////
    private val devices = mutableStateMapOf<String,String>()
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            with(result.device) {
                devices[address] = name?:"No name"
                Log.i("fkldjfd", "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
            }
        }
    }
    @SuppressLint("MissingPermission")
    private fun startBleScan() {
        bleScanner.startScan(null, scanSettings, scanCallback)
    }
    private val toRequest = mutableStateOf("")
    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("fkldjfd", "${toRequest.value}:$isGranted")
                toRequest.value = ""
                startProcess()
            } else {
                Log.d("fkldjfd", "${toRequest.value}:denied")
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the receiver to avoid memory leaks
        unregisterReceiver(bluetoothStateReceiver)
    }

    private var bluetoothStateReceiver: BluetoothStateReceiver? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothStateReceiver = BluetoothStateReceiver {
            Log.d("fkldjfd", "$it")
            BluetoothAdapter.STATE_OFF
        }
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateReceiver, filter)
        setContent {
            BleSearchTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        Button(onClick = {
                            startProcess()
                        }) {
                            Text("Process")
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                        ){
                            items(devices.keys.toList()){
                                Text(it)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startProcess() {
        ensureBluetoothExists()
    }

    private fun ensureBluetoothExists(): Any {
        val hasBluetooth = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        if (!hasBluetooth) {
            return Log.d("fkldjfd", "Bluetooth not supported")
        }
        val hasBle = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        if (!hasBle) {
            return Log.d("fkldjfd", "BLE not supported")
        }
        Log.d("fkldjfd", "Required Bluetooth features supported")
        return checkPermissions()
    }

    private fun ensureBluetoothEnabled(): Any {
        val adapter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
            bluetoothManager.adapter
        } else {
            BluetoothAdapter.getDefaultAdapter()
        }
        return if (adapter.isEnabled) {
            scan()
        } else {
            enableBluetooth(adapter)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableBluetooth(adapter: BluetoothAdapter): Any {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, 100)
        return true
    }

    /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 || resultCode == -1) {
            scan()
        }
    }*/

    private fun checkPermissions() {
        val permissions = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        if (Build.VERSION.SDK_INT > 30) {
            permissions.add(android.Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(android.Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(android.Manifest.permission.BLUETOOTH)
            permissions.add(android.Manifest.permission.BLUETOOTH_ADMIN)
        }
        for (p in permissions) {
            val result = ContextCompat.checkSelfPermission(this, p)
            if (result != PackageManager.PERMISSION_GRANTED) {
                toRequest.value = p
                requestPermissionLauncher.launch(p)
                break
            }
        }
        ensureBluetoothEnabled()
    }

    @SuppressLint("MissingPermission")
    private fun scan() {
        startBleScan()
        /*val centralManagerCallback = object : BluetoothCentralManagerCallback() {
            override fun onDiscovered(peripheral: BluetoothPeripheral, scanResult: ScanResult) {
                Log.d("fkldjfd","${peripheral.address}:${scanResult.scanRecord?.serviceUuids}")
            }
        }
        val central = BluetoothCentralManager(getApplicationContext(), centralManagerCallback, Handler(Looper.getMainLooper()))
        central.scanForPeripherals()*/
        /*BleScanner(this)
            .scan(
                //settings = BleScannerSettings(callbackType = BleScannerCallbackType.CALLBACK_TYPE_FIRST_MATCH)
            )
            .onEach {
                Log.d("fkldjfd", "${it.device.address}:${it.data?.txPower}:${it.data?.scanRecord?.serviceUuids}")
            }
            .launchIn(lifecycleScope)*/
    }
}

class BluetoothStateReceiver(private val callback: (state: Int) -> Unit) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action: String? = intent?.action
        if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
            val state: Int =
                intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            callback(state)
        }
    }
}