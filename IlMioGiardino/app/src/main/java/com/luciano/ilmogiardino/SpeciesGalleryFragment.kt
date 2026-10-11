package com.luciano.ilmogiardino

import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class SpeciesGalleryFragment : Fragment() {

    private var plantId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        plantId = requireArguments().getLong("plantId", -1L)
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        val v = i.inflate(R.layout.fragment_gallery, c, false)
        val plant = Store.loadPlants(requireContext()).firstOrNull { it.id == plantId }
        v.findViewById<TextView>(R.id.tvGTitle).text = "Foto — ${plant?.name ?: ""}"
        v.findViewById<View>(R.id.btnBackG).setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }

        val grid = v.findViewById<android.widget.GridLayout>(R.id.galleryGrid)
        val files = SpeciesImages.filesFor(requireContext(), plant?.species ?: "")
        if (files.isEmpty()) {
            val tv = TextView(requireContext()).apply {
                text = "Nessuna foto in libreria per questa specie.\nUsa \"Riconosci\" per analizzare una foto della pianta."
                gravity = Gravity.CENTER
                setPadding(32, 48, 32, 48)
                setTextColor(resources.getColor(R.color.text_secondary))
            }
            grid.addView(tv)
        }
        val side = (resources.displayMetrics.widthPixels / 2) - 24
        for ((idx, f) in files.withIndex()) {
            val iv = ImageView(requireContext()).apply {
                layoutParams = android.widget.GridLayout.LayoutParams(
                    android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED),
                    android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED)
                ).apply {
                    width = side; height = side
                    setMargins(8, 8, 8, 8)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            SpeciesImages.into(requireContext(), plant?.species ?: "", iv, idx)
            iv.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setView(ImageView(requireContext()).apply {
                        val bmp = SpeciesImages.loadBitmap(requireContext(), f)
                        bmp?.let { setImageBitmap(it) }
                        adjustViewBounds = true
                    })
                    .setPositiveButton("Chiudi", null)
                    .show()
            }
            grid.addView(iv)
        }
        return v
    }
}
