package com.treasurehuntadventure.tha

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.node.ModelNode
import io.github.sceneview.math.Position

class ARActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arSceneView = ARSceneView(this)
        setContentView(arSceneView)
        val glbFile = intent.getStringExtra("glbFile")
        val resId = getGlbResId(glbFile)
        if (resId != 0) {
            val modelNode = ModelNode(
                context = this,
                modelGlbFileLocation = null,
                modelGlbResId = resId,
                autoAnimate = true
            )
            modelNode.position = Position(0.0f, 0.0f, -1.0f)
            arSceneView.addChild(modelNode)
        }
    }
    private fun getGlbResId(glbFile: String?): Int = when (glbFile) {
        "gold_bar.glb" -> R.raw.gold_bar
        "gold_coin.glb" -> R.raw.gold_coin
        "treasure_chest.glb" -> R.raw.treasure_chest
        else -> 0
    }
} 