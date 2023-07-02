package com.example.tutorial6;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.opencsv.CSVReader;

import com.google.android.gms.maps.model.LatLng;


public class DrivingData extends AppCompatActivity {

    private TextView textViewScore;
    private Button buttonCalculate, buttonImportCSV;
    private double totalDistance;
    private final float MULTIPLY =  2.23693629f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driving_data);

        // Retrieve references to TextView elements
        TextView distanceTextView = findViewById(R.id.distanceTextView);
        TextView averageSpeedTextView = findViewById(R.id.averageSpeedTextView);
        TextView maxSpeedTextView = findViewById(R.id.maxSpeedTextView);
        TextView durationTextView = findViewById(R.id.durationTextView);
        TextView accelerationTextView = findViewById(R.id.accelerationTextView);
        TextView decelerationTextView = findViewById(R.id.decelerationTextView);
        TextView easySuddenBreaksTextView = findViewById(R.id.easySuddenBreaksTextView);
        TextView mediumSuddenBreaksTextView = findViewById(R.id.mediumSuddenBreaksTextView);
        TextView hardSuddenBreaksTextView = findViewById(R.id.hardSuddenBreaksTextView);
        TextView scoreTextView = findViewById(R.id.scoreTextView);

        String fileName = getIntent().getStringExtra("csvFileName");
        ArrayList<String[]> csvData = CsvRead("/sdcard/csv_dir/" + fileName);

        // Process the acceleration data
        ArrayList<Float> accelerationXList = new ArrayList<>();
        ArrayList<Float> accelerationYList = new ArrayList<>();
        ArrayList<Float> accelerationZList = new ArrayList<>();
        ArrayList<Float> times = new ArrayList<>();

        // Create an ArrayList of LatLng objects representing the driving route
        // TO DO: Get the ArrayList from Doron and Lion's intent
        ArrayList<LatLng> drivingRoute = new ArrayList<>();
        drivingRoute.add(new LatLng(37.7749, -122.4194)); // Latitude and longitude of the starting point
        drivingRoute.add(new LatLng(37.3352, -122.0096)); // Latitude and longitude of intermediate waypoints
        drivingRoute.add(new LatLng(34.0522, -118.2437)); // ...
        drivingRoute.add(new LatLng(32.7157, -117.1611)); // ...
        drivingRoute.add(new LatLng(34.0522, -118.2437)); // ...
        drivingRoute.add(new LatLng(37.7749, -122.4194)); // Latitude and longitude of the ending point

        // Implement the DistanceCalculationListener interface
        DistanceCalculator.DistanceCalculationListener distanceCalculationListener = new DistanceCalculator.DistanceCalculationListener() {
            @Override
            public void onDistanceCalculated(double distance) {
                // Distance calculation successful
                totalDistance = distance;
                Log.d("Distance", "Total distance: " + distance + " meters");
            }

            @Override
            public void onDistanceCalculationFailed(String errorMessage) {
                // Distance calculation failed
                Log.e("Distance", "Distance calculation failed: " + errorMessage);
            }
        };

        // Call the calculateTotalDistance method with the ArrayList of LatLng and the listener
        DistanceCalculator.calculateTotalDistance(drivingRoute, distanceCalculationListener);


        for (String[] row : csvData) {
            // Assuming the acceleration values are in the first two columns (X and Y)
            float accelerationX = Float.parseFloat(row[0]);
            float accelerationY = Float.parseFloat(row[1]);
            float accelerationZ = Float.parseFloat(row[2]);
            float t = Float.parseFloat(row[3]);

            accelerationXList.add(accelerationX);
            accelerationYList.add(accelerationY);
            accelerationZList.add(accelerationZ);
            times.add(t);
        }

        float duration = calculateDuration(times);
        float averageSpeed = calculateAverageSpeed(duration);
        float maxSpeed = calculateMaxSpeed(accelerationXList, accelerationYList, accelerationZList, times);
        float maxAcceleration = calculateMaxAcceleration(accelerationXList, accelerationYList, accelerationZList);
        float maxDeceleration = calculateMaxDeceleration(accelerationXList, accelerationYList, accelerationZList);
        float drivingScore = calculateDrivingScore(accelerationXList, accelerationYList, accelerationZList, times);


        // Update the text of TextViews with the driving statistics
        distanceTextView.setText("Total Distance: " + totalDistance + " km");
        averageSpeedTextView.setText("Average Speed: " + averageSpeed + " km/h");
        maxSpeedTextView.setText("Max Speed: " + maxSpeed + " km/h");
        durationTextView.setText("Duration: " + duration + " seconds");
        accelerationTextView.setText("Max Acceleration: " + maxAcceleration + " m/s^2");
        decelerationTextView.setText("Max Deceleration: " + maxDeceleration + " m/s^2");
        scoreTextView.setText("Driving Score: " + drivingScore);
    }


    private ArrayList<String[]> CsvRead(String path){
        ArrayList<String[]> CsvData = new ArrayList<>();
        try {
            File file = new File(path);
            CSVReader reader = new CSVReader(new FileReader(file));
            String[] nextLine;
            while((nextLine = reader.readNext()) != null){
                if(nextLine != null){
                    CsvData.add(nextLine);
                }
            }
        }catch (Exception e){}
        return CsvData;
    }


    private float calculateAverageSpeed(float duration){
        return (float) (totalDistance / duration);
    }

    private float calculateMaxSpeed(ArrayList<Float> accelerationXList, ArrayList<Float> accelerationYList, ArrayList<Float> accelerationZList, ArrayList<Float> times){
        // Calculate the velocity based on the acceleration data
        float initialSpeed = 0.0f;
        float currentSpeed = initialSpeed;
        float maxSpeed = initialSpeed;
        float timeStep;

        for (int i = 0; i < accelerationXList.size(); i++) {
            float accelerationX = accelerationXList.get(i);
            float accelerationY = accelerationYList.get(i);
            float accelerationZ = accelerationZList.get(i);
            if(i == 0){
                timeStep = times.get(i);
            }
            else{
                timeStep = times.get(i) - times.get(i - 1); // Time step between acceleration samples (in seconds)
            }

            // Calculate the total acceleration magnitude
            float accelerationMagnitude = (float) Math.sqrt(
                    accelerationX * accelerationX +
                            accelerationY * accelerationY +
                            accelerationZ * accelerationZ
            );

            // Calculate the change in velocity using the acceleration
            float deltaVelocity = accelerationMagnitude * timeStep;
            if (accelerationX < 0){
                deltaVelocity *= -1;
            }
            // Update the current speed
            currentSpeed += deltaVelocity;

            // Check if the current speed is higher than the maximum speed
            if (currentSpeed > maxSpeed) {
                maxSpeed = currentSpeed;
            }
        }
        return maxSpeed;
    }

    private float calculateDuration(ArrayList<Float> times){
        return times.get(times.size() - 1);
    }

    private float calculateMaxAcceleration(ArrayList<Float> accelerationXList, ArrayList<Float> accelerationYList, ArrayList<Float> accelerationZList){
        float maxAcceleration = 0.0f;

        for (int i = 0; i < accelerationXList.size(); i++) {
            float accelerationX = accelerationXList.get(i);
            float accelerationY = accelerationYList.get(i);
            float accelerationZ = accelerationZList.get(i);

            // Calculate the magnitude of the acceleration vector
            float accelerationMagnitude = (float) Math.sqrt(accelerationX * accelerationX + accelerationY * accelerationY + accelerationZ * accelerationZ);

            // Update the maximum acceleration if the current magnitude is higher
            if (accelerationMagnitude > maxAcceleration) {
                maxAcceleration = accelerationMagnitude;
            }
        }
        return maxAcceleration;
    }

    private float calculateMaxDeceleration(ArrayList<Float> accelerationXList, ArrayList<Float> accelerationYList, ArrayList<Float> accelerationZList){
        float maxDeceleration = 0.0f; // in m/s^2

        for (int i = 0; i < accelerationXList.size(); i++) {
            float accelerationX = accelerationXList.get(i);
            float accelerationY = accelerationYList.get(i);
            float accelerationZ = accelerationZList.get(i);

            // Calculate the magnitude of the acceleration vector
            float accelerationMagnitude = (float) Math.sqrt(accelerationX * accelerationX + accelerationY * accelerationY + accelerationZ * accelerationZ);

            // Update the maximum acceleration if the current magnitude is higher
            if (accelerationMagnitude > maxDeceleration && accelerationX < 0) {
                maxDeceleration = accelerationMagnitude;
            }
        }
        return maxDeceleration;
    }

    private int calculateDrivingScore(){
        // Read and process the imported CSV data
        // Example logic: Calculate driving score based on sudden breaks

        // Replace this with your own logic to calculate the driving score
        // based on the actual data in the CSV file
        int drivingScore = 75;

        return drivingScore;
    }

    private int[] calculateSuddenBreaks(ArrayList<Float> accelerationX,ArrayList<Float> accelerationY, ArrayList<Float> accelerationZ){
        int[] a = new int[3];
        return a;
    }

    private float calculateDrivingScore(ArrayList<Float> accelerationXList, ArrayList<Float> accelerationYList, ArrayList<Float> accelerationZList, ArrayList<Float> times){
        float score = 0.0f;
        return score; }
}