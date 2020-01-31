package com.example.geofence;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.ActionBar;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GeoQueryEventListener
{

    private GoogleMap mMap;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Marker cureentUser;

    private DatabaseReference myLocationRef;
    private DatabaseReference geoFence_Ref;
    private GeoFire geoFire;
    private List<LatLng> dangerousArea;
    private List<Data_Model> data_modelList;
    private EditText searchLocation;
    Marker mm;
    Data_Model data_model;
    long maxid = 0;


    SeekBar seekBar;
    TextView radiusText;
    double radius=0.0;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        searchLocation = findViewById(R.id.edit_text);
        dangerousArea = new ArrayList<>();
        data_modelList = new ArrayList<>();

        setting_Radius();

        geoFence_Ref = FirebaseDatabase.getInstance().getReference("GeoFence");
        geoFence_Ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if(dataSnapshot.exists())
                {
                    maxid = dataSnapshot.getChildrenCount();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {

            }
        });

        readFromFireBase();

        Dexter.withActivity(this)
        .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener()
                {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response)
                    {
                        buildLocationRequest();
                        buildLocationCallback();
                        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapsActivity.this);
                        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                                .findFragmentById(R.id.map);
                        mapFragment.getMapAsync(MapsActivity.this);

                        initiArea();
                        settingGeoFire();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response)
                    {
                        Toast.makeText(MapsActivity.this,"You Must enabled permission!",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();
    }

    public void setting_Radius()
    {
        radiusText = findViewById(R.id.radius);
        seekBar = findViewById(R.id.seekbar);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                radiusText.setText(progress +" km");
                radius = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.none:
                mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
                break;

            case R.id.normal:
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;

            case R.id.satellite:
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                break;

            case R.id.hybrid:
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                break;

            case R.id.terrain:
                mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void buildLocationRequest()
    {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //locationRequest.setInterval(5000);
        //locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10f);
    }

    private void settingGeoFire()
    {
        myLocationRef = FirebaseDatabase.getInstance().getReference("MyLocation");
        geoFire = new GeoFire(myLocationRef);
    }

    private void initiArea()
    {
        //dangerousArea.add(new LatLng(31.523983,74.347240));
        //dangerousArea.add(new LatLng(31.184568,74.370494));
        //dangerousArea.add(new LatLng(31.486706,74.272910));

        // read from firebase
        //readFromFireBase();
    }

    public void readFromFireBase()
    {
        geoFence_Ref.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                for (DataSnapshot snapshot:dataSnapshot.getChildren())
                {
                    Map<String,Object> data = (Map<String,Object>) snapshot.getValue();
                    double lat = (double)data.get("latitude");
                    double lon = (double)data.get("longitude");
                    Long radi = (Long)data.get("radius");

                    Data_Model data_model = new Data_Model();
                    data_model.setLatitude(lat);
                    data_model.setLongitude(lon);
                    data_model.setRadius(radi);
                    data_modelList.add(data_model);

                    dangerousArea.add(new LatLng(lat,lon));
                    System.out.println("Fencing Size "+dangerousArea.size());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {

            }
        });

    }

    private void buildLocationCallback()
    {
        locationCallback = new LocationCallback()
        {
            @Override
            public void onLocationResult(final LocationResult locationResult)
            {
                if(mMap!=null)
                {
                    geoFire.setLocation("You", new GeoLocation(locationResult.getLastLocation().getLatitude(),
                            locationResult.getLastLocation().getLongitude()), new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error)
                        {

                            if(cureentUser!=null) cureentUser.remove();
                            cureentUser = mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(locationResult.getLastLocation().getLatitude(),
                                            locationResult.getLastLocation().getLongitude()))
                                    .title("You are here")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)) );

                            // add markar
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(cureentUser.getPosition(),12.0f));
                        }
                    });

                }

            }
        };
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;

        if(mMap!=null)
        {
            // listen marker's new position
            mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDragStart(Marker marker) {

                }

                @Override
                public void onMarkerDrag(Marker marker) {

                }

                @Override
                public void onMarkerDragEnd(Marker marker)
                {
                    Geocoder gc = new Geocoder(MapsActivity.this);
                    List<Address> list = null;

                    try
                    {
                        LatLng latLng = marker.getPosition();

                        list = gc.getFromLocation(latLng.latitude,latLng.longitude,1);
                        Address address = list.get(0);
                        marker.setTitle(address.getLocality());
                        marker.showInfoWindow();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            });

            mMap.getUiSettings().setZoomControlsEnabled(true);

            if (fusedLocationProviderClient != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

            // add circle to dangerous Area
            addGeofences();

            // show more info about location
            mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {
                    View row = getLayoutInflater().inflate(R.layout.custom_address, null);
                    TextView textView_locality = row.findViewById(R.id.locality);
                    TextView textView_latitude = row.findViewById(R.id.latText);
                    TextView textView_longitude = row.findViewById(R.id.lonText);
                    TextView textView_snippet = row.findViewById(R.id.snippet);

                    LatLng latLng = marker.getPosition();
                    textView_locality.setText(marker.getTitle());
                    textView_latitude.setText(String.valueOf(latLng.latitude));
                    textView_longitude.setText(String.valueOf(latLng.longitude));
                    textView_snippet.setText(marker.getSnippet());

                    return row;
                }
            });
        }
    }

    public void addGeofences()
    {
        // after some time we will remove already add fences before add new fences again

        System.out.println("Size of Fences : "+dangerousArea.size());
        int count=0;

        for (LatLng latLng : dangerousArea)
        {
            Data_Model model = data_modelList.get(count);
            double rad = model.getRadius();
            rad = rad*1000;

            mMap.addCircle(new CircleOptions().center(latLng)
                    .radius(rad)
                    .strokeColor(Color.RED)
                    .fillColor(0x220000FF)
                    .strokeWidth(5.0f));

            count++;
            // query when user enter dangerous area
            GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(latLng.latitude, latLng.longitude), 1f);
            geoQuery.addGeoQueryEventListener(MapsActivity.this);
        }
        count=0;
    }

    @Override
    protected void onStop()
    {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onStop();
    }

    @Override
    public void onKeyEntered(String key, GeoLocation location)
    {
        sendNotification("Alert",String.format("%s Entered Dangerous Area",key));
    }

    @Override
    public void onKeyExited(String key)
    {
        sendNotification("Alert",String.format("%s leave Dangerous Area",key));
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location)
    {
        sendNotification("Alert",String.format("%s move within Dangerous Area",key));
    }

    @Override
    public void onGeoQueryReady() {

    }

    @Override
    public void onGeoQueryError(DatabaseError error)
    {
       Toast.makeText(this,""+error.getMessage(),Toast.LENGTH_SHORT).show();
    }

    private void sendNotification(String title, String content)
    {
       String NOTIFICATION_CHANNEL_ID = "geofence_multiple_location";
       NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
      {
          NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,"My Notification",
                  NotificationManager.IMPORTANCE_DEFAULT);

          notificationChannel.setDescription("Channel description");
          notificationChannel.enableLights(true);
          notificationChannel.setLightColor(Color.RED);
          notificationChannel.setVibrationPattern(new long[]{0,1000,500,1000});
          notificationChannel.enableVibration(true);
          notificationManager.createNotificationChannel(notificationChannel);
      }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,NOTIFICATION_CHANNEL_ID);
        builder.setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(false)
                .setSmallIcon(R.mipmap.important_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));

        Notification notification = builder.build();
        notificationManager.notify(new Random().nextInt(),notification);

    }

    public void findOnMap(View view)
    {
        Geocoder geocoder = new Geocoder(this);
        try
        {
            List<Address> myList = geocoder.getFromLocationName(searchLocation.getText().toString(),1);
            Address address = myList.get(0);
            String locality = address.getLocality();

            Toast.makeText(getApplicationContext(),locality,Toast.LENGTH_SHORT).show();

            final double latitude = address.getLatitude();
            final double longitude = address.getLongitude();
            goToLocation(latitude,longitude,12);

            if(mm!=null)
            {
                mm.remove();
            }
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.title(locality);
            markerOptions.draggable(true);
            markerOptions.snippet("Destination Point");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            markerOptions.position(new LatLng(latitude,longitude));

            mm = mMap.addMarker(markerOptions);

            new Handler().postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    alertBox(latitude,longitude);
                }
            },5000);


        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void goToLocation(double latitude, double longitude, int zoom)
    {
        LatLng latLng = new LatLng(latitude,longitude);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng,zoom);
        mMap.moveCamera(cameraUpdate);
    }

    public void alertBox(final double latitude, final double longitude)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setMessage("Do you want to add Geofence !")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dangerousArea.add(new LatLng(latitude,longitude));

                        // adding data to Firebase
                        data_model = new Data_Model();
                        data_model.setLatitude(latitude);
                        data_model.setLongitude(longitude);
                        if(radius==0)
                        {
                          data_model.setRadius(2000);
                        }
                        else
                        {
                            data_model.setRadius(radius);
                        }

                        data_modelList.add(data_model);
                        addGeofences();
                        geoFence_Ref.child("location "+(maxid+1)).setValue(data_model);

                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.setTitle("Add Geofence");
        alertDialog.show();
    }
}
