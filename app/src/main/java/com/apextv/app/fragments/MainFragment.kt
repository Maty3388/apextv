package com.apextv.app.fragments

import android.content.Intent
import android.os.Bundle
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.*
import com.bumptech.glide.Glide
import com.apextv.app.activities.PlayerActivity
import com.apextv.app.models.Channel
import com.apextv.app.services.ApiService
import kotlinx.coroutines.*

class MainFragment : RowsSupportFragment() {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var allChannels = listOf<Channel>()
    var onChannelsLoaded: (() -> Unit)? = null
    private val catOrder = listOf(
        "MUNDIAL 2026","EVENTOS","ARGENTINA","ARGENTINA INTERIOR","ARGENTINA 2",
        "DEPORTES","DEPORTES 2","NOTICIAS","NOTICIAS 2","MÚSICA","MÚSICA 2",
        "RELIGIÓN","INFANTILES","INFANTILES 2","CANALES 24/7","CANALES 24/7 2",
        "CINE","CINE 2","SERIES","SERIES 2","INTERNACIONAL","INTERNACIONAL 2",
        "COLOMBIA","CHILE","MEXICO","BRASIL","URUGUAY","DOCUMENTALES","PLUTOTV"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, row ->
            if (item is Channel) {
                val rowAdapter = (row as ListRow).adapter as ArrayObjectAdapter
                val list = (0 until rowAdapter.size()).map { rowAdapter.get(it) as Channel }
                val idx = list.indexOf(item).coerceAtLeast(0)
                startActivity(Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra(PlayerActivity.EXTRA_CHANNELS, ArrayList(list))
                    putExtra(PlayerActivity.EXTRA_INDEX, idx)
                })
            }
        }
        loadChannels()
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setOnKeyListener { _, keyCode, event ->
            keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT &&
            event.action == android.view.KeyEvent.ACTION_DOWN
        }
    }

    fun loadFavorites() {
        scope.launch {
            val favs = withContext(Dispatchers.IO) {
                try { ApiService.getFavorites() } catch (_: Exception) { emptyList() }
            }
            if (!isAdded) return@launch
            if (favs.isEmpty()) {
                val rowsAdapter = ArrayObjectAdapter(ListRowPresenter(FocusHighlight.ZOOM_FACTOR_SMALL).apply {
                    shadowEnabled = false; selectEffectEnabled = false
                })
                val adapter = ArrayObjectAdapter(ChannelPresenter())
                rowsAdapter.add(ListRow(HeaderItem("Sin favoritos"), adapter))
                this@MainFragment.adapter = rowsAdapter
            } else buildRows(favs)
        }
    }

    fun filterCategory(category: String?) {
        val filtered = if (category == null) allChannels
                       else allChannels.filter { it.category == category || it.category == "$category 2" }
        buildRows(filtered)
    }

    fun loadChannels() {
        scope.launch {
            val cached = withContext(Dispatchers.IO) {
                try { ApiService.getCachedChannels() } catch (_: Exception) { emptyList() }
            }
            if (cached.isNotEmpty() && isAdded) {
                allChannels = cached
                buildRows(allChannels)
            }
            val fresh = withContext(Dispatchers.IO) {
                try { ApiService.getChannels() } catch (_: Exception) { emptyList() }
            }
            if (!isAdded) return@launch
            if (fresh.isNotEmpty()) {
                allChannels = fresh
                buildRows(allChannels)
                onChannelsLoaded?.invoke()
            } else if (allChannels.isNotEmpty()) {
                onChannelsLoaded?.invoke()
            }
        }
    }

    private fun buildRows(channels: List<Channel>) {
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter(FocusHighlight.ZOOM_FACTOR_SMALL).apply {
            shadowEnabled = false; selectEffectEnabled = false
        })
        val grouped = channels.filter { it.category != "ADULTOS" }.groupBy { it.category }
        val sorted = catOrder.mapNotNull { grouped[it]?.let { chs -> it to chs } } +
                     grouped.filter { it.key !in catOrder }.map { it.key to it.value }
        sorted.forEach { (cat, chs) ->
            val adapter = ArrayObjectAdapter(ChannelPresenter())
            chs.sortedBy { it.number }.forEach { adapter.add(it) }
            rowsAdapter.add(ListRow(HeaderItem(cat), adapter))
        }
        adapter = rowsAdapter
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}

class ChannelPresenter : Presenter() {
    private val catColors = mapOf(
        "MUNDIAL 2026" to Pair(0xFF1B6B1B.toInt(), 0xFF0A3A0A.toInt()),
        "EVENTOS" to Pair(0xFFB8620A.toInt(), 0xFF6B3500.toInt()),
        "ARGENTINA" to Pair(0xFF1565C0.toInt(), 0xFF0A3A7A.toInt()),
        "DEPORTES" to Pair(0xFFB71C1C.toInt(), 0xFF6A0000.toInt()),
        "NOTICIAS" to Pair(0xFF283593.toInt(), 0xFF0D1660.toInt()),
        "MÚSICA" to Pair(0xFF6A1B9A.toInt(), 0xFF380060.toInt()),
        "INFANTILES" to Pair(0xFFE65100.toInt(), 0xFF8B3000.toInt()),
        "CINE" to Pair(0xFF4A148C.toInt(), 0xFF1A0050.toInt()),
        "SERIES" to Pair(0xFF00695C.toInt(), 0xFF003830.toInt()),
        "INTERNACIONAL" to Pair(0xFF1565A0.toInt(), 0xFF083060.toInt()),
        "DOCUMENTALES" to Pair(0xFF5D4037.toInt(), 0xFF2E1A0A.toInt()),
    )

    override fun onCreateViewHolder(parent: android.view.ViewGroup): ViewHolder {
        val ctx = parent.context
        val dp = ctx.resources.displayMetrics.density
        val W = (160*dp).toInt(); val H = (130*dp).toInt()
        val card = android.widget.FrameLayout(ctx).apply {
            layoutParams = android.view.ViewGroup.MarginLayoutParams(W, H).apply {
                setMargins((5*dp).toInt(), (5*dp).toInt(), (5*dp).toInt(), (5*dp).toInt())
            }
            isFocusable = true; isFocusableInTouchMode = true
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 12*dp; setColor(0xFF0F0F1A.toInt())
            }
            elevation = 4*dp
        }
        val logo = android.widget.ImageView(ctx).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                (130*dp).toInt(), (85*dp).toInt()).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL or android.view.Gravity.TOP
                topMargin = (6*dp).toInt()
            }
            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE; tag = "logo"
        }
        val nameBar = android.widget.FrameLayout(ctx).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, (36*dp).toInt()).apply {
                gravity = android.view.Gravity.BOTTOM
            }
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadii = floatArrayOf(0f,0f,0f,0f,12*dp,12*dp,12*dp,12*dp)
                setColor(0xCC000000.toInt())
            }
        }
        val name = android.widget.TextView(ctx).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT).apply {
                marginStart = (8*dp).toInt(); marginEnd = (8*dp).toInt()
            }
            setTextColor(0xFFE8E8E8.toInt()); textSize = 10f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
            gravity = android.view.Gravity.CENTER_VERTICAL; tag = "name"
        }
        val liveBadge = android.widget.TextView(ctx).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.END
                marginEnd = (4*dp).toInt()
            }
            text = "● VIVO"; textSize = 7f
            setTextColor(0xFFFF4444.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD; tag = "live"
        }
        nameBar.addView(name); nameBar.addView(liveBadge)
        card.addView(logo); card.addView(nameBar)
        return ViewHolder(card)
    }

    override fun onBindViewHolder(vh: ViewHolder, item: Any) {
        val ch = item as Channel
        val card = vh.view as android.widget.FrameLayout
        val ctx = card.context
        val dp = ctx.resources.displayMetrics.density
        card.findViewWithTag<android.widget.TextView>("name")?.text = ch.name
        val (colorTop, colorBot) = catColors[ch.category] ?: Pair(0xFF0F0F1A.toInt(), 0xFF080810.toInt())
        card.background = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(colorTop, colorBot)).apply { cornerRadius = 12*dp }
        val logo = card.findViewWithTag<android.widget.ImageView>("logo")
        logo?.setImageDrawable(null)
        if (ch.logoUrl.isNotEmpty()) Glide.with(card).load(ch.logoUrl).into(logo!!)
        val liveBadge = card.findViewWithTag<android.widget.TextView>("live")
        liveBadge?.clearAnimation()
        val anim = android.view.animation.AlphaAnimation(1f, 0.2f).apply {
            duration = 800; repeatMode = android.view.animation.Animation.REVERSE
            repeatCount = android.view.animation.Animation.INFINITE
        }
        liveBadge?.startAnimation(anim)
        card.setOnFocusChangeListener { v, focused ->
            val (ct, cb) = catColors[ch.category] ?: Pair(0xFF0F0F1A.toInt(), 0xFF080810.toInt())
            v.background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(ct, cb)).apply {
                cornerRadius = 12*dp
                if (focused) setStroke((3*dp).toInt(), 0xFFC9A84C.toInt())
            }
            v.animate().scaleX(if (focused) 1.08f else 1f).scaleY(if (focused) 1.08f else 1f)
                .translationZ(if (focused) 10f else 0f).setDuration(120).start()
        }
    }

    override fun onUnbindViewHolder(vh: ViewHolder) {
        val card = vh.view as android.widget.FrameLayout
        card.findViewWithTag<android.widget.ImageView>("logo")?.setImageDrawable(null)
        card.findViewWithTag<android.widget.TextView>("live")?.clearAnimation()
    }
}
