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
        v.findViewById<Button>(R.id.btnBack).setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }
        return v
    }

    override fun onResume() {
        super.onResume()
        areas = Store.loadAreas(requireContext())
        plants = Store.loadPlants(requireContext())
        adapter.notifyDataSetChanged()
    }

    private fun plantCount(id: Long) = plants.count { it.areaId == id }

    inner class AreaAdapter : RecyclerView.Adapter<AreaAdapter.VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_area, parent, false))

        override fun getItemCount() = areas.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val a = areas[pos]
            h.name.text = a.name
            h.details.text = "${plantCount(a.id)} piante • ${a.description.ifBlank { "—" }}"
            h.itemView.setOnClickListener { showAreaDialog(a) }
            h.btnDelete.setOnClickListener {
                areas.remove(a)
                Store.saveAreas(requireContext(), areas)
                notifyDataSetChanged()
            }
        }

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvAreaName)
            val details: TextView = v.findViewById(R.id.tvAreaDetails)
            val btnDelete: ImageButton = v.findViewById(R.id.btnDeleteArea)
        }
    }

    private fun showAreaDialog(area: Area?) {
        val v = layoutInflater.inflate(R.layout.dialog_area, null)
        val name = v.findViewById<EditText>(R.id.etAreaName)
        val desc = v.findViewById<EditText>(R.id.etAreaDesc)
        area?.let { name.setText(it.name); desc.setText(it.description) }

        AlertDialog.Builder(requireContext()).setTitle(if (area == null) "Nuova area" else "Modifica area")
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
