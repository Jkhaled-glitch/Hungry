package com.example.hungry;



import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.microsoft.maps.Geopoint;
import com.microsoft.maps.MapAnimationKind;
import com.microsoft.maps.MapElementLayer;
import com.microsoft.maps.MapIcon;
import com.microsoft.maps.MapImage;
import com.microsoft.maps.MapRenderMode;
import com.microsoft.maps.MapScene;
import com.microsoft.maps.MapTappedEventArgs;
import com.microsoft.maps.MapView;
import com.microsoft.maps.search.MapLocation;
import com.microsoft.maps.search.MapLocationFinder;
import com.microsoft.maps.search.MapLocationFinderResult;
import com.microsoft.maps.search.MapLocationFinderStatus;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.OkHttpClient;



public class MainActivity extends AppCompatActivity {

    private  final  static int REQUEST_CODE=100;

    EditText editText;
    Button buttonSearch, ButtonLocation;
    FusedLocationProviderClient fusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.editTextTextPersonName);
        buttonSearch = findViewById(R.id.button);
        ButtonLocation = findViewById(R.id.buttonLocation);
        fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(this);

        //getLastLocation();



        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (TextUtils.isEmpty(editText.getText())) {
                    editText.setError( getResources().getString(R.string.enter_city_name_error) );
                } else {
                    String  city = editText.getText().toString();

                    searchCityCoordinates(city);
                    //FindNearBy(lat,lon);

                }
            }
        });

        ButtonLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getLastLocation();

            }
        });


    }
    private void getLastLocation() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location !=null){
                                Geocoder geocoder=new Geocoder(MainActivity.this, Locale.getDefault());
                                List<Address> addresses= null;
                                try {

                                    addresses = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);
                                    double lat=addresses.get(0).getLatitude();
                                    double lon =addresses.get(0).getLongitude();


                                    displayMap(lat,lon);

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }

                        }
                    });
        }else
        {
            //ask localisation permission
            ActivityCompat.requestPermissions(MainActivity.this, new String[]
                    {android.Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_CODE);

        }


    }


    private void displayMap(double latitude, double longitude){
        ParcelableGeopoint myLocation = new ParcelableGeopoint(latitude, longitude);

        MapFragment mapFragment = new MapFragment();
        Bundle args = new Bundle();
        //send the user location
        args.putParcelable("myLocation", myLocation);

        mapFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mapFragment)
                .commit();
    }


    private void searchCityCoordinates(String cityName) {
        String url = "https://nominatim.openstreetmap.org/search?q=" + cityName + "&format=json";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            if (response.length() > 0) {

                                JSONArray responseArray = new JSONArray(response);

                                if (responseArray.length()!=0) {
                                    JSONObject firstEntry = responseArray.getJSONObject(1);
                                    double lat = Double.parseDouble(firstEntry.getString("lat"));
                                    double lon = Double.parseDouble(firstEntry.getString("lon"));
                                    //Afficher le map
                                    displayMap(lat,lon);
                                } else {
                                    Toast.makeText(getApplicationContext(),
                                            getResources().getString(R.string.msg_no_restaurants),
                                            Toast.LENGTH_LONG).show();
                                }
                            } else {
                                if (!isNetworkAvailable(getApplicationContext())) {
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.msg_network_error), Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.msg_city_error), Toast.LENGTH_LONG).show();
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (!isNetworkAvailable(getApplicationContext())) {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.msg_network_available), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.msg_city_error), Toast.LENGTH_LONG).show();
                }
            }
        });
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        requestQueue.add(stringRequest);
    }

    // Vérifier si le terminal est connecté à Internet
    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private void clearPins() {
        //mPinLayer.getElements().clear();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode==REQUEST_CODE){

            if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getLastLocation();
            }
            else {
                Toast.makeText(this, getResources().getString(R.string.permission), Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}

