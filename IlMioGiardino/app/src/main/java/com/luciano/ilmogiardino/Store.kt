package com.luciano.ilmogiardino

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Plant(
    val id: Long,
    var name: String,
    var species: String,
    var areaId: Long,
    var wateringIntervalDays: Int,
    var notes: String,
    var photoPath: String?
)

data class Area(
    val id: Long,
    var name: String,
    var description: String
)

object Store {
    private const val PREFS = "giardino_store"
    private const val KEY_PLANTS = "plants"
    private const val KEY_AREAS = "areas"
    private const val KEY_SEQ = "seq"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadPlants(ctx: Context): MutableList<Plant> {
        val raw = prefs(ctx).getString(KEY_PLANTS, "[]")
        val arr = JSONArray(raw)
        val list = mutableListOf<Plant>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                Plant(
                    o.getLong("id"),
                    o.getString("name"),
                    o.optString("species", ""),
                    o.getLong("areaId"),
                    o.optInt("wateringIntervalDays", 3),
                    o.optString("notes", ""),
                    if (o.isNull("photoPath")) null else o.optString("photoPath")
                )
            )
        }
        return list
    }

    fun savePlants(ctx: Context, plants: List<Plant>) {
        val arr = JSONArray()
        for (p in plants) {
            val o = JSONObject()
            o.put("id", p.id)
            o.put("name", p.name)
            o.put("species", p.species)
            o.put("areaId", p.areaId)
            o.put("wateringIntervalDays", p.wateringIntervalDays)
            o.put("notes", p.notes)
            o.put("photoPath", p.photoPath ?: JSONObject.NULL)
            arr.put(o)
        }
        prefs(ctx).edit().putString(KEY_PLANTS, arr.toString()).apply()
    }

    fun loadAreas(ctx: Context): MutableList<Area> {
        val raw = prefs(ctx).getString(KEY_AREAS, "[]")
        val arr = JSONArray(raw)
        val list = mutableListOf<Area>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(Area(o.getLong("id"), o.getString("name"), o.optString("description", "")))
        }
        return list
    }

    fun saveAreas(ctx: Context, areas: List<Area>) {
        val arr = JSONArray()
        for (a in areas) {
            val o = JSONObject()
            o.put("id", a.id)
            o.put("name", a.name)
            o.put("description", a.description)
            arr.put(o)
        }
        prefs(ctx).edit().putString(KEY_AREAS, arr.toString()).apply()
    }

    fun nextId(ctx: Context): Long {
        val v = prefs(ctx).getLong(KEY_SEQ, 0L) + 1L
        prefs(ctx).edit().putLong(KEY_SEQ, v).apply()
        return v
    }
}
