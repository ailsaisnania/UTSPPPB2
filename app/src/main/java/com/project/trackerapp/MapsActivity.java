package com.project.trackerapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.project.trackerapp.databinding.ActivityMapsBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, SensorEventListener {
    private int initialStepCount = -1;
    private int currentNumberOfStepCount = 0;
    private Button mButton;
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    AppWidgetManager appWidgetManager;

    ArrayList<LatLng> latLngList = new ArrayList<>();

    int PERMISSION_ALL = 1;

    String[] PERMISSIONS = {
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private FusedLocationProviderClient fusedLocationProviderClient;
    private PolylineOptions polylineOptions = new PolylineOptions();
    private Timer T = new Timer();

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);
            if (locationResult.getLocations().size() > 0) {
                addToLocationRoute(locationResult.getLocations());
            }
        }
    };

    private boolean isBtnStarted = false;
    private float distanceTravelled = 0f;
    private int timeCount = 0;

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        appWidgetManager = AppWidgetManager.getInstance(
                this);

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }


        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        if (!isBtnStarted) {
            binding.tvNumberStep.setText("0 Steps");
            binding.tvAveragePace.setText("0 Seconds");
            showContent(false);
        }

        binding.btnAction.setOnClickListener(v -> {
            if (!isBtnStarted) {
                isBtnStarted = true;
                polylineOptions = new PolylineOptions();

                latLngList.clear();
                mMap.clear();

                sendDataToWidget(0,
                        0, 0f);

                binding.tvNumberStep.setText("0 Steps");
                binding.tvAveragePace.setText("0 Seconds");
                showContent(false);
                setupStepCounterListener();
                setupLocationChangeListener();
                timeCount = 0;

                T = new Timer();
                T.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(() -> {
                            timeCount++;
                            binding.tvAveragePace.setText(new StringBuilder(timeCount + " Seconds"));
                            sendDataToWidget(currentNumberOfStepCount,
                                    timeCount, 0f);
                        });
                    }
                }, 1000, 1000);

                binding.btnAction.setText("end");
            } else if (isBtnStarted) {
                isBtnStarted = false;
                T.cancel();
                for (int i = 0; i < latLngList.size(); i++) {
                    if (i > 0) {
                        Location locationA = new Location("Previous Location");
                        locationA.setLatitude(latLngList.get(i - 1).latitude);
                        locationA.setLongitude(latLngList.get(i - 1).longitude);
                        Location locationB = new Location("New Location");
                        locationB.setLatitude(latLngList.get(i).latitude);
                        locationB.setLongitude(latLngList.get(i).longitude);
                        float meter = locationA.distanceTo(locationB);
                        distanceTravelled += meter;
                    }
                }

                showContent(true);

                binding.tvTotalDistance.setText(new StringBuilder(distanceTravelled + " meter"));
                binding.tvAveragePace.setText(new StringBuilder(timeCount + " Seconds"));
                binding.btnAction.setText("start");


                sendDataToWidget(currentNumberOfStepCount,
                        timeCount, distanceTravelled);

                stopTracking();
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mButton = findViewById(R.id.change_language);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent languageIntent = new Intent(Settings.ACTION_LOCALE_SETTINGS);
                startActivity(languageIntent);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        showUserLocation();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                LatLng startLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLocation, 17f));
            });
        }
    }

    private void showUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    private void sendDataToWidget(int currentNumberSteps, int timerCounter, float distanceTravelled) {
        Intent intent = new Intent(this, TrackerAppWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra("current_steps", currentNumberSteps);
        intent.putExtra("total_distance", distanceTravelled);
        intent.putExtra("timer_counter", timerCounter);

        int[] ids = AppWidgetManager.getInstance(this).getAppWidgetIds(new ComponentName(this, TrackerAppWidget.class));
        if (ids != null && ids.length > 0) {
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            sendBroadcast(intent);
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void addToLocationRoute(List<Location> locations) {
        mMap.clear();
        List<LatLng> originalLatLngList = polylineOptions.getPoints();

        for (Location location :
                locations) {
            latLngList.add(new LatLng(location.getLatitude(), location.getLongitude()));
        }

        originalLatLngList.addAll(latLngList);
        mMap.addPolyline(polylineOptions);
    }

    private void setupLocationChangeListener() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationRequest locationRequest = new LocationRequest();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(5000);
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void setupStepCounterListener() {
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (stepCounterSensor == null) {
            return;
        }
        sensorManager.registerListener(MapsActivity.this, stepCounterSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] arr = event.values;
        if (arr.length > 0) {
            if (initialStepCount == -1) {
                initialStepCount = Math.round(arr[0]);
            }
            currentNumberOfStepCount = Math.round(arr[0]) - initialStepCount;
            Log.d("TAG", "Steps count: " + currentNumberOfStepCount);
            binding.tvNumberStep.setText(new StringBuilder(currentNumberOfStepCount + " Steps"));

            sendDataToWidget(currentNumberOfStepCount,
                    timeCount, distanceTravelled);

//            Intent intent = new Intent(this, TrackerAppWidget.class);
//            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
//            intent.putExtra("current_steps", currentNumberOfStepCount);
//            int[] ids = AppWidgetManager.getInstance(this).getAppWidgetIds(new ComponentName(this, TrackerAppWidget.class));
//            if (ids != null && ids.length > 0) {
//                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
//                sendBroadcast(intent);
//            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d("TAG", "onAccuracyChanged: Sensor: " + sensor + "; accuracy: " + accuracy);
    }

    public void showContent(boolean isShow) {
        if (isShow) {
            binding.tvTotalDistance.setVisibility(View.VISIBLE);
//            binding.tvAveragePace.setVisibility(View.VISIBLE);
            binding.dumbPoint1.setVisibility(View.VISIBLE);
//            binding.dumbPoint2.setVisibility(View.VISIBLE);
            binding.dumbDistance.setVisibility(View.VISIBLE);
//            binding.dumbPace.setVisibility(View.VISIBLE);
        } else {
            binding.tvTotalDistance.setVisibility(View.GONE);
//            binding.tvAveragePace.setVisibility(View.GONE);
            binding.dumbPoint1.setVisibility(View.GONE);
//            binding.dumbPoint2.setVisibility(View.GONE);
            binding.dumbDistance.setVisibility(View.GONE);
//            binding.dumbPace.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_ALL) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        finish();
                        startActivity(getIntent());
                    }
                }
            }
        }
    }

    private void stopTracking() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        initialStepCount = -1;
        currentNumberOfStepCount = 0;
        Sensor stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        sensorManager.unregisterListener(this, stepCounterSensor);
    }

}