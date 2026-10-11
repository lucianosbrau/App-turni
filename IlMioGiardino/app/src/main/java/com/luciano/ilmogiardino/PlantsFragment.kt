package com.luciano.ilmogiardino

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class PlantsFragment : Fragment() {

    private lateinit var adapter: PlantAdapter
    private var plants: MutableList<Plant> = mutableListOf()
    private var areas: MutableList<Area> = mutableListOf()
    private var query = ""

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        val v = i.inflate(R.layout.fragment_plants, c, false)
        val rv = v.findViewById<RecyclerView>(R.id.plantsList)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = PlantAdapter()
        rv.adapter = adapter

        val search = v.findViewById<EditText>(R.id.searchBox)
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(e: Editable?) { query = e.toString(); adapter.update() }
            override fun beforeTextChanged(p0: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(p0: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        v.findViewById<FloatingActionButton>(R.id.btnAddPlant).setOnClickListener { showPlantDialog(null) }

        val nav = v.findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_areas -> { (requireActivity() as MainActivity).show(AreasFragment()); true }
                R.id.nav_calendar -> { (requireActivity() as MainActivity).show(CalendarFragment()); true }
                R.id.nav_camera -> { (requireActivity() as MainActivity).show(IdentifyFragment()); true }
                R.id.nav_diagnose -> { (requireActivity() as MainActivity).show(DiagnoseFragment()); true }
                else -> true
            }
        }
        return v
    }

    override fun onResume() {
        super.onResume()
        plants = Store.loadPlants(requireContext())
        areas = Store.loadAreas(requireContext())
        adapter.update()
    }

    private fun areaName(id: Long) = areas.firstOrNull { it.id == id }?.name ?: "Senza area"

    private val empty: View? get() = view?.findViewById(R.id.emptyView)

    inner class PlantAdapter : RecyclerView.Adapter<PlantAdapter.VH>() {
        private var shown = plants.toList()

        fun update() {
            shown = plants.filter {
                query.isBlank() || it.name.contains(query, true) || it.species.contains(query, true)
                        || areaName(it.areaId).contains(query, true)
            }
            empty?.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_plant, parent, false))

        override fun getItemCount() = shown.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val p = shown[pos]
            h.name.text = p.name
            h.species.text = p.species.ifBlank { "Specie sconosciuta" }
            h.details.text = "📍 ${areaName(p.areaId)} • 💧 ogni ${p.wateringIntervalDays} giorni"
            SpeciesImages.into(requireContext(), p.species, h.img)
            h.itemView.setOnClickListener {
                PlantDetailFragment().also { it.arguments = Bundle().apply { putLong("plantId", p.id) } }
                    .let { (requireActivity() as MainActivity).show(it) }
            }
            h.btnEdit.setOnClickListener { showPlantDialog(p) }
            h.btnDelete.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Eliminare ${p.name}?")
                    .setPositiveButton("Elimina") { _, _ ->
                        plants.remove(p)
                        Notifier.cancel(requireContext(), p)
                        Store.savePlants(requireContext(), plants)
                        update()
                    }
                    .setNegativeButton("Annulla", null)
                    .show()
            }
        }

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val img: ImageView = v.findViewById(R.id.imgSpecies)
            val name: TextView = v.findViewById(R.id.tvName)
            val species: TextView = v.findViewById(R.id.tvSpecies)
            val details: TextView = v.findViewById(R.id.tvDetails)
            val btnEdit: ImageButton = v.findViewById(R.id.btnEdit)
            val btnDelete: ImageButton = v.findViewById(R.id.btnDelete)
        }
    }

    private fun showPlantDialog(plant: Plant?) {
        val v = layoutInflater.inflate(R.layout.dialog_plant, null)
        val name = v.findViewById<EditText>(R.id.etName)
        val species = v.findViewById<EditText>(R.id.etSpecies)
        val notes = v.findViewById<EditText>(R.id.etNotes)
        val days = v.findViewById<EditText>(R.id.etDays)
        val areaSpinner = v.findViewById<Spinner>(R.id.spArea)

        if (areas.isEmpty()) {
            Toast.makeText(requireContext(), "Prima crea un'area del giardino (scheda Aree)", Toast.LENGTH_LONG).show()
        }
        val areaNames = areas.map { it.name }.ifEmpty { listOf("Nessuna area — crea un'area") }
        areaSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, areaNames)

        plant?.let {
            name.setText(it.name)
            species.setText(it.species)
            notes.setText(it.notes)
            days.setText(it.wateringIntervalDays.toString())
            val idx = areas.indexOfFirst { a -> a.id == it.areaId }
            if (idx >= 0) areaSpinner.setSelection(idx)
        }

        AlertDialog.Builder(requireContext()).setTitle(if (plant == null) "Nuova pianta" else "Modifica pianta")
            .setView(v)
            .setPositiveButton("Salva") { _, _ ->
                val p = plant ?: Plant(Store.nextId(requireContext()), "", "", -1, 3, "", null)
                p.name = name.text.toString().ifBlank { "Pianta" }
                p.species = species.text.toString()
                p.notes = notes.text.toString()
                p.wateringIntervalDays = days.text.toString().toIntOrNull() ?: 3
                val aidx = areaSpinner.selectedItemPosition
                p.areaId = if (areas.isNotEmpty() && aidx in areas.indices) areas[aidx].id else -1
                if (plant == null) plants.add(p)
                Store.savePlants(requireContext(), plants)
                Notifier.cancel(requireContext(), p)
                Notifier.scheduleNext(requireContext(), p, p.wateringIntervalDays * 24L * 3600L * 1000L)
                adapter.update()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
}
