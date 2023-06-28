package com.example.mensajes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import android.content.Intent
import android.app.Activity
import android.net.Uri
import java.io.InputStreamReader
import java.io.IOException



class MainActivity : AppCompatActivity() {

    private lateinit var editTextPhoneNumbers: EditText
    private lateinit var editTextMessage: EditText
    private lateinit var buttonConfirm: Button
    private lateinit var buttonUpload: Button

    companion object {
        private const val PERMISSION_REQUEST_READ_STORAGE = 456
        private const val PERMISSION_REQUEST_SMS = 123
        private const val PICK_FILE_REQUEST_CODE = 789
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editTextPhoneNumbers = findViewById(R.id.editTextPhoneNumbers)
        editTextMessage = findViewById(R.id.editTextMessage)
        buttonConfirm = findViewById(R.id.buttonConfirm)
        buttonUpload = findViewById(R.id.buttonUpload)

        buttonConfirm.setOnClickListener {
            val phoneNumbers = editTextPhoneNumbers.text.toString()
            val message = editTextMessage.text.toString()

            if (phoneNumbers.isNotEmpty() && message.isNotEmpty()) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.SEND_SMS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    sendSMS(phoneNumbers, message)
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.SEND_SMS),
                        PERMISSION_REQUEST_SMS
                    )
                }
            } else {
                Toast.makeText(
                    this,
                    "Ingresa al menos un número de teléfono y un mensaje",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        buttonUpload.setOnClickListener {
            openFilePicker()
        }

        checkStoragePermission()
    }

    private fun checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_READ_STORAGE
            )
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/json"
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val json = readJsonFromUri(uri)
                if (json != null) {
                    val gson = Gson()
                    val contacts = gson.fromJson(json, Contacts::class.java)

                    val phoneNumbers = contacts.numeros_telefono.joinToString(", ")
                    editTextPhoneNumbers.setText(phoneNumbers)
                } else {
                    Toast.makeText(this, "Error al leer el archivo JSON", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun readJsonFromUri(uri: Uri): String? {
        val contentResolver = contentResolver
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.use {
                it.readText()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun sendSMS(phoneNumbers: String, message: String) {
        val smsManager: SmsManager = SmsManager.getDefault()
        val numbersArray = phoneNumbers.split(",").map { it.trim() }

        val sendResults = mutableListOf<String>()

        for (phoneNumber in numbersArray) {
            try {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                sendResults.add("$phoneNumber: Enviado")
            } catch (e: Exception) {
                sendResults.add("$phoneNumber: Error")
            }
        }

        val resultMessage = sendResults.joinToString("\n")
        Toast.makeText(this, resultMessage, Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_READ_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, do nothing
                } else {
                    Toast.makeText(
                        this,
                        "Permiso denegado para acceder a la memoria interna",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            PERMISSION_REQUEST_SMS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val phoneNumbers = editTextPhoneNumbers.text.toString()
                    val message = editTextMessage.text.toString()
                    sendSMS(phoneNumbers, message)
                } else {
                    Toast.makeText(
                        this,
                        "Permiso denegado para enviar mensajes",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    data class Contacts(val numeros_telefono: List<String>)
}
