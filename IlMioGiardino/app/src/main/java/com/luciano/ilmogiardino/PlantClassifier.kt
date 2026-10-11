package com.luciano.ilmogiardino

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class ClassifierResult(val label: String, val confidence: Float)

class PlantClassifier(context: Context) {
    private var speciesInterpreter: Interpreter? = null
    private var diseaseInterpreter: Interpreter? = null
    private var speciesLabels: List<String> = emptyList()
    private var diseaseLabels: List<String> = emptyList()

    init {
        val opts = Interpreter.Options()
        speciesInterpreter = Interpreter(loadModel(context, "plants_species.tflite"), opts)
        diseaseInterpreter = Interpreter(loadModel(context, "plant_disease.tflite"), opts)
        speciesLabels = loadSpeciesLabels(context)
        diseaseLabels = loadDiseaseLabels(context)
    }

    private fun loadModel(context: Context, name: String): ByteBuffer {
        val fd = context.assets.openFd(name)
        val inputStream = fd.createInputStream()
        val channel = inputStream.channel
        val buffer = channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
        return buffer
    }

    private fun loadSpeciesLabels(context: Context): List<String> {
        val lines = BufferedReader(InputStreamReader(context.assets.open("plants_labels.csv"))).readLines()
        return lines.drop(1).map { it.trim().split(",", limit = 2).lastOrNull() ?: "" }
    }

    private fun loadDiseaseLabels(context: Context): List<String> =
        BufferedReader(InputStreamReader(context.assets.open("disease_labels.txt"))).readLines().filter { it.isNotBlank() }

    fun classifySpecies(bitmap: Bitmap, topK: Int = 3): List<ClassifierResult> {
        val input = preprocess(bitmap, uint8 = true)
        val output = Array(1) { ByteArray(speciesLabels.size) }
        speciesInterpreter?.run(input, output)
        val probs = output[0].map { (it.toInt() and 0xFF) / 255.0f }
        return probs.withIndex().sortedByDescending { it.value }.take(topK)
            .map { ClassifierResult(speciesLabels.getOrNull(it.index) ?: "?", it.value) }
    }

    fun classifyDisease(bitmap: Bitmap, topK: Int = 3): List<ClassifierResult> {
        val input = preprocess(bitmap, uint8 = false)
        val output = Array(1) { FloatArray(diseaseLabels.size) }
        diseaseInterpreter?.run(input, output)
        val probs = output[0].toList()
        return probs.withIndex().sortedByDescending { it.value }.take(topK)
            .map { ClassifierResult(diseaseLabels.getOrNull(it.index) ?: "?", it.value) }
    }

    private fun preprocess(bitmap: Bitmap, uint8: Boolean): Any {
        val scaled = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val pixels = IntArray(224 * 224)
        scaled.getPixels(pixels, 0, 224, 0, 0, 224, 224)
        return if (uint8) {
            val buffer = ByteBuffer.allocateDirect(224 * 224 * 3)
            buffer.order(ByteOrder.nativeOrder())
            for (p in pixels) {
                buffer.put(((p shr 16) and 0xFF).toByte())
                buffer.put(((p shr 8) and 0xFF).toByte())
                buffer.put((p and 0xFF).toByte())
            }
            buffer.rewind()
            buffer
        } else {
            Array(1) { Array(224) { Array(224) { FloatArray(3) } } }.also { arr ->
                var idx = 0
                for (y in 0 until 224) {
                    for (x in 0 until 224) {
                        val p = pixels[idx++]
                        arr[0][y][x][0] = (((p shr 16) and 0xFF) / 255.0f)
                        arr[0][y][x][1] = (((p shr 8) and 0xFF) / 255.0f)
                        arr[0][y][x][2] = ((p and 0xFF) / 255.0f)
                    }
                }
            }
        }
    }

    fun close() {
        speciesInterpreter?.close()
        diseaseInterpreter?.close()
    }

    companion object {
        fun decodeScaled(path: String, maxDim: Int = 512): Bitmap? {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, opts)
            var sample = 1
            while (opts.outWidth / sample > maxDim * 2 || opts.outHeight / sample > maxDim * 2) sample *= 2
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
            return BitmapFactory.decodeFile(path, decodeOpts)
        }
    }
}
