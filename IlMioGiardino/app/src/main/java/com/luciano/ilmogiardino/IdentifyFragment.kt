package com.luciano.ilmogiardino

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IdentifyFragment : Fragment() {

    private var classifier: PlantClassifier? = null
    private var currentBitmap: android.graphics.Bitmap? = null
    private lateinit var image: ImageView
    private lateinit var result: TextView

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { analyze(it) }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        val v = i.inflate(R.layout.fragment_identify, c, false)
        image = v.findViewById(R.id.imgPhoto)
        result = v.findViewById(R.id.tvResult)
        v.findViewById<Button>(R.id.btnPick).setOnClickListener { pickImage.launch("image/*") }
        v.findViewById<Button>(R.id.btnBackId).setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }
        return v
    }

    override fun onDestroyView() {
        super.onDestroyView()
        classifier?.close()
        classifier = null
    }

    private fun analyze(uri: Uri) {
        result.text = "Analisi in corso…"
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                if (classifier == null) classifier = PlantClassifier(requireContext())
                val bmp = decode(uri) ?: throw IllegalStateException("Impossibile leggere l'immagine")
                currentBitmap = bmp
                val species = classifier!!.classifySpecies(bmp)
                val disease = classifier!!.classifyDisease(bmp)
                withContext(Dispatchers.Main) {
                    image.setImageURI(uri)
                    val sb = StringBuilder()
                    sb.append("Specie riconosciuta (AIY plants_V1):\n")
                    species.forEach { sb.append("• ").append(it.label).append(" — ").append("%.1f".format(it.confidence * 100)).append("%\n") }
                    sb.append("\nStato di salute (PlantVillage):\n")
                    disease.forEach { sb.append("• ").append(prettify(it.label)).append(" — ").append("%.1f".format(it.confidence * 100)).append("%\n") }
                    result.text = sb.toString()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Errore: ${e.message}", Toast.LENGTH_LONG).show()
                    result.text = "Analisi non riuscita."
                }
            }
        }
    }

    private fun decode(uri: Uri): android.graphics.Bitmap? {
        val path = uri.path ?: return null
        return PlantClassifier.decodeScaled(path)
    }

    private fun prettify(label: String): String =
        label.replace("___", " → ").replace("_", " ")
}
