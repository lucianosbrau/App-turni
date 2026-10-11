package com.luciano.ilmogiardino

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView
import org.json.JSONObject
import kotlin.concurrent.thread

object SpeciesImages {
    private var index: Map<String, List<String>>? = null
    private val cache = mutableMapOf<String, Bitmap>()

    @Synchronized
    fun load(ctx: Context): Map<String, List<String>> {
        index?.let { return it }
        val raw = ctx.assets.open("species_index.json").bufferedReader().readText()
        val obj = JSONObject(raw)
        val map = mutableMapOf<String, List<String>>()
        for (key in obj.keys()) {
            val arr = obj.getJSONArray(key)
            map[key.lowercase()] = (0 until arr.length()).map { arr.getString(it) }
        }
        index = map
        return map
    }

    fun filesFor(ctx: Context, species: String): List<String> {
        if (species.isBlank()) return emptyList()
        val map = load(ctx)
        val s = species.trim().lowercase()
        return map[s]
            ?: map.entries.firstOrNull { it.key.contains(s) || s.contains(it.key) }?.value
            ?: emptyList()
    }

    fun loadBitmap(ctx: Context, assetPath: String): Bitmap? {
        cache[assetPath]?.let { return it }
        return runCatching {
            BitmapFactory.decodeStream(ctx.assets.open(assetPath)).also { cache[assetPath] = it }
        }.getOrNull()
    }

    fun into(ctx: Context, species: String, view: ImageView, which: Int = 0) {
        val files = filesFor(ctx, species)
        if (files.isEmpty()) {
            view.setImageResource(android.R.drawable.ic_menu_report_image)
            return
        }
        val path = files[which.coerceIn(0, files.size - 1)]
        thread {
            val bmp = loadBitmap(ctx, path)
            view.post {
                if (bmp != null) view.setImageBitmap(bmp) else view.setImageResource(android.R.drawable.ic_menu_report_image)
            }
        }
    }
}
