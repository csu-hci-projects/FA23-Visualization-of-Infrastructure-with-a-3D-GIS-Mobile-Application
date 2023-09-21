package com.example.cs567_3d_ui_project.qgis_map.qgis_map_elements

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.cs567_3d_ui_project.R

//https://stackoverflow.com/questions/40587168/simple-android-grid-example-using-recyclerview-with-gridlayoutmanager-like-the

class QGisMapRecyclerViewAdapter(private val qgisMapTiles: ArrayList<QGisMapTile>):
    RecyclerView.Adapter<QGisMapRecyclerViewAdapter.QGisMapRecyclerViewHolder>(){

    //private var layoutInflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QGisMapRecyclerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.qgis_map_tile, parent, false)

        return QGisMapRecyclerViewHolder(view)
    }

    override fun getItemCount(): Int {
        return qgisMapTiles.size
    }

    override fun onBindViewHolder(holder: QGisMapRecyclerViewHolder, position: Int) {
        var qGisMapTile = qgisMapTiles[position]
        holder.qGisMapTileImage.setImageBitmap(qGisMapTile.tile)
    }


    class QGisMapRecyclerViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        var qGisMapTileImage: ImageView

        init{
            qGisMapTileImage = itemView.findViewById(R.id.qgisMapTile)
        }
    }

}

