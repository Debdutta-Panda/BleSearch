package com.debduttapanda.blesearch

import android.annotation.SuppressLint
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
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat



class Ble(private val context: ComponentActivity, private val callback: Callback){
    abstract class Callback{
        abstract fun onBluetoothStateChange(it: Int)
        abstract fun onPermissionResult(it: Map<String, @JvmSuppressWildcards Boolean>)

    }
    private val bluetoothStateReceiver = BluetoothStateReceiver {
        onBluetoothStateChanged(it)
    }

    fun close(){
        context.unregisterReceiver(bluetoothStateReceiver)
    }

    private fun onBluetoothStateChanged(it: Int) {
        callback.onBluetoothStateChange(it)
    }

    init {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bluetoothStateReceiver, filter)
    }
    private val requestPermissionLauncher =
        context.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            onPermissionResult(it)
        }
    private val requestBluetoothEnable =
        context.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            onBluetoothEnableRequestResponse(it.resultCode==-1)
        }

    private fun requestEnableBluetooth(){
        requestBluetoothEnable.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }

    private fun onBluetoothEnableRequestResponse(allowed: Boolean) {

    }

    private fun onPermissionResult(permissionMap :Map<String,Boolean>){
        
    }
    private fun requestPermissions(){
        requestPermissionLauncher.launch(requiredPermissions())
    }
    private fun checkPermissions(): Map<String, Boolean> {
        return requiredPermissions()
            .associateWith { 
                ContextCompat.checkSelfPermission(context,it) == PackageManager.PERMISSION_GRANTED 
            }
    }
    private fun bluetoothSupported(): Boolean{
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
    }
    private fun bleSupported(): Boolean{
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }
    private fun adapterEnabled() = bluetoothAdapter.isEnabled



    private fun requiredPermissions(): Array<String>{
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
        return permissions.toTypedArray()
    }
    /////////////////////////
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
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
                
            }
        }
    }
    @SuppressLint("MissingPermission")
    private fun startBleScan() {
        bleScanner.startScan(null, scanSettings, scanCallback)
    }


    @SuppressLint("MissingPermission")
    private fun stopScan(){
        bleScanner.stopScan(scanCallback)
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
}