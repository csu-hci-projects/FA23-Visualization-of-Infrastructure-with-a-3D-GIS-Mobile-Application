/*
package com.example.cs567_3d_ui_project.argis.mlutils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cs567_3d_ui_project.argis.helpers.ObjectDetectionHelper

@Composable
fun ResultsOverlay(
    modifier: Modifier = Modifier,
    result: ObjectDetectionHelper.DetectionResult,
) {
    BoxWithConstraints(
        modifier
            .fillMaxSize()
    ) {
        val detections = result.detections
        for (detection in detections) {
            val ratioBox = detection.boundingBox
            val boxWidth = ratioBox.width() * maxWidth.value
            val boxHeight = ratioBox.height() * maxHeight.value
            val boxLeftOffset = ratioBox.left * maxWidth.value
            val boxTopOffset = ratioBox.top * maxHeight.value
            Box(
                Modifier
                    .fillMaxSize()
                    .offset(
                        boxLeftOffset.dp,
                        boxTopOffset.dp,
                    )
                    .width(boxWidth.dp)
                    .height(boxHeight.dp)
            ) {
                Box(
                    modifier = Modifier
                        .border(3.dp, Turquoise)
                        .width(boxWidth.dp)
                        .height(boxHeight.dp)
                )
                Box(modifier = Modifier.padding(3.dp)) {
                    Text(
                        text = "${
                            detection.label
                        } ${String.format("%.1f", detection.score)}",
                        modifier = Modifier
                            .background(Color.Black)
                            .padding(5.dp, 0.dp),
                        color = Color.White,
                    )
                }
            }
        }
    }
}*/
