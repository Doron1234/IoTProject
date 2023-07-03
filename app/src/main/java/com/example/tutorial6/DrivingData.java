package com.example.tutorial6;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.opencsv.CSVReader;

import com.google.android.gms.maps.model.LatLng;


public class DrivingData extends AppCompatActivity {

    private TextView textViewScore;
    private Button buttonCalculate, buttonImportCSV;
    private double totalDistance;
    private final float MULTIPLY =  2.23693629f;
    private int smallBrakes = 0;
    private int mediumBrakes = 0;
    private int largeBrakes = 0;
    FirebaseAuth auth;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driving_data);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        String firebasePlace = getIntent().getStringExtra("yourKey");
        DatabaseReference totalDistanceRef = mDatabase.child(firebasePlace + "totalDistance");
        DatabaseReference avgSpeedRef = mDatabase.child(firebasePlace + "avgSpeed");
        DatabaseReference maxSpeedRef = mDatabase.child(firebasePlace + "maxSpeed");
        DatabaseReference maxAccRef = mDatabase.child(firebasePlace + "maxAcc");
        DatabaseReference maxDecRef = mDatabase.child(firebasePlace + "maxDec");
        DatabaseReference driveScoreRef = mDatabase.child(firebasePlace + "driveScore");
        DatabaseReference totalTimeRef = mDatabase.child(firebasePlace + "totalTime");
        DatabaseReference markersRef = mDatabase.child(firebasePlace + "markers");
        markersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    DataSnapshot latLngSnapshot = snapshot.child("first");
                    Double lat = latLngSnapshot.child("latitude").getValue(Double.class);
                    Double lng = latLngSnapshot.child("longitude").getValue(Double.class);

                    if (lat == null || lng == null) {
                        continue;
                    }

                    LatLng latLng = new LatLng(lat, lng);
                    Integer colorCode = snapshot.child("second").getValue(Integer.class);

                    if (colorCode == null) {
                        continue;
                    }

                    switch (colorCode) {
                        case 1:
                            smallBrakes++;
                            break;
                        case 2:
                            smallBrakes++;
                            break;
                        case 3:
                            mediumBrakes++;
                            break;
                        case 4:
                            mediumBrakes++;
                            break;
                        case 5:
                            largeBrakes++;
                            break;
                        default:
                            break;// or any other default color
                    }
                }
                easySuddenBreaksTextView.setText("Number of easy sudden breaks: " + String.valueOf(smallBrakes));
                mediumSuddenBreaksTextView.setText("Number of medium sudden breaks: " + String.valueOf(mediumBrakes));
                hardSuddenBreaksTextView.setText("Number of hard sudden breaks: " + String.valueOf(largeBrakes));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        totalTimeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Get the value from the dataSnapshot
                float value = dataSnapshot.getValue(Float.class);
                durationTextView.setText("Duration of Drive: " + String.valueOf(value));

                // Use the value as needed
                // ...
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle any errors that occur during the read operation
                // ...
            }
        });
        driveScoreRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Get the value from the dataSnapshot
                float value = dataSnapshot.getValue(Float.class);
                scoreTextView.setText("Drive Score: " + String.valueOf(value));

                // Use the value as needed
                // ...
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle any errors that occur during the read operation
                // ...
            }
        });
        totalDistanceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Get the value from the dataSnapshot
                float value = dataSnapshot.getValue(Float.class);
                distanceTextView.setText("Distance Traveled: " + String.valueOf(value));

                // Use the value as needed
                // ...
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle any errors that occur during the read operation
                // ...
            }
        });
        avgSpeedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Get the value from the dataSnapshot
                float value = dataSnapshot.getValue(Float.class);
                averageSpeedTextView.setText("Average Speed: " + String.valueOf(value));

                // Use the value as needed
                // ...
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle any errors that occur during the read operation
                // ...
            }
        });
        maxSpeedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Get the value from the dataSnapshot
                float value = dataSnapshot.getValue(Float.class);
                maxSpeedTextView.setText("Maximum Speed: " + String.valueOf(value));

                // Use the value as needed
                // ...
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle any errors that occur during the read operation
                // ...
            }
        });
        maxAccRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Get the value from the dataSnapshot
                float value = dataSnapshot.getValue(Float.class);
                accelerationTextView.setText("Maximum Acceleration: " + String.valueOf(value));

                // Use the value as needed
                // ...
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle any errors that occur during the read operation
                // ...
            }
        });
        maxDecRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Get the value from the dataSnapshot
                float value = dataSnapshot.getValue(Float.class);
                decelerationTextView.setText("Maximum Deceleration: " + String.valueOf(value));

                // Use the value as needed
                // ...
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle any errors that occur during the read operation
                // ...
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.themenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.menu_logout:
                FirebaseAuth.getInstance().signOut();
                Intent intent_logout = new Intent(DrivingData.this, Login.class);
                startActivity(intent_logout);
                return true;
            case R.id.menu_home:
                Intent intent_home = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent_home);
                return true;
            case R.id.menu_history:
                Intent intent_history = new Intent(getApplicationContext(), History.class);
                startActivity(intent_history);
                return true;
        }

        return super.onOptionsItemSelected(item);
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
        return score;
    }
}