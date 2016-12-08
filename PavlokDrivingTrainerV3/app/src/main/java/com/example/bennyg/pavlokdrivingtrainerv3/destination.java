package com.example.bennyg.pavlokdrivingtrainerv3;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;

public class destination extends Fragment implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    /*
        Destination fragment where the user selects a destination to go to
     */

    private
        flipcomm parent;
        MapView mMapView;
        GoogleMap googleMap;
        Marker lastpos;
        Button destination;
        EditText destText;
        String preText;
        GoogleApiClient mGoogleApiClient;

    public destination() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragview = inflater.inflate(R.layout.fragment_destination, container, false);

        //Code for the MapView fragment & GoogleApi to get the current location
        mMapView = (MapView) fragview.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);

        mMapView.onResume(); // needed to get the map to display immediately

        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        mGoogleApiClient.connect();

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Instantiates the edit text and sets it if the user previously inputed a location
        destText = (EditText) fragview.findViewById(R.id.etDest);

        if (preText != null) {
            destText.setText(preText);
        }

        //Instantiates the Button
        destination = (Button) fragview.findViewById(R.id.btnDest);

        destination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //grabs the string location
                String strLoc = destText.getText().toString();
                List<Address> addresses;
                try {
                    Geocoder gc;
                    gc = new Geocoder(getContext());
                    try {
//                        addresses = gc.getFromLocationName("Eifel Tower", 1);  //address, max number of address resolutions.
                        //preforms a google search based off that string
                        addresses = gc.getFromLocationName(strLoc, 1);  //address, max number of address resolutions.a
                        //Some addresses dont work, we don't know why however
                        //this is the fault of the Geocoder not us so we cannot do anything about it
                        //This code is here incase the address fails
                        if (addresses.isEmpty()) {
                            Toast.makeText(
                                    getContext(),
                                    "Destination invalid",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }
                        strLoc = addresses.get(0).getLocality();  //Retrieving the "known" name from Location Services (might be different than the string we submitted.)
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }

                    //Gets the location of the destination
                    LatLng mylocation = new LatLng(addresses.get(0).getLatitude(),
                                                   addresses.get(0).getLongitude());


                    lastpos.remove();
                    //Creates a visual marker at the destination
                    lastpos = googleMap.addMarker(new MarkerOptions().position(mylocation).title("Marker Title").snippet("Marker Description"));

                    // For zooming automatically to the location of the marker
                    CameraPosition cameraPosition = new CameraPosition.Builder().target(mylocation).zoom(12).build();
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                    //The destination is set
                    Toast.makeText(getContext(), "Destination set, swipe left", Toast.LENGTH_SHORT).show();

                    //sets the location inside of Homescreen
                    parent.setData(null, mylocation);

                    //This codes makes the keyboard dissapear after pressing the button for smoother interface
                    InputMethodManager inputManager = (InputMethodManager)getActivity()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);

                    inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);

                } catch (SecurityException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), " GPS not Setup. ", Toast.LENGTH_LONG);
                }
            }

        });

        return fragview;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //cast the context from Homescreen as the flipp comm interface
        try {
            parent = (flipcomm) context;
        }catch (Exception e) {

        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        //Saves the previous location in case the user goes back to the fragment
        if (destText.getText().toString() != null) {
            preText = destText.getText().toString();
        }
    }

    /*
    This is the same code as in onClickListener except its called when we are connected with Google,
    it moves the map to the current users location
     */

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //noinspection MissingPermission
        final Location local = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        mMapView.getMapAsync(new OnMapReadyCallback() {
            @SuppressWarnings("MissingPermission")
            @Override
            public void onMapReady(GoogleMap mMap) {
                googleMap = mMap;
                LatLng mylocation;
                // For showing a move to my location button
                googleMap.setMyLocationEnabled(true);

                if (parent.getLocation() != null) {
                    mylocation = parent.getLocation();
                }else if (local == null) {
                    mylocation = new LatLng(-32,-45);
                }else{
                    mylocation = new LatLng(local.getLatitude(),local.getLongitude());
                }

                lastpos = googleMap.addMarker(new MarkerOptions().position(mylocation)
                        .title("Marker Title")
                        .snippet("Marker Description"));

                // For zooming automatically to the location of the marker
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(mylocation)
                        .zoom(12).build();
                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        });
    }


    /*
        Methods we have to override because of calling the google maps api
     */
    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
