package com.example.passvault

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.passvault.databinding.ActivityMainBinding
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: CredentialAdapter
    private lateinit var encryptedPrefs: EncryptedSharedPreferences
    private val credentials = mutableListOf<Credential>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authenticateApp {
            setupUI()
        }
    }

    private fun authenticateApp(onSuccess: () -> Unit) {
        val biometricManager = BiometricManager.from(this)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            val executor: Executor = ContextCompat.getMainExecutor(this)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authenticate")
                .setSubtitle("Access your password vault")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                ).build()

            val biometricPrompt = BiometricPrompt(this, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        finish()
                    }
                })

            biometricPrompt.authenticate(promptInfo)
        } else {
            Toast.makeText(this, "Biometric/PIN not available", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupUI() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val masterKey = MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        encryptedPrefs = EncryptedSharedPreferences.create(
            this,
            "secure_store",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences

        adapter = CredentialAdapter(credentials) { credential ->
            authenticateItem {
                Toast.makeText(this, "Username: ${credential.username}\nPassword: ${credential.password}", Toast.LENGTH_LONG).show()
            }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        loadCredentials()

        binding.fab.setOnClickListener {
            startActivity(Intent(this, AddCredentialActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (::encryptedPrefs.isInitialized) {
            credentials.clear()
            loadCredentials()
        }
    }

    private fun authenticateItem(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authenticate to view details")
            .setSubtitle("Biometric or PIN required")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }

    private fun loadCredentials() {
        encryptedPrefs.all.forEach { entry ->
            if (entry.key.endsWith("-user")) {
                val title = entry.key.removeSuffix("-user")
                val username = entry.value as String
                val password = encryptedPrefs.getString("$title-pass", "") ?: ""
                credentials.add(Credential(title, username, password))
            }
        }
        adapter.notifyDataSetChanged()
    }
}
