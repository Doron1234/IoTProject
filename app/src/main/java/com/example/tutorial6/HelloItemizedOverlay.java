package com.example.tutorial6;

import android.graphics.Color;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class HelloItemizedOverlay {
    private final List<LatLng> points = new ArrayList<>();
    private final GoogleMap googleMap;
    private Polyline polyline;

    public HelloItemizedOverlay(GoogleMap googleMap) {
        this.googleMap = googleMap;
    }

    public void addOverlay(LatLng latLng) {
        points.add(latLng);

        if (polyline != null) {
            polyline.remove();
        }

        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(points)
                .color(Color.BLUE)
                .width(5.0f); // Adjust the width as desired
        polyline = googleMap.addPolyline(polylineOptions);
    }

    public void clear() {
        points.clear();
        if (polyline != null) {
            polyline.remove();
            polyline = null;
        }
    }
}