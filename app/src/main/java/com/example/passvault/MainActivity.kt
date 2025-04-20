package com.example.passvault

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
            try {
                setupUI()
            } catch (e: Exception) {
                Toast.makeText(this, "Error setting up UI: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
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
                )
                .build()

            val biometricPrompt = BiometricPrompt(this@MainActivity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        Toast.makeText(this@MainActivity, "Authentication error: $errString", Toast.LENGTH_LONG).show()
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

        adapter = CredentialAdapter(
            credentials,
            onItemClick = { credential -> showCredential(credential) },
            onItemLongClick = { credential -> authenticateThenDelete(credential) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        loadCredentials()

        binding.addFab.setOnClickListener {
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

    private fun showCredential(credential: Credential) {
        val executor = ContextCompat.getMainExecutor(this)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authenticate to view")
            .setSubtitle(credential.title)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val biometricPrompt = BiometricPrompt(this@MainActivity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(credential.title)
                        .setMessage("Username: ${credential.username}\nPassword: ${credential.password}")
                        .setPositiveButton("OK", null)
                        .show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@MainActivity, "Authentication error: $errString", Toast.LENGTH_LONG).show()
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }

    private fun authenticateThenDelete(credential: Credential) {
        val executor = ContextCompat.getMainExecutor(this)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authenticate to delete")
            .setSubtitle(credential.title)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val biometricPrompt = BiometricPrompt(this@MainActivity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    showDeleteDialog(credential)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@MainActivity, "Authentication error: $errString", Toast.LENGTH_LONG).show()
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }

    private fun showDeleteDialog(credential: Credential) {
        AlertDialog.Builder(this)
            .setTitle("Delete '${credential.title}'?")
            .setMessage("Are you sure you want to delete this credential?")
            .setPositiveButton("Delete") { _, _ ->
                encryptedPrefs.edit()
                    .remove("${credential.title}-user")
                    .remove("${credential.title}-pass")
                    .apply()
                loadCredentials()
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadCredentials() {
        try {
            credentials.clear()
            encryptedPrefs.all.forEach { entry ->
                if (entry.key.endsWith("-user")) {
                    val title = entry.key.removeSuffix("-user")
                    val username = entry.value as String
                    val password = encryptedPrefs.getString("$title-pass", "") ?: ""
                    credentials.add(Credential(title, username, password))
                }
            }
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading credentials: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}