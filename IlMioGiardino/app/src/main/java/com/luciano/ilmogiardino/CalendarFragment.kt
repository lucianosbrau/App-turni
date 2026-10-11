package com.luciano.ilmogiardino

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalendarFragment : Fragment() {

    private val fmt = SimpleDateFormat("EEE d MMM", Locale.ITALIAN)

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        val v = i.inflate(R.layout.fragment_calendar, c, false)
        val text = v.findViewById<TextView>(R.id.tvSchedule)
        val btnBack = v.findViewById<Button>(R.id.btnBackCal)
        btnBack.setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }
        render(text)
        return v
    }

    private fun render(text: TextView) {
        val plants = Store.loadPlants(requireContext())
        if (plants.isEmpty()) {
            text.text = "Nessuna pianta registrata. Aggiungi piante per vedere il calendario delle annaffiature."
            return
        }
        val sb = StringBuilder("Prossime annaffiature:\n\n")
        val cal = Calendar.getInstance()
        for (p in plants.sortedBy { it.wateringIntervalDays }) {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, p.wateringIntervalDays)
            sb.append("• ").append(p.name)
                .append(" — ogni ").append(p.wateringIntervalDays).append(" giorni (prossima: ")
                .append(fmt.format(cal.time)).append(")\n")
        }
        sb.append("\nRiceverai una notifica locale il giorno dell'annaffiatura. Tutto offline.")
        text.text = sb.toString()
    }
}
