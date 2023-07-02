package com.example.tutorial6;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class History extends AppCompatActivity implements OnMapReadyCallback {

    private List<String> spinnerItems;
    private ArrayAdapter<String> adapter;
    private DatabaseReference mDatabase;
    private MapView mapView;
    private GoogleMap googleMap;
    Spinner spinner;
    private String TAG = "lol";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        spinner = findViewById(R.id.spinner);
        spinnerItems = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                googleMap.clear();
                String selectedItem = parent.getItemAtPosition(position).toString();
                DatabaseReference selectedTableRef = mDatabase.child(user.getEmail().replace(".", "%") + selectedItem);

                selectedTableRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        List<LatLng> points = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            double latitude = snapshot.child("latitude").getValue(Double.class);
                            double longitude = snapshot.child("longitude").getValue(Double.class);
                            LatLng latLng = new LatLng(latitude, longitude);
                            points.add(latLng);
                        }

                        if (points.size() > 1) {
                            PolylineOptions polylineOptions = new PolylineOptions()
                                    .addAll(points)
                                    .color(Color.BLUE)
                                    .width(5f);

                            googleMap.addPolyline(polylineOptions);
                        }
                        float zoomLevel = 17.0f; //This goes up to 21
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), zoomLevel)); // replace zoomLevel with the desired zoom level

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Handle database error if needed
                    }
                });
                DatabaseReference selectedTableRef2 = mDatabase.child(user.getEmail().replace(".", "%") + selectedItem + "markers");
                selectedTableRef2.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            DataSnapshot latLngSnapshot = snapshot.child("key");
                            Double lat = latLngSnapshot.child("latitude").getValue(Double.class);
                            Double lng = latLngSnapshot.child("longitude").getValue(Double.class);

                            if (lat == null || lng == null) {
                                Log.e(TAG, "Null LatLng at: " + snapshot.getKey());
                                continue;
                            }

                            LatLng latLng = new LatLng(lat, lng);
                            Integer colorCode = snapshot.child("value").getValue(Integer.class);

                            if (colorCode == null) {
                                Log.e(TAG, "Null color code at: " + snapshot.getKey());
                                continue;
                            }

                            Pair<LatLng, Integer> pair = new Pair<>(latLng, colorCode);
                            addMarker(pair);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle the case when nothing is selected
            }
        });

        if (user != null) {
            String userEmail = user.getEmail();
            mDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    spinnerItems.clear();
                    for (DataSnapshot tableSnapshot : dataSnapshot.getChildren()) {
                        String tableName = tableSnapshot.getKey();
                        if (tableName.startsWith(userEmail.replace(".", "%")) && !(tableName.endsWith("markers"))) {
                            spinnerItems.add(tableName.replace(userEmail.replace(".", "%"), ""));
                        }
                    }

                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Handle database error if needed
                }
            });
        }

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    public void onMapReady(GoogleMap map) {
        googleMap = map;

    }

    private void addMarker(Pair<LatLng, Integer> pair) {
        if (googleMap != null && pair.getFirst() != null) {
            float color;
            switch (pair.getSecond()) {
                case 3:
                    color = BitmapDescriptorFactory.HUE_YELLOW;
                    break;
                case 4:
                    color = BitmapDescriptorFactory.HUE_ORANGE;
                    break;
                case 5:
                    color = BitmapDescriptorFactory.HUE_RED;
                    break;
                default:
                    color = BitmapDescriptorFactory.HUE_BLUE; // or any other default color
            }

            googleMap.addMarker(new MarkerOptions()
                    .position(pair.getFirst())
                    .icon(BitmapDescriptorFactory.defaultMarker(color)));
        }
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
                Intent intent = new Intent(History.this, Login.class);
                startActivity(intent);
                return true;
            case R.id.menu_new_drive:
                TerminalFragment fragment = new TerminalFragment();
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment, fragment)
                        .commit();
                return true;
            case R.id.menu_history:
                // Perform action for menu item "History"
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
