package com.example.hungry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.microsoft.maps.Geopoint;
import com.microsoft.maps.MapAnimationKind;
import com.microsoft.maps.MapElementLayer;
import com.microsoft.maps.MapIcon;
import com.microsoft.maps.MapImage;
import com.microsoft.maps.MapRenderMode;
import com.microsoft.maps.MapScene;
import com.microsoft.maps.MapView;
import com.microsoft.maps.routing.MapRoute;
import com.microsoft.maps.routing.MapRouteDrivingOptions;
import com.microsoft.maps.routing.MapRouteOptimization;
import com.microsoft.maps.routing.MapRouteRestrictions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapFragment extends Fragment {

    private MapView mMapView;
    private MapElementLayer mPinLayer;
    private MapElementLayer mUserPinLayer;
    private MapImage mPinImage;
    private MapImage mUserPinImage;
    private int mUntitledPushpinCount = 0;
    private static final String MY_API_KEY = "AlwLTKgevIemLkhFY8wA2oDQwpxY8SBBAR8a5dXymXDFKTmfGWKkXnJGQkGzXUMM";

    public MapFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_map, container, false);

        // Initialize the map view
        mMapView = rootView.findViewById(R.id.map_container);
        mMapView.setCredentialsKey(MY_API_KEY);
        mMapView.onCreate(savedInstanceState);

        // Set the map scene to show both locations
        Bundle args = getArguments();

        ParcelableGeopoint parcelableMyLocation = args.getParcelable("myLocation");
        double myLatitude = parcelableMyLocation.getLatitude();
        double myLongitude = parcelableMyLocation.getLongitude();
        Geopoint myLocation = new Geopoint(myLatitude, myLongitude);
        mMapView.setScene(
                MapScene.createFromLocationAndZoomLevel(myLocation, 13),
                MapAnimationKind.NONE);



        // Add pins for user location
        mPinLayer = new MapElementLayer();
        mMapView.getLayers().add(mPinLayer);
        mPinImage = getPinImage("user");
        addPin(myLocation, "Position","user");
       //search and display nearby restaurants

        FindRestaurantsByLocation(myLatitude,myLongitude);


        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Add the map view to the fragment's layout
        FrameLayout container = view.findViewById(R.id.fragment_container);
        if (container != null) {
            container.addView(mMapView);
        }

    }

    private MapImage getPinImage(String type) {
        // Create a pin image from a drawable resource
        Drawable drawable = type == "user" ?
                ResourcesCompat.getDrawable(getResources(), R.drawable.position, null):
                ResourcesCompat.getDrawable(getResources(), R.drawable.restaurant, null) ;

        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();

// Create a bitmap of the correct size
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

// Draw the drawable onto the bitmap
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);

// Return a MapImage created from the bitmap
        return new MapImage(bitmap);
    }

    private void addPin(Geopoint location, String title,String type) {
        // Add a pin to the map at the given location
        MapIcon pushpin = new MapIcon();
        pushpin.setLocation(location);
        pushpin.setTitle(title);
        switch (type){
            case "user":
                pushpin.setImage(mUserPinImage);
                break;
            case "restaurant":
                pushpin.setImage(mPinImage);
                break;
            default:break;
        }
        pushpin.setNormalizedAnchorPoint(new PointF(0.5f, 1f));
        if (title.isEmpty()) {
            pushpin.setContentDescription(String.format(
                    Locale.ROOT,
                    "Untitled pushpin %d",
                    ++mUntitledPushpinCount));
        }
        mPinLayer.getElements().add(pushpin);
    }

    public void FindRestaurantsByLocation(double latitude, double longitude) {
        int limit = 10;
        int distanceKM=10;
        String url = "https://travel-advisor.p.rapidapi.com/restaurants/list-by-latlng?latitude=" + latitude + "&longitude=" + longitude + "&limit="+limit+"&currency=USD&distance="+distanceKM+"&open_now=false&lunit=km&lang=en_U";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    JSONArray data=jsonObject.getJSONArray("data");
                    // Créer une liste de restaurants
                    List<Element> elementList = new ArrayList<>();
                    int len=data.length();
                    for (int i = 0; i < len; i++) {

                        JSONObject restaurant = data.getJSONObject(i);
                        if (restaurant.has("name")) {
                            String name = restaurant.getString("name");
                            double lt = restaurant.getDouble("latitude");
                            double lg = restaurant.getDouble("longitude");
                            Element element = new Element(name,lt,lg);

                            elementList.add(element);
                        }
                    }
                    //affiche les restaurants sur le map
                    displayPlaces( elementList);

                } catch (JSONException e) {

                    e.printStackTrace();
                }

            }
        },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Code pour gérer les erreurs de requête

                        if (!isNetworkAvailable(getContext())) {
                            Toast.makeText(getContext(),
                                    getResources().getString(R.string.msg_network_available),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getContext(),
                                    getResources().getString(R.string.connexion_error),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("X-RapidAPI-Host", "travel-advisor.p.rapidapi.com");
                headers.put("X-RapidAPI-Key", "54da830830msh3b6cacf87fd75d3p196435jsn050cd5ee7973");
                return headers;
                //548a8fc0-c2a6-11ed-8c70-c5c3a0a20821
            }
        };

// Ajouter la requête à la file d'attente de Volley


        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        requestQueue.add(stringRequest);
    }

    public void displayPlaces(List<Element> elementList){
        double restaurantLat;
        double restaurantLon;
        String name;
        String subString;
        Geopoint restauLocation;
        for(Element element: elementList){
            restaurantLat = element.getLatitude();
            restaurantLon = element.getLongitude();
            name =element.getName();
            restauLocation = new Geopoint(restaurantLat, restaurantLon);
            subString = name.length()<=7 ?  name : name.substring(0, 7)+ "...";

            // Add pins for Restaurant Location
            mPinLayer = new MapElementLayer();
            mMapView.getLayers().add(mPinLayer);
            mPinImage = getPinImage("restaurant");
            addPin(restauLocation, subString, "restaurant");

        }

    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
}
