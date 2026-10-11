package com.luciano.ilmogiardino

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Treatment(
    val id: Long,
    val plantId: Long,
    var type: String,
    var frequencyDays: Int,
    var lastDoneMillis: Long,
    var note: String
)

object TreatmentStore {
    private const val PREFS = "giardino_store"
    private const val KEY_TREATMENTS = "treatments"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(ctx: Context): MutableList<Treatment> {
        val arr = JSONArray(prefs(ctx).getString(KEY_TREATMENTS, "[]"))
        val list = mutableListOf<Treatment>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                Treatment(
                    o.getLong("id"), o.getLong("plantId"),
                    o.getString("type"), o.optInt("frequencyDays", 30),
                    o.optLong("lastDoneMillis", System.currentTimeMillis()),
                    o.optString("note", "")
                )
            )
        }
        return list
    }

    fun save(ctx: Context, list: List<Treatment>) {
        val arr = JSONArray()
        for (t in list) {
            val o = JSONObject()
            o.put("id", t.id); o.put("plantId", t.plantId)
            o.put("type", t.type); o.put("frequencyDays", t.frequencyDays)
            o.put("lastDoneMillis", t.lastDoneMillis); o.put("note", t.note)
            arr.put(o)
        }
        prefs(ctx).edit().putString(KEY_TREATMENTS, arr.toString()).apply()
    }
}
