package com.luciano.ilmogiardino

import android.content.Context
import org.json.JSONObject

data class SpeciesInfo(
    val name: String,
    val fioritura: String,
    val foglie: String,
    val concimazione: String,
    val trattamenti: String,
    val potaturaMesi: List<Int>,
    val potaturaNota: String
)

object SpeciesDb {

    private var db: Map<String, SpeciesInfo>? = null
    val monthNames = listOf(
        "gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno",
        "luglio", "agosto", "settembre", "ottobre", "novembre", "dicembre"
    )

    @Synchronized
    fun load(ctx: Context): Map<String, SpeciesInfo> {
        db?.let { return it }
        val raw = ctx.assets.open("species_database.json").bufferedReader().readText()
        val obj = JSONObject(raw)
        val map = mutableMapOf<String, SpeciesInfo>()
        for (key in obj.keys()) {
            val o = obj.getJSONObject(key)
            val mesi = mutableListOf<Int>()
            val arr = o.getJSONArray("potatura")
            for (i in 0 until arr.length()) mesi.add(arr.getInt(i))
            map[key.lowercase()] = SpeciesInfo(
                key, o.getString("fioritura"), o.getString("foglie"),
                o.getString("concimazione"), o.getString("trattamenti"),
                mesi, o.getString("potaturaNota")
            )
        }
        db = map
        return map
    }

    fun find(ctx: Context, species: String): SpeciesInfo? {
        if (species.isBlank()) return null
        val map = load(ctx)
        val s = species.trim().lowercase()
        return map[s]
            ?: map.entries.firstOrNull { it.key.contains(s) || s.contains(it.key) }?.value
    }

    fun names(ctx: Context): List<String> = load(ctx).values.map { it.name }.sorted()

    fun mesiTesto(mesi: List<Int>): String =
        mesi.sorted().joinToString(" • ") { monthNames[(it - 1) % 12].replaceFirstChar { c -> c.uppercase() } }
}
