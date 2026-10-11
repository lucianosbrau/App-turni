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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TreatmentsFragment : Fragment() {

    private var plantId: Long = -1
    private lateinit var adapter: TreatmentAdapter
    private var treatments: MutableList<Treatment> = mutableListOf()
    private val fmt = SimpleDateFormat("d MMM", Locale.ITALIAN)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        plantId = requireArguments().getLong("plantId", -1L)
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        val v = i.inflate(R.layout.fragment_treatments, c, false)
        val plant = Store.loadPlants(requireContext()).firstOrNull { it.id == plantId }
        v.findViewById<TextView>(R.id.tvTTitle).text = "Trattamenti — ${plant?.name ?: ""}"

        val rv = v.findViewById<RecyclerView>(R.id.treatmentsList)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = TreatmentAdapter()
        rv.adapter = adapter

        v.findViewById<View>(R.id.btnBackT).setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }
        v.findViewById<Button>(R.id.btnAddTreatment).setOnClickListener { showTreatmentDialog(null) }
        return v
    }

    override fun onResume() {
        super.onResume()
        treatments = TreatmentStore.load(requireContext()).filter { it.plantId == plantId }.toMutableList()
        adapter.update()
    }

    private fun reschedule(t: Treatment) {
        val plant = Store.loadPlants(requireContext()).firstOrNull { it.id == plantId }
        Notifier.cancelTreatment(requireContext(), t)
        plant?.let { Notifier.scheduleTreatment(requireContext(), it, t) }
    }

    inner class TreatmentAdapter : RecyclerView.Adapter<TreatmentAdapter.VH>() {
        private var shown = treatments.toList()
        fun update() { shown = treatments.toList(); notifyDataSetChanged() }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_treatment, parent, false))

        override fun getItemCount() = shown.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val t = shown[pos]
            h.type.text = t.type
            val next = t.lastDoneMillis + t.frequencyDays * 24L * 3600L * 1000L
            h.details.text = "Ogni ${t.frequencyDays} giorni • prossimo: ${fmt.format(Date(next))}"
            h.itemView.setOnClickListener { showTreatmentDialog(t) }
            h.btnDone.setOnClickListener {
                t.lastDoneMillis = System.currentTimeMillis()
                TreatmentStore.save(requireContext(), TreatmentStore.load(requireContext()).map { if (it.id == t.id) t else it })
                reschedule(t)
                Toast.makeText(requireContext(), "${t.type}: registrazione aggiornata", Toast.LENGTH_SHORT).show()
                adapter.update()
            }
            h.btnDelete.setOnClickListener {
                treatments.remove(t)
                val all = TreatmentStore.load(requireContext()).filter { it.id != t.id }
                TreatmentStore.save(requireContext(), all)
                Notifier.cancelTreatment(requireContext(), t)
                adapter.update()
            }
        }

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val type: TextView = v.findViewById(R.id.tvTrType)
            val details: TextView = v.findViewById(R.id.tvTrDetails)
            val btnDone: ImageButton = v.findViewById(R.id.btnTrDone)
            val btnDelete: ImageButton = v.findViewById(R.id.btnTrDelete)
        }
    }

    private fun showTreatmentDialog(existing: Treatment?) {
        val v = layoutInflater.inflate(R.layout.dialog_treatment, null)
        val typeSpinner = v.findViewById<Spinner>(R.id.spTrType)
        val types = listOf("Concime", "Concime liquido", "Fungicida", "Antiparassitario", "Acaricida", "Potatura", "Rinvaso", "Altro")
        typeSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)
        val freq = v.findViewById<EditText>(R.id.etTrDays)
        val note = v.findViewById<EditText>(R.id.etTrNote)
        existing?.let {
            val idx = types.indexOf(it.type).coerceAtLeast(0)
            if (types.contains(it.type)) typeSpinner.setSelection(idx)
            freq.setText(it.frequencyDays.toString())
            note.setText(it.note)
        }

        AlertDialog.Builder(requireContext()).setTitle(if (existing == null) "Nuovo trattamento" else "Modifica trattamento")
            .setView(v)
            .setPositiveButton("Salva") { _, _ ->
                val t = existing ?: Treatment(
                    Store.nextId(requireContext()), plantId, "", 30,
                    System.currentTimeMillis(), ""
                )
                t.type = if (typeSpinner.selectedItemPosition == types.size - 1 && existing != null)
                    existing.type.takeIf { it.isNotEmpty() && it !in types } ?: "Altro"
                else typeSpinner.selectedItem.toString()
                t.frequencyDays = freq.text.toString().toIntOrNull() ?: 30
                t.note = note.text.toString()
                if (existing == null) {
                    treatments.add(t)
                    TreatmentStore.save(requireContext(), TreatmentStore.load(requireContext()).also { it.add(t) })
                } else {
                    TreatmentStore.save(requireContext(), TreatmentStore.load(requireContext()).map { if (it.id == t.id) t else it })
                }
                reschedule(t)
                adapter.update()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
}
