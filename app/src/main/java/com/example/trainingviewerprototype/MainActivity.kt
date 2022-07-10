package com.example.trainingviewerprototype

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.trainingviewerprototype.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_ENABLE_BT         = 1

        const val BLE_DEVICE_ADDRESS        = "C8:78:44:7D:FE:EF"
        const val BLE_SERVICE_UUID          = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
        const val BLE_CHARACTERISTICS_UUID  = "6e400003-b5a3-f393-e0a9-e50e24dcca9e"
        const val CCD_UUID                  = "00002902-0000-1000-8000-00805f9b34fb"
    }

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothGatt: BluetoothGatt
    private lateinit var characteristic: BluetoothGattCharacteristic

    private lateinit var binding: ActivityMainBinding

    // BLEスキャンコールバック
    private val mScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            checkBlePermission()

            if (result == null) return

            if (result.device.address == BLE_DEVICE_ADDRESS) {
                result.device.connectGatt(this@MainActivity, true, mGattCallBack)
            }
        }
    }

    // GATT接続コールバック
    private val mGattCallBack = object : BluetoothGattCallback() {
        // 接続状態変更時のコールバック
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            checkBlePermission()

            if (gatt == null) return

            if (newState == BluetoothGatt.STATE_CONNECTED) {
                // 接続成功時にはサービス検索
                Log.d("BLE_STATUS", "Connected!")
                this@MainActivity.runOnUiThread {
                    binding.textStatus.text = getString(R.string.status_connect)
                }
                stopScan()
                bluetoothGatt = gatt
                gatt.discoverServices()
            }
            else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                // 切断時にはオブジェクトを空にする
                Log.d("BLE_STATUS", "Connecting lost")
                this@MainActivity.runOnUiThread {
                    binding.textStatus.text = getString(R.string.status_disconnect)
                }
                gatt.close()
            }
        }

        // サービス発見時のコールバック
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            checkBlePermission()

            Log.d("BLE_STATUS", "Service not discovered!")
            if (gatt == null) return

            Log.d("BLE_STATUS", "Service Discovered!")

            // notifierを使用するための設定
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(UUID.fromString(BLE_SERVICE_UUID))
                characteristic = service.getCharacteristic(UUID.fromString(BLE_CHARACTERISTICS_UUID))
                gatt.setCharacteristicNotification(characteristic, true)
                val descriptor = characteristic.getDescriptor(UUID.fromString(CCD_UUID)).apply {
                    value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                }
                gatt.writeDescriptor(descriptor)
            }
        }

        // Characteristic変更時のコールバック
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            if (characteristic == null) return
            val count = byteArrayToInt(characteristic.value)
            Log.d("count", count.toString())
            this@MainActivity.runOnUiThread {
                binding.textCount.text = count.toString()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkBlePermission()

        // ビューバインディング設定
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Bluetooth初期処理
        initBle()

        binding.textStatus.text = getString(R.string.status_disconnect)

        binding.buttonConnect.setOnClickListener {
            bluetoothAdapter.bluetoothLeScanner.startScan(mScanCallback)
        }

        binding.buttonDisconnect.setOnClickListener {
            bluetoothGatt.close()
            bluetoothGatt
        }
    }

    private fun initBle() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    // Bluetooth権限確認
    private fun checkBlePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN), REQUEST_ENABLE_BT)
        }
    }

    private fun byteArrayToInt(byteArray: ByteArray?) : Int {
        byteArray ?: return 0
        return byteArray[0].toInt()
    }

    private fun stopScan() {
        checkBlePermission()
        bluetoothAdapter.bluetoothLeScanner.stopScan(mScanCallback)
    }
}