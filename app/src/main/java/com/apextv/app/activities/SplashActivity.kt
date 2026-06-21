package com.apextv.app.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.apextv.app.utils.Prefs
import kotlinx.coroutines.*

class SplashActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.apextv.app.services.ApiService.appContext = applicationContext
        scope.launch {
            delay(1500)
            if (Prefs.isLoggedIn(this@SplashActivity)) {
                com.apextv.app.services.ApiService.token = Prefs.getToken(this@SplashActivity)
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            } else {
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            }
            finish()
        }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
