package com.apextv.app.activities

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.apextv.app.R
import com.apextv.app.fragments.VodFragment

class VodActivity : AppCompatActivity() {

    companion object {
        const val TYPE_MOVIES = "movies"
        const val TYPE_SERIES = "series"
        const val EXTRA_TYPE = "type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vod)

        val type = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_MOVIES
        findViewById<android.widget.TextView>(R.id.tvTitle)?.text =
            if (type == TYPE_MOVIES) "🎬 Películas" else "📺 Series"
        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }

        val fragment = VodFragment()
        fragment.onLoaded = {
            findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmerVod)?.stopShimmer()
            findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmerVod)?.visibility = View.GONE
        }
        fragment.onCategoriesLoaded = { categories ->
            runOnUiThread {
                val container = findViewById<android.widget.LinearLayout>(R.id.filterContainer)
                container?.removeAllViews()
                categories.forEach { (cat, rowIndex) ->
                    val btn = android.widget.Button(this).apply {
                        text = cat; textSize = 11f
                        setTextColor(getColor(R.color.text_secondary))
                        setBackgroundColor(getColor(R.color.surface2))
                        isFocusable = true; setPadding(20, 0, 20, 0)
                        val params = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
                        params.marginEnd = 8; layoutParams = params
                        setOnClickListener { fragment.setSelectedPosition(rowIndex, true) }
                        setOnFocusChangeListener { v, focused ->
                            (v as android.widget.Button).setTextColor(
                                if (focused) getColor(R.color.background) else getColor(R.color.text_secondary))
                            v.setBackgroundColor(
                                if (focused) getColor(R.color.primary) else getColor(R.color.surface2))
                        }
                    }
                    container?.addView(btn)
                }
            }
        }
        findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmerVod)?.startShimmer()
        supportFragmentManager.beginTransaction()
            .replace(R.id.rvVod, fragment)
            .runOnCommit { fragment.load(type) }
            .commit()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true }
        return super.onKeyDown(keyCode, event)
    }
}
