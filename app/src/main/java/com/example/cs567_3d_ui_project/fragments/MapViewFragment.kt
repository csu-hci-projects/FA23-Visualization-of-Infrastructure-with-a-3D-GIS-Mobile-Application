package com.example.cs567_3d_ui_project.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.arcgismaps.mapping.view.MapView
import com.example.cs567_3d_ui_project.R
import com.example.cs567_3d_ui_project.databinding.FragmentMapViewBinding

class MapViewFragment: Fragment(R.layout.fragment_map_view) {

    private var binding: FragmentMapViewBinding? = null

    private lateinit var mapView: MapView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMapViewBinding.bind(view)

    }
}