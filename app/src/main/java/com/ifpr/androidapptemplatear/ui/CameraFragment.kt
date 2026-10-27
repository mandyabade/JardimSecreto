
package com.ifpr.androidapptemplatear.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ifpr.androidapptemplatear.R
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.ar.core.Config
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Position
import java.io.IOException

class CameraFragment : Fragment() {

    private lateinit var sceneView: ArSceneView
    private lateinit var placeButton: ExtendedFloatingActionButton
    private lateinit var sharedViewModel: SharedModelViewModel

    private var modelNode: ArModelNode? = null
    private var currentModelFile = "flower.glb"
    private var modelPlaced = false
    private var loadingModel = false

    private val cameraPermissionCode = 1001

    private val availableModels = listOf(
        "flower.glb",
        "neutrofilo.glb",
        "orquidea.glb",
        "arvore1.glb"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel =
            ViewModelProvider(requireActivity())[SharedModelViewModel::class.java]

        sceneView = view.findViewById(R.id.sceneView)
        sceneView.lightEstimationMode = Config.LightEstimationMode.DISABLED

        placeButton = view.findViewById(R.id.place)
        placeButton.text = "Posicionar objeto"

        placeButton.setOnClickListener {
            handleObjectButton()
        }

        sharedViewModel.selectedModel.observe(viewLifecycleOwner) { selected ->
            if (!selected.isNullOrBlank()) {
                loadModel(selected)
            }
        }

        checkCameraPermission()
    }

    private fun handleObjectButton() {
        val node = modelNode

        if (node == null || loadingModel) {
            return
        }

        if (!modelPlaced) {
            node.anchor()
            node.isEditable = true
            node.isScaleEditable = false

            modelPlaced = true
            placeButton.text = "Coletar item"
            sceneView.planeRenderer.isVisible = false
        } else {
            sharedViewModel.collectItem(currentModelFile)

            Toast.makeText(
                requireContext(),
                "Você encontrou: ${currentModelFile.removeSuffix(".glb")}",
                Toast.LENGTH_SHORT
            ).show()

            loadRandomModel()
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                cameraPermissionCode
            )
        } else {
            initAR()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == cameraPermissionCode &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            initAR()
        } else if (requestCode == cameraPermissionCode) {
            Toast.makeText(
                requireContext(),
                "A câmera é necessária para explorar o jardim.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun initAR() {
        val selected = sharedViewModel.selectedModel.value

        if (!selected.isNullOrBlank()) {
            loadModel(selected)
        } else {
            loadRandomModel()
        }
    }

    private fun loadRandomModel() {
        val nextModel = availableModels.random()
        loadModel(nextModel)
    }

    private fun loadModel(fileName: String) {
        if (!isAdded || loadingModel) return

        try {
            requireContext().assets.open(fileName).use { }
        } catch (e: IOException) {
            Toast.makeText(
                requireContext(),
                "Não foi possível encontrar $fileName nos assets.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        loadingModel = true

        modelNode?.let { sceneView.removeChild(it) }
        modelNode = null

        currentModelFile = fileName
        modelPlaced = false
        placeButton.text = "Posicionar objeto"
        sceneView.planeRenderer.isVisible = true

        val newNode = ArModelNode(
            sceneView.engine,
            PlacementMode.INSTANT
        ).apply {
            isEditable = true
            isScaleEditable = false
        }

        modelNode = newNode
        sceneView.addChild(newNode)

        newNode.loadModelGlbAsync(
            glbFileLocation = fileName,
            centerOrigin = Position(0f, 0f, 1f)
        ) {
            if (!isAdded) return@loadModelGlbAsync

            loadingModel = false
            placeButton.text = "Posicionar objeto"
            sceneView.planeRenderer.isVisible = true
        }
    }

    override fun onDestroyView() {
        modelNode?.let {
            sceneView.removeChild(it)
        }

        modelNode = null
        super.onDestroyView()
    }
}
