package com.campusfix.feature.report

import android.annotation.SuppressLint
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusfix.domain.repository.AulaRepository
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject

data class QrScanUiState(
    val aulaId: String? = null,
    val message: String? = null,
)

/**
 * HU03 - ViewModel del escaneo. Conecta CameraX con ML Kit Barcode Scanning
 * y resuelve el codigo QR contra el catalogo local de aulas (Room).
 */
@HiltViewModel
class QrScanViewModel @Inject constructor(
    private val aulaRepository: AulaRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(QrScanUiState())
    val state: StateFlow<QrScanUiState> = _state.asStateFlow()

    private val scanner = BarcodeScanning.getClient()
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var resolved = false

    @SuppressLint("UnsafeOptInUsageError")
    fun bindCamera(
        cameraProvider: ProcessCameraProvider,
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
    ) {
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analysis.setAnalyzer(analysisExecutor) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null && !resolved) {
                val input = InputImage.fromMediaImage(
                    mediaImage, imageProxy.imageInfo.rotationDegrees
                )
                scanner.process(input)
                    .addOnSuccessListener { barcodes -> handleBarcodes(barcodes) }
                    .addOnCompleteListener { imageProxy.close() }
            } else {
                imageProxy.close()
            }
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
            )
        } catch (e: Exception) {
            _state.update { it.copy(message = "No se pudo iniciar la camara") }
        }
    }

    private fun handleBarcodes(barcodes: List<Barcode>) {
        if (resolved) return
        val value = barcodes.firstOrNull()?.rawValue ?: return
        resolved = true
        viewModelScope.launch {
            val aula = aulaRepository.findByQr(value)
            if (aula != null) {
                _state.update { it.copy(aulaId = aula.id) }
            } else {
                _state.update {
                    it.copy(message = "El QR \"$value\" no corresponde a un aula registrada")
                }
                resolved = false // permitir volver a intentar
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        analysisExecutor.shutdown()
        scanner.close()
    }
}
