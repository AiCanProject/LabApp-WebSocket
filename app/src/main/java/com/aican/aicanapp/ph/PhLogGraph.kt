package com.aican.aicanapp.ph

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aican.aicanapp.databinding.ActivityPhLogGraphBinding
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries

class PhLogGraph : AppCompatActivity() {

    companion object {
        public var phDataArrayList = ArrayList<Float>()
    }

    lateinit var binding: ActivityPhLogGraphBinding

    lateinit var graphView: GraphView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhLogGraphBinding.inflate(layoutInflater)
        setContentView(binding.root)

        graphView = binding.graph

        graphView.viewport.isScalable = true

        graphView.viewport.isScrollable = true

        graphView.viewport.setScalableY(true)

        graphView.viewport.setScrollableY(true)

        graphView.viewport.isXAxisBoundsManual = true
        graphView.viewport.setMinX(-2.0)
        graphView.viewport.setMaxX(20.0)

        graphView.viewport.isYAxisBoundsManual = true
        graphView.viewport.setMinY(-800.0)
        graphView.viewport.setMaxY(800.0)

        val seriesData = ArrayList<DataPoint>()
        var i = 1
        for (data in phDataArrayList) {
            seriesData.add(DataPoint(i.toDouble(), data.toDouble()))
            i++
        }


        val series = LineGraphSeries<DataPoint>(
           seriesData.toTypedArray()
        )
        graphView.addSeries(series)
        series.isDrawDataPoints = true
        series.setAnimated(true)


    }
}