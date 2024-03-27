package com.example.mybletest

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.mybletest.databinding.ActivitySubBinding
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class SubActivity : AppCompatActivity() {

    private lateinit var deviceAddress: String
    private lateinit var textView: TextView
    private lateinit var sendButton: Button
    private lateinit var editText: EditText
    private lateinit var cameraButton : Button
    private lateinit var imgButton : Button
    private lateinit var imageView: ImageView

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothSocket: BluetoothSocket? = null
    private lateinit var outputStream: OutputStream
    private lateinit var inputStream: InputStream

    private val SERVICE_UUID: UUID = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee") // SPP UUID

    val binding by lazy { ActivitySubBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Intent로부터 deviceAddress 가져오기
        deviceAddress = intent.getStringExtra("deviceAddress")!!

        textView = findViewById(R.id.textview3)
        editText = findViewById(R.id.textview2)
        imageView = findViewById(R.id.imageView)

        sendButton = findViewById(R.id.sendButton)
        cameraButton = findViewById(R.id.cameraButton)
        imgButton = findViewById(R.id.imgButton)

        sendButton.setOnClickListener {
            val message = editText.text.toString()
            sendBluetoothMessage(message)
        }

        binding.backButton.setOnClickListener {
            // 블루투스 연결 끊기
            disconnectBluetooth()
            // 현재 액티비티 종료
            finish()
        }

        cameraButton.setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
        }

        imgButton.setOnClickListener {
            val imgIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imgIntent.type = "image/*"
            startActivityForResult(imgIntent, PICK_IMAGE_REQUEST)
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
        connectBluetooth(device)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun disconnectBluetooth() {
        try {
            bluetoothSocket?.close()
            bluetoothSocket = null
            Log.d(TAG, "Bluetooth disconnected.")
            textView.text = "Bluetooth disconnected."
        } catch (e: IOException) {
            Log.e(TAG, "Error closing Bluetooth socket.", e)
        }
    }

    private fun connectBluetooth(device: BluetoothDevice) {
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
            bluetoothSocket?.connect()
            Log.d(TAG, "Bluetooth connected. Device is $deviceAddress")
            textView.text = "Bluetooth connected."

            outputStream = bluetoothSocket!!.outputStream
            inputStream = bluetoothSocket!!.inputStream

            // 데이터 수신을 위한 스레드 시작
            Thread {
                val buffer = ByteArray(1024)
                var bytes: Int
                try {
                    while (true) {
                        bytes = inputStream.read(buffer)
                        if (bytes == -1) {
                            Log.d(TAG, "End of stream reached. Closing connection.")
                            break
                        }
                        val incomingMessage = String(buffer, 0, bytes)
                        Log.d(TAG, "InputStream: $incomingMessage")
                        runOnUiThread {
                            // 수신된 데이터를 UI에 표시하거나 필요한 작업 수행
                        }
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Error reading from InputStream. Closing connection.", e)
                } finally {
                    disconnectBluetooth()
                }
            }.start()

        } catch (e: IOException) {
            Log.e(TAG, "Error connecting to Bluetooth device.", e)
            textView.text = "Error connecting to Bluetooth device."
        }
    }

    private fun sendBluetoothMessage(message: String) {
        try {
            outputStream.write(message.toByteArray())
            Log.d(TAG, "Message sent: $message")
            editText.text.clear()
        } catch (e: IOException) {
            Log.e(TAG, "Error sending message.", e)
            textView.text = "Error sending message."
        }
    }

   /* override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            sendImage(imageBitmap)
            // 이미지를 표시합니다.
            imageView.setImageBitmap(imageBitmap)
            imageView.visibility = View.VISIBLE
        }
        else if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap

            // 이미지를 표시합니다.
            imageView.setImageBitmap(imageBitmap)
            imageView.visibility = View.VISIBLE
        }
    }*/

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {;'/'
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as? Bitmap
                    if (imageBitmap != null) {
                        sendImage(imageBitmap)
                        imageView.setImageBitmap(imageBitmap)
                        imageView.visibility = View.VISIBLE
                    } else {
                        // 이미지가 "data" 키에 포함되지 않은 경우
                        val imageUri = data?.data
                        if (imageUri != null) {
                            try {
                                val inputStream = contentResolver.openInputStream(imageUri)
                                val selectedImage = BitmapFactory.decodeStream(inputStream)
                                inputStream?.close()
                                sendImage(selectedImage)
                                imageView.setImageBitmap(selectedImage)
                                imageView.visibility = View.VISIBLE
                            } catch (e: IOException) {
                                Log.e(TAG, "Error loading image from URI: $imageUri", e)
                            }
                        } else {
                            Log.e(TAG, "Captured image bitmap is null.")
                        }
                    }
                }
                PICK_IMAGE_REQUEST -> {
                    data?.data?.let { uri ->
                        try {
                            val imageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                            sendImage(imageBitmap)
                            imageView.setImageBitmap(imageBitmap)
                            imageView.visibility = View.VISIBLE
                        } catch (e: IOException) {
                            Log.e(TAG, "Error getting image from gallery.", e)
                        }
                    } ?: run {
                        Log.e(TAG, "Selected image URI is null.")
                    }
                }
            }
        }
    }

    private fun sendImage(bitmap: Bitmap){
        try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            outputStream.write("IMG_START".toByteArray())
            outputStream.write(byteArray)
            outputStream.flush()
            outputStream.write("IMG_END".toByteArray())
            Log.d(TAG, "Image sent successfully.")

            imageView.setImageBitmap(bitmap)
            imageView.visibility = View.VISIBLE

        } catch (e: IOException) {
            Log.e(TAG, "Error sending image.", e)
        }
    }

    /*private fun sendImage(bitmap: Bitmap) {
        try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            val blockSize = 1024 // 작은 블록 크기
            val totalBlocks = byteArray.size / blockSize
            val lastBlockSize = byteArray.size % blockSize

            // 이미지 데이터 전송 시작 신호 전송
            outputStream.write("IMG_START".toByteArray())

            // 작은 블록 단위로 이미지 데이터 전송
            for (i in 0 until totalBlocks) {
                val blockStart = i * blockSize
                val blockEnd = blockStart + blockSize
                val blockData = byteArray.copyOfRange(blockStart, blockEnd)
                sendBlock(blockData)
            }

            // 나머지 블록 전송
            if (lastBlockSize > 0) {
                val lastBlockData = byteArray.copyOfRange(byteArray.size - lastBlockSize, byteArray.size)
                sendBlock(lastBlockData)
            }

            // 이미지 데이터 전송 종료 신호 전송
            outputStream.write("END".toByteArray())

            Log.d(TAG, "Image sent successfully.")

            imageView.setImageBitmap(bitmap)
            imageView.visibility = View.VISIBLE

        } catch (e: IOException) {
            Log.e(TAG, "Error sending image.", e)
        }
    }*/

    private fun sendBlock(blockData: ByteArray) {
        Thread {
            try {
                outputStream.write(blockData)
                outputStream.flush()
            } catch (e: IOException) {
                Log.e(TAG, "Error sending image block.", e)
            }
        }.start()
    }

    companion object {
        private const val TAG = "SubActivity"
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val PICK_IMAGE_REQUEST = 1
    }
}