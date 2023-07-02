package com.example.tutorial6;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.opencsv.CSVWriter;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener, OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private enum Connected { False, Pending, True }

    private String deviceAddress;
    private SerialService service;

    private TextView receiveText;

    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean hexEnabled = false;

    private boolean start_flag = false;
    private long startTimebegin;
    private boolean save_flag = false;
    private MapView mapView;
    private Button buttonStart;
    private Button buttonStop;
    private Button buttonStatistics;
    private ArrayList<Float> accelerationXList;
    private ArrayList<Float> accelerationYList;
    private ArrayList<Float> accelerationZList;
    private ArrayList<Float> times;
    private Context context;
    private GoogleMap googleMap;
    private Date startTime;
    private HelloItemizedOverlay itemizedOverlay;
    private List<LatLng> pathPoints = new ArrayList<>();
    private static final float MIN_DISTANCE_THRESHOLD = 10; // Minimum distance threshold in meters
    private DatabaseReference mDatabase;
    LocationManager locationManager;
    LocationListener locationListener;
    FirebaseAuth auth;
    FirebaseUser user;
    private String startTimeString;
    private LatLng currentLocation;
    private final float MULTIPLY =  2.23693629f;
    private float accelerationIndex;
    private float brakingIndex;
    private boolean accFlag = false;
    private boolean brakeFlag = false;
    private HashMap<LatLng, Marker> markerHashMap = new HashMap<>();
    private int maxAcceleration = 0;
    private int maxBrake = 0;
    private LatLng maxAccPoint;
    private LatLng maxBrakePoint;
    int level = 0;
    private List<Pair<LatLng, Integer>> markers;




    String selectedInSpinner = "Walking";

    int sum = 0;
    float before = 0, current = 0, after = 0;

    /*
     * Lifecycle
     */


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mapView.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if(service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation") // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try { getActivity().unbindService(this); } catch(Exception ignored) {}
        super.onDetach();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        if(initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if(initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }


    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }


    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.mains, container, false);        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());

        File file = new File("/sdcard/csv_dir/");
        file.mkdirs();
        context = getContext();
        buttonStart = view.findViewById(R.id.buttonStart);
        buttonStop = view.findViewById(R.id.buttonStop);
        buttonStatistics = view.findViewById(R.id.buttonStatistics);

        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            buttonStart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    markers = new ArrayList<>();
                    accelerationIndex = 0;
                    brakingIndex = 0;
                    start_flag = true;
                    startTimebegin = System.currentTimeMillis();
                    buttonStop.setEnabled(true);
                    buttonStart.setEnabled(false);
                    buttonStatistics.setEnabled(false);
                    Calendar calendar = Calendar.getInstance();
                    startTime = calendar.getTime();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                    startTimeString = dateFormat.format(startTime);
                    startLocationUpdates();
                    accelerationXList = new ArrayList<>();
                    accelerationYList = new ArrayList<>();
                    accelerationZList = new ArrayList<>();
                    times = new ArrayList<>();
                    googleMap.clear();
                }
            });
        }

        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start_flag = false;
                buttonStop.setEnabled(false);
                buttonStart.setEnabled(true);
                buttonStatistics.setEnabled(true);
                mDatabase.child(user.getEmail().replace(".", "%").toString() + startTimeString).setValue(pathPoints);
                mDatabase.child(user.getEmail().replace(".", "%").toString() + startTimeString + "markers").setValue(markers);
                stopLocationUpdates();
                pathPoints.clear();
                markers.clear();

            }
        });
        buttonStatistics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), DrivingData.class);
                startActivity(intent);
            }
        });

        return view;
    }




    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.themenu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_logout) {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getContext(), Login.class);
            startActivity(intent);
            // Handle settings option click
            return true;
        }
        else if (id == R.id.menu_history) {
            Intent intent = new Intent(getContext(), History.class);
            startActivity(intent);

            // Handle settings option click
            return true;
        }
        else if (id == R.id.menu_new_drive) {
            googleMap.clear();
            return true;
            // Handle settings option click
        }

        return super.onOptionsItemSelected(item);
    }

    /*
     * Serial + UI
     */
    private String[] clean_str(String[] stringsArr){
        for (int i = 0; i < stringsArr.length; i++)  {
            stringsArr[i]=stringsArr[i].replaceAll(" ","");
        }


        return stringsArr;
    }
    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            status("connecting...");
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }


    private void startLocationUpdates() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new TerminalFragment.MyLocationListener(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
    }

    private void stopLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.removeUpdates(locationListener);
    }

    void addPoint(double lat, double lng) {
        LatLng newPoint = new LatLng(lat, lng);

        if (pathPoints.isEmpty()) {
            pathPoints.add(newPoint);
        } else {
            LatLng lastPoint = pathPoints.get(pathPoints.size() - 1);
            float[] distance = new float[1];
            Location.distanceBetween(lastPoint.latitude, lastPoint.longitude, lat, lng, distance);
            float distanceInMeters = distance[0];

            if (distanceInMeters >= MIN_DISTANCE_THRESHOLD) {
                pathPoints.add(newPoint);
            }
        }
        itemizedOverlay.clear();
        for (LatLng point : pathPoints) {
            itemizedOverlay.addOverlay(point);
        }

        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newPoint, 15.0f));
    }

    private static final int REQUEST_LOCATION_PERMISSION = 1; // request code for location permission

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        // check if location permissions are granted
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permissions
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            googleMap.setMyLocationEnabled(true);
            itemizedOverlay = new HelloItemizedOverlay(googleMap);

            // Call getCurrentLocation() here
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // permissions have not been granted
            return;
        }

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            float zoomLevel = 16.0f;
                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, zoomLevel));
                        }
                    }
                });
    }



    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
    }


    private void receive(byte[] message) {

        if(hexEnabled) {
            receiveText.append(TextUtil.toHexString(message) + '\n');
        } else {
            String msg = new String(message);
            if(msg.length() > 0) {
                // don't show CR as ^M if directly before LF
                String msg_to_save = msg;
                msg_to_save = msg.replace(TextUtil.newline_crlf, TextUtil.emptyString);
                // check message length
                if (msg_to_save.length() > 1){
                    // split message string by ',' char
                    String[] parts = msg_to_save.split(",");
                    // function to trim blank spaces
                    parts = clean_str(parts);

                    // saving data to csv

                        // create new csv unless file already exists



                        // add received values to line dataset for plotting the linechart
                        long timePassed = System.currentTimeMillis() - startTimebegin;
                        float z = Float.parseFloat(parts[0]);
                        float y = Float.parseFloat(parts[1]);
                        float x = Float.parseFloat(parts[2]);
                        accelerationXList.add(x);
                        accelerationYList.add(y);
                        accelerationZList.add(z);
                        float t = ((float) timePassed) / 1000;
                        times.add(t);
                        if (accelerationXList.size() > 20){
                            accelerationXList.remove(0);
                            accelerationYList.remove(0);
                            accelerationZList.remove(0);
                            times.remove(0);
                        }
                        float integral_sum = 0;
                        for(int i = 0; i < 20; i++){
                            if (accelerationXList.get(i) < 0) {
                                integral_sum -= (accelerationXList.get(i) * accelerationXList.get(i) + accelerationYList.get(i) * accelerationYList.get(i) + accelerationZList.get(i) * accelerationZList.get(i)) * 0.05;
                            }
                            else{
                                integral_sum += (accelerationXList.get(i) * accelerationXList.get(i) + accelerationYList.get(i) * accelerationYList.get(i) + accelerationZList.get(i) * accelerationZList.get(i)) * 0.05;

                            }
                        }
                        integral_sum = integral_sum * MULTIPLY;
                        if (integral_sum >= 8){
                            maxAcceleration = 5;
                            accFlag = true;
                        }
                        else if (integral_sum >= 7){
                            if (maxAcceleration < 4) {
                                maxAcceleration = 4;
                                accFlag = true;
                            }
                        }
                        else if (integral_sum >= 6){
                            if (maxAcceleration < 3) {
                                maxAcceleration = 3;
                                accFlag = true;
                            }
                        }
                        else if (integral_sum >= 5){
                            if (maxAcceleration < 2) {
                                maxAcceleration = 2;
                                accFlag = true;
                            }
                        }
                        else if (integral_sum >= 4){
                            if (maxAcceleration < 1) {
                                maxAcceleration = 1;
                                accFlag = true;
                            }
                        }
                        else if(accFlag == true){
                            accelerationIndex += maxAcceleration;
                            maxAcceleration = 0;
                            accFlag = false;

                        }
                        if (integral_sum <= -13){
                            if (maxBrake < 5) {
                                maxBrake = 5;
                                brakeFlag = true;
                                maxBrakePoint = new LatLng(currentLocation.latitude, currentLocation.longitude);
                                level  = 5;

                            }
                        }
                        else if (integral_sum <= -11){
                            if (maxBrake < 4) {
                                maxBrake = 4;
                                brakeFlag = true;
                                maxBrakePoint = new LatLng(currentLocation.latitude, currentLocation.longitude);
                                level  = 4;
                            }
                        }
                        else if (integral_sum <= -9){
                            if (maxBrake < 3) {
                                maxBrake = 3;
                                brakeFlag = true;
                                maxBrakePoint = new LatLng(currentLocation.latitude, currentLocation.longitude);
                                level  = 3;

                            }
                        }
                        else if (integral_sum <= -7){
                            if (maxBrake < 2) {
                                maxBrake = 2;
                                brakeFlag = true;
                            }
                        }
                        else if (integral_sum <= -5){
                            if (maxBrake < 1) {
                                maxBrake = 1;
                                brakeFlag = true;
                            }
                        }
                        else if (brakeFlag == true){
                            brakingIndex += maxBrake;
                            maxBrake = 0;
                            brakeFlag = false;
                            addMarkerIfNotExist(maxBrakePoint, level);
                            if (maxBrakePoint != null) {
                                Pair<LatLng, Integer> pair = new Pair<LatLng, Integer>(maxBrakePoint, level);
                                markers.add(pair);
                            }
                            level = 0;
                        }
                        before = current;
                        current = after;
                        after = (float)Math.sqrt(x * x + y * y + z * z);
                        // parse string values, in this case [0] is tmp & [1] is count (t)
                        ArrayList<String> row= new ArrayList<String>();
                        row.add(parts[0]);
                        row.add(parts[1]);
                        row.add(parts[2]);
                        row.add(String.valueOf((float)timePassed/1000));

                   }
                try{
                if(!Python.isStarted()) {
                    Python.start(new AndroidPlatform(getContext()));
                }

                Python py = Python.getInstance();
                PyObject pyobj = py.getModule("notpythoncode");
                System.out.println(pyobj.toString());


                PyObject obj = pyobj.callAttr("m", before, current, after);
                sum += obj.toInt();

            } catch (Exception e) {
                System.out.println(e);
                e.printStackTrace();
            }

            msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);
            // send msg to function that saves it to csv
            // special handling if CR and LF come in separate fragments
            if (true && msg.charAt(0) == '\n') {
                Editable edt = receiveText.getEditableText();
                if (edt != null && edt.length() > 1)
                    edt.replace(edt.length() - 2, edt.length(), "");
            }
        }
    }
}
    private void addMarkerIfNotExist(LatLng point, int level) {
        if (level == 3){
            addYellowMarker(point);
        }
        else if (level == 4){
            addOrangeMarker(point);
        }
        else if (level == 5){
            addRedMarker(point);
        }

    }
    private void addOrangeMarker(LatLng point) {
        MarkerOptions markerOptions = new MarkerOptions()
                .position(point)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));

        googleMap.addMarker(markerOptions);
    }
    private void addRedMarker(LatLng point) {
        MarkerOptions markerOptions = new MarkerOptions()
                .position(point)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

        googleMap.addMarker(markerOptions);
    }
    private void addYellowMarker(LatLng point) {
        MarkerOptions markerOptions = new MarkerOptions()
                .position(point)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));

        googleMap.addMarker(markerOptions);
    }

    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        try {
            if(start_flag)
                receive(data);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }

    private ArrayList<Entry> emptyDataValues()
    {
        ArrayList<Entry> dataVals = new ArrayList<Entry>();
        return dataVals;
    }

    private void OpenLoadCSV(){
        Intent intent = new Intent(getContext(),LoadCSV.class);
        //intent.putExtra("fileOpenText", fileOpenText.getText().toString());
        startActivity(intent);
    }
    public class MyLocationListener implements LocationListener {
        private Context context;

        MyLocationListener(Context context) {
            this.context = context;
        }

        @Override
        public void onProviderDisabled(String provider) {
            Toast.makeText(context, "Gps Disabled", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderEnabled(String provider) {
            Toast.makeText(context, "Gps Enabled", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onLocationChanged(Location location) {
            double lat = location.getLatitude();
            double lng = location.getLongitude();
            currentLocation = new LatLng (lat, lng);
            addPoint(lat, lng);
        }
    }

}
