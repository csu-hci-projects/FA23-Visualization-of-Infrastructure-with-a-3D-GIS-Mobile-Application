package com.example.cs567_3d_ui_project.argis.mlutils

import androidx.compose.runtime.Immutable
import com.example.cs567_3d_ui_project.argis.helpers.ObjectDetectionHelper

@Immutable
class UiState(
    val detectionResult: ObjectDetectionHelper.DetectionResult? = null,
    val setting: Setting = Setting(),
    val errorMessage: String? = null,
)

@Immutable
data class Setting(
    val model: ObjectDetectionHelper.Model = ObjectDetectionHelper.MODEL_DEFAULT,
    val delegate: ObjectDetectionHelper.Delegate = ObjectDetectionHelper.Delegate.CPU,
    val resultCount: Int = ObjectDetectionHelper.MAX_RESULTS_DEFAULT,
    val threshold: Float = ObjectDetectionHelper.THRESHOLD_DEFAULT,
)
