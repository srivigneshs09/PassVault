package com.example.passvault

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.passvault.databinding.ActivityAddCredentialBinding

class AddCredentialActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddCredentialBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCredentialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val masterKey = MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        val prefs = EncryptedSharedPreferences.create(
            this,
            "secure_store",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        binding.saveBtn.setOnClickListener {
            val title = binding.titleInput.text.toString()
            val uname = binding.usernameInput.text.toString()
            val pwd = binding.passwordInput.text.toString()

            if (title.isNotEmpty() && uname.isNotEmpty() && pwd.isNotEmpty()) {
                prefs.edit().putString("$title-user", uname)
                    .putString("$title-pass", pwd).apply()
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
