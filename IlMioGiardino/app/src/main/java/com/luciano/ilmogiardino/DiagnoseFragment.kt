package com.luciano.ilmogiardino

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class DiagnoseFragment : Fragment() {

    private val answers = mutableMapOf<String, Int>()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        val v = i.inflate(R.layout.fragment_diagnose, c, false)
        val container = v.findViewById<LinearLayout>(R.id.questionsContainer)
        val btnResult = v.findViewById<Button>(R.id.btnResult)
        val tvResult = v.findViewById<TextView>(R.id.tvDiagnosis)
        v.findViewById<Button>(R.id.btnBackDiag).setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }

        for (q in Diagnoser.questions) {
            val tv = TextView(requireContext()).apply { text = q.text; setPadding(0, 24, 0, 8) }
            container.addView(tv)
            val rg = RadioGroup(requireContext())
            q.options.forEachIndexed { idx, opt ->
                val rb = RadioButton(requireContext()).apply { text = opt; id = idx }
                rg.addView(rb)
            }
            rg.setOnCheckedChangeListener { _, checked -> answers[q.id] = checked }
            container.addView(rg)
        }

        btnResult.setOnClickListener {
            tvResult.text = Diagnoser.diagnose(answers)
        }
        return v
    }
}
