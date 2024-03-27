package com.example.mybletest

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mybletest.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var deviceListView: ListView
    private lateinit var scanButton: Button
    private lateinit var devicesAdapter: ArrayAdapter<String>
    private val devicesList = mutableListOf<String>()
    var bluetoothGatt: BluetoothGatt? = null // BluetoothGatt 인스턴스 선언

    val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "이 디바이스는 Bluetooth를 지원하지 않습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        deviceListView = binding.deviceListView
        scanButton = binding.scanButton

        devicesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, devicesList)
        deviceListView.adapter = devicesAdapter

        deviceListView.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = devicesAdapter.getItem(position)

            if (selectedItem != null) {
                val deviceInfo = selectedItem.split(":")
                val deviceSize = deviceInfo.size

                Log.d(TAG, "THIS: $deviceInfo, size: $deviceSize")
                if (deviceSize == 7) {
                    val deviceAddress = deviceInfo.subList(1, 7).joinToString(separator = ":").trim()

                    val intent = Intent(this, SubActivity::class.java)
                    intent.putExtra("deviceAddress", deviceAddress)
                    startActivity(intent)

                } else {
                    Log.e(TAG, "Invalid device info format: $selectedItem")
                    Toast.makeText(this, "Invalid device info", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e(TAG, "Selected item is null")
            }
        }

        scanButton.setOnClickListener {
            checkBluetoothPermission()
        }
    }
/*
    private fun connectBluetooth(deviceAddress: String): BluetoothGatt? {
        val device: BluetoothDevice? = bluetoothAdapter.getRemoteDevice(deviceAddress)
        if (device == null) {
            Log.e(TAG, "Device not found. Unable to connect.")
            return null
        }

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Connected to GATT server.")
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "Disconnected from GATT server.")
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // 서비스 발견 시 동작할 코드 작성
                } else {
                    Log.e(TAG, "onServicesDiscovered received: $status")
                }
            }
        }
        return device.connectGatt(this, false, gattCallback)
    }*/

    private fun checkBluetoothPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_CODE
            )
        } else {
            startBluetoothScan()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBluetoothScan()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startBluetoothScan() {
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        devicesList.clear()
        devicesAdapter.notifyDataSetChanged()

        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        registerReceiver(
            bluetoothReceiver,
            IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        )
        bluetoothAdapter.startDiscovery()
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    if (device.name?.startsWith("ras") == true) {
                        devicesList.add("${device.name ?: "Unknown Device"}: ${device.address}")
                        devicesAdapter.notifyDataSetChanged()
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                if (devicesList.isEmpty()) {
                    Toast.makeText(context, "근처에 장치가 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val TAG = "MainActivity"
    }
}
