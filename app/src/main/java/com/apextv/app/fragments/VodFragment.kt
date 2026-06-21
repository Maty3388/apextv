package com.apextv.app.fragments

import android.content.Intent
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.*
import com.bumptech.glide.Glide
import com.apextv.app.activities.DetailActivity
import com.apextv.app.activities.VodActivity
import com.apextv.app.activities.DetailActivity
import com.apextv.app.models.Movie
import com.apextv.app.models.Serie
import com.apextv.app.services.ApiService
import kotlinx.coroutines.*

class VodFragment : RowsSupportFragment() {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var type = VodActivity.TYPE_MOVIES
    var onCategoriesLoaded: ((List<Pair<String, Int>>) -> Unit)? = null
    var onLoaded: (() -> Unit)? = null

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setOnKeyListener { _, keyCode, event ->
            keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT &&
            event.action == android.view.KeyEvent.ACTION_DOWN
        }
        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            when (item) {
                is Movie -> startActivity(Intent(requireContext(), DetailActivity::class.java).apply {
                    putExtra(DetailActivity.EXTRA_MOVIE, item)
                })
                is Serie -> startActivity(Intent(requireContext(), DetailActivity::class.java).apply {
                    putExtra(DetailActivity.EXTRA_SERIE, item)
                })
            }
        }
    }

    fun load(vodType: String) {
        type = vodType
        scope.launch {
            if (!isAdded) return@launch
            val rowsAdapter = ArrayObjectAdapter(ListRowPresenter(FocusHighlight.ZOOM_FACTOR_SMALL).apply {
                shadowEnabled = false; selectEffectEnabled = false
            })
            if (type == VodActivity.TYPE_MOVIES) {
                val movies = withContext(Dispatchers.IO) {
                    try { ApiService.getMovies() } catch (_: Exception) { emptyList() }
                }
                val featured = movies.filter { it.featured }
                if (featured.isNotEmpty()) {
                    val adapter = ArrayObjectAdapter(PosterPresenter())
                    featured.forEach { adapter.add(it) }
                    rowsAdapter.add(ListRow(HeaderItem("⭐ DESTACADAS"), adapter))
                }
                val categoryIndices = mutableListOf<Pair<String, Int>>()
                movies.groupBy { it.category }.forEach { (cat, items) ->
                    val adapter = ArrayObjectAdapter(PosterPresenter())
                    items.forEach { adapter.add(it) }
                    categoryIndices.add(cat.ifEmpty { "OTRAS" } to rowsAdapter.size())
                    rowsAdapter.add(ListRow(HeaderItem(cat.ifEmpty { "OTRAS" }), adapter))
                }
                onCategoriesLoaded?.invoke(categoryIndices)
            } else {
                val series = withContext(Dispatchers.IO) {
                    try { ApiService.getSeries() } catch (_: Exception) { emptyList() }
                }
                val featured = series.filter { it.featured }
                if (featured.isNotEmpty()) {
                    val adapter = ArrayObjectAdapter(PosterPresenter())
                    featured.forEach { adapter.add(it) }
                    rowsAdapter.add(ListRow(HeaderItem("⭐ DESTACADAS"), adapter))
                }
                val categoryIndices = mutableListOf<Pair<String, Int>>()
                series.groupBy { it.category }.forEach { (cat, items) ->
                    val adapter = ArrayObjectAdapter(PosterPresenter())
                    items.forEach { adapter.add(it) }
                    categoryIndices.add(cat.ifEmpty { "OTRAS" } to rowsAdapter.size())
                    rowsAdapter.add(ListRow(HeaderItem(cat.ifEmpty { "OTRAS" }), adapter))
                }
                onCategoriesLoaded?.invoke(categoryIndices)
            }
            if (!isAdded) return@launch
            adapter = rowsAdapter
            onLoaded?.invoke()
        }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}

class PosterPresenter : Presenter() {
    override fun onCreateViewHolder(parent: android.view.ViewGroup): ViewHolder {
        val ctx = parent.context
        val dp = ctx.resources.displayMetrics.density
        val card = android.widget.FrameLayout(ctx).apply {
            layoutParams = android.view.ViewGroup.LayoutParams((120*dp).toInt(), (180*dp).toInt())
            isFocusable = true; isFocusableInTouchMode = true
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 8*dp; setColor(0xFF0F0F1A.toInt())
            }
            (layoutParams as? android.view.ViewGroup.MarginLayoutParams)
                ?.setMargins((4*dp).toInt(), (4*dp).toInt(), (4*dp).toInt(), (4*dp).toInt())
        }
        val poster = android.widget.ImageView(ctx).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT)
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            tag = "poster"
        }
        val nameOverlay = android.widget.LinearLayout(ctx).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = android.view.Gravity.BOTTOM
            }
            setBackgroundColor(0xCC000000.toInt())
            setPadding((6*dp).toInt(), (4*dp).toInt(), (6*dp).toInt(), (4*dp).toInt())
        }
        val name = android.widget.TextView(ctx).apply {
            setTextColor(0xFFE8E8E8.toInt()); textSize = 10f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
            tag = "name"
        }
        nameOverlay.addView(name); card.addView(poster); card.addView(nameOverlay)

        card.setOnFocusChangeListener { v, focused ->
            v.animate().scaleX(if (focused) 1.08f else 1f)
                .scaleY(if (focused) 1.08f else 1f).setDuration(120).start()
            (v.background as? android.graphics.drawable.GradientDrawable)
                ?.setStroke(if (focused) (2*ctx.resources.displayMetrics.density).toInt() else 0,
                    0xFFC9A84C.toInt())
        }
        return ViewHolder(card)
    }

    override fun onBindViewHolder(vh: ViewHolder, item: Any) {
        val card = vh.view as android.widget.FrameLayout
        val poster = card.findViewWithTag<android.widget.ImageView>("poster")
        poster?.setImageDrawable(null)
        when (item) {
            is Movie -> {
                card.findViewWithTag<android.widget.TextView>("name")?.text = item.title
                if (item.posterUrl.isNotEmpty()) Glide.with(card).load(item.posterUrl).into(poster!!)
            }
            is Serie -> {
                card.findViewWithTag<android.widget.TextView>("name")?.text = item.title
                if (item.posterUrl.isNotEmpty()) Glide.with(card).load(item.posterUrl).into(poster!!)
            }
        }
    }

    override fun onUnbindViewHolder(vh: ViewHolder) {
        (vh.view as android.widget.FrameLayout)
            .findViewWithTag<android.widget.ImageView>("poster")?.setImageDrawable(null)
    }
}
