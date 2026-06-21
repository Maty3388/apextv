package com.apextv.app.activities

import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.apextv.app.R
import com.apextv.app.services.ApiService
import com.apextv.app.utils.Prefs
import kotlinx.coroutines.*

class AccountActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        findViewById<android.widget.TextView>(R.id.tvEmail)?.text = Prefs.getEmail(this)
        findViewById<android.widget.TextView>(R.id.tvSubEnd)?.text = Prefs.getSubEnd(this).ifEmpty { "Sin datos" }

        findViewById<android.widget.Button>(R.id.btnClose)?.setOnClickListener { finish() }
        findViewById<android.widget.Button>(R.id.btnLogout)?.setOnClickListener {
            Prefs.logout(this)
            startActivity(android.content.Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
        findViewById<android.widget.Button>(R.id.btnPin)?.setOnClickListener {
            Toast.makeText(this, "PIN - próximamente", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
