package com.luciano.ilmogiardino

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AreasFragment : Fragment() {

    private lateinit var adapter: AreaAdapter
    private var areas: MutableList<Area> = mutableListOf()
    private var plants: MutableList<Plant> = mutableListOf()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        val v = i.inflate(R.layout.fragment_areas, c, false)
        val rv = v.findViewById<RecyclerView>(R.id.areasList)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = AreaAdapter()
        rv.adapter = adapter
        v.findViewById<Button>(R.id.btnAddArea).setOnClickListener { showAreaDialog(null) }
        return v
    }

    override fun onResume() {
        super.onResume()
        areas = Store.loadAreas(requireContext())
        plants = Store.loadPlants(requireContext())
        adapter.notifyDataSetChanged()
    }

    private fun plantsIn(id: Long) = plants.filter { it.areaId == id }

    inner class AreaAdapter : RecyclerView.Adapter<AreaAdapter.VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_area, parent, false))

        override fun getItemCount() = areas.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val a = areas[pos]
            val inArea = plantsIn(a.id)
            h.name.text = a.name
            h.details.text = if (a.description.isNotBlank()) "${a.description}\n" else ""
            h.details.append("${inArea.size} piante")
            if (inArea.isNotEmpty()) {
                h.plantsList.text = inArea.joinToString(" • ") { it.name }
                h.plantsList.visibility = View.VISIBLE
            } else {
                h.plantsList.visibility = View.GONE
            }
            h.itemView.setOnClickListener { showAreaDialog(a) }
            h.btnDelete.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Eliminare l'area \"${a.name}\"?")
                    .setMessage("Le piante non verranno eliminate, resteranno senza area.")
                    .setPositiveButton("Elimina") { _, _ ->
                        areas.remove(a)
                        Store.saveAreas(requireContext(), areas)
                        notifyDataSetChanged()
                    }
                    .setNegativeButton("Annulla", null)
                    .show()
            }
        }

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvAreaName)
            val details: TextView = v.findViewById(R.id.tvAreaDetails)
            val plantsList: TextView = v.findViewById(R.id.tvAreaPlants)
            val btnDelete: ImageButton = v.findViewById(R.id.btnDeleteArea)
        }
    }

    private fun showAreaDialog(area: Area?) {
        val v = layoutInflater.inflate(R.layout.dialog_area, null)
        val name = v.findViewById<EditText>(R.id.etAreaName)
        val desc = v.findViewById<EditText>(R.id.etAreaDesc)
        area?.let { name.setText(it.name); desc.setText(it.description) }

        AlertDialog.Builder(requireContext()).setTitle(if (area == null) "Nuova area del giardino" else "Modifica area")
            .setView(v)
            .setPositiveButton("Salva") { _, _ ->
                val a = area ?: Area(Store.nextId(requireContext()), "", "")
                a.name = name.text.toString().ifBlank { "Area" }
                a.description = desc.text.toString()
                if (area == null) areas.add(a)
                Store.saveAreas(requireContext(), areas)
                adapter.notifyDataSetChanged()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
}
