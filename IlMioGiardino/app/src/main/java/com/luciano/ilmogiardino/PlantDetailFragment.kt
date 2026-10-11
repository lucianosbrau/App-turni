package com.luciano.ilmogiardino

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class PlantDetailFragment : Fragment() {

    private var plantId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        plantId = requireArguments().getLong("plantId", -1L)
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        val v = i.inflate(R.layout.fragment_plant_detail, c, false)

        val plant = Store.loadPlants(requireContext()).firstOrNull { it.id == plantId }
        if (plant == null) { requireActivity().supportFragmentManager.popBackStack(); return v }
        val area = Store.loadAreas(requireContext()).firstOrNull { it.id == plant.areaId }

        v.findViewById<TextView>(R.id.tvDName).text = plant.name
        v.findViewById<TextView>(R.id.tvDSub).text = buildString {
            append(plant.species.ifBlank { "Specie sconosciuta" })
            area?.let { append("  •  📍 ${it.name}") }
        }

        val img = v.findViewById<ImageView>(R.id.imgDHeader)
        SpeciesImages.into(requireContext(), plant.species, img)

        val menuWater = v.findViewById<LinearLayout>(R.id.menuWatering)
        val menuTreat = v.findViewById<LinearLayout>(R.id.menuTreatments)
        val menuPhotos = v.findViewById<LinearLayout>(R.id.menuPhotos)
        val btnBack = v.findViewById<View>(R.id.btnBackDetail)

        btnBack.setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }
        menuWater.setOnClickListener {
            CalendarFragment().also { it.arguments = Bundle().apply { putLong("focusPlantId", plant.id) } }
                .let { (requireActivity() as MainActivity).show(it) }
        }
        menuTreat.setOnClickListener {
            TreatmentsFragment().also { it.arguments = Bundle().apply { putLong("plantId", plant.id) } }
                .let { (requireActivity() as MainActivity).show(it) }
        }
        menuPhotos.setOnClickListener {
            SpeciesGalleryFragment().also { it.arguments = Bundle().apply { putLong("plantId", plant.id) } }
                .let { (requireActivity() as MainActivity).show(it) }
        }
        return v
    }
}
