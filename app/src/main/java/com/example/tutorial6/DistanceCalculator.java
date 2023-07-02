package com.example.tutorial6;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class DistanceCalculator {
    // Google Maps Directions API endpoint URL
    private static final String DIRECTIONS_API_URL = "https://maps.googleapis.com/maps/api/directions/json";

    // API key for the Google Maps Directions API
    private static final String API_KEY = "AIzaSyAnMzKMQjaK1QJLEmyoaHecN3bEZ9XALJ8";

    public interface DistanceCalculationListener {
        void onDistanceCalculated(double distance);

        void onDistanceCalculationFailed(String errorMessage);
    }


    public static void calculateTotalDistance(ArrayList<LatLng> latLngs, DistanceCalculationListener listener) {
        String requestUrl = buildDirectionsUrl(latLngs);

        if (requestUrl != null) {
            new DistanceCalculationTask(listener).execute(requestUrl);
        } else {
            listener.onDistanceCalculationFailed("Invalid input coordinates");
        }
    }

    private static String buildDirectionsUrl(ArrayList<LatLng> latLngs) {
        if (latLngs == null || latLngs.size() < 2) {
            return null; // Invalid input
        }

        StringBuilder urlBuilder = new StringBuilder(DIRECTIONS_API_URL);
        urlBuilder.append("?origin=").append(latLngs.get(0).latitude).append(",").append(latLngs.get(0).longitude);

        for (int i = 1; i < latLngs.size(); i++) {
            urlBuilder.append("&waypoints=").append("via:").append(latLngs.get(i).latitude).append(",")
                    .append(latLngs.get(i).longitude);
        }

        urlBuilder.append("&destination=").append(latLngs.get(latLngs.size() - 1).latitude).append(",")
                .append(latLngs.get(latLngs.size() - 1).longitude)
                .append("&key=").append(API_KEY);

        return urlBuilder.toString();
    }

    private static class DistanceCalculationTask extends AsyncTask<String, Void, String> {

        private final DistanceCalculationListener listener;

        DistanceCalculationTask(DistanceCalculationListener listener) {
            this.listener = listener;
        }

        @Override
        protected String doInBackground(String... urls) {
            String responseString = "";

            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                InputStream inputStream = connection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    responseString += line;
                }

                bufferedReader.close();
                inputStream.close();
                connection.disconnect();

            } catch (IOException e) {
                Log.e("DistanceCalculator", "Error occurred while fetching route data", e);
            }

            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject jsonResult = new JSONObject(result);
                String status = jsonResult.getString("status");

                if (status.equals("OK")) {
                    JSONArray routes = jsonResult.getJSONArray("routes");
                    if (routes.length() > 0) {
                        JSONObject route = routes.getJSONObject(0);
                        JSONObject legs = route.getJSONArray("legs").getJSONObject(0);
                        JSONObject distance = legs.getJSONObject("distance");
                        double distanceValue = distance.getDouble("value");

                        listener.onDistanceCalculated(distanceValue);
                    } else {
                        listener.onDistanceCalculationFailed("No routes found");
                    }
                } else {
                    listener.onDistanceCalculationFailed("Directions API request failed with status: " + status);
                }

            } catch (JSONException e) {
                Log.e("DistanceCalculator", "Error parsing JSON response", e);
                listener.onDistanceCalculationFailed("Error parsing JSON response");
            }
        }

    }
}