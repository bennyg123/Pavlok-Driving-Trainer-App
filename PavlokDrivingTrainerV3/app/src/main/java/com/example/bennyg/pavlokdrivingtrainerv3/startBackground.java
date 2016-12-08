package com.example.bennyg.pavlokdrivingtrainerv3;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class startBackground extends Fragment{

    private
        flipcomm parent;

        String prevMessage;
        LatLng prevLocation;

        TextView textMessage;
        Button startBackgroundService,stopBackgroundService;
        MapView mMapView;
        Marker prev;
        GoogleMap googleMap;

    public startBackground() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("TEAMB","On Create");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("TEAMB","On Create View");
        // Inflate the layout for this fragment
        View fragB = inflater.inflate(R.layout.fragment_start_background, container, false);

        startBackgroundService = (Button) fragB.findViewById(R.id.btnBackground);
        stopBackgroundService = (Button) fragB.findViewById(R.id.btnStopService);
        textMessage = (TextView) fragB.findViewById(R.id.tvMessage);
        mMapView = (MapView) fragB.findViewById(R.id.mapView2);
        mMapView.onCreate(savedInstanceState);

        mMapView.onResume(); // needed to get the map to display immediately

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @SuppressWarnings("MissingPermission")
            @Override
            public void onMapReady(GoogleMap mMap) {
                googleMap = mMap;
                LatLng mylocation ;
                // For showing a move to my location button
                googleMap.setMyLocationEnabled(true);

                if (prevLocation == null) {
                    mylocation = new LatLng(-32, -45);
                }else {
                    mylocation = prevLocation;
                }

                if (prev != null) {
                    prev.remove();
                }

                prev = googleMap.addMarker(new MarkerOptions().position(mylocation).title("Marker Title").snippet("Marker Description"));
                // For zooming automatically to the location of the marker
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(mylocation)
                        .zoom(12).build();
                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        });

        setData(parent.getMessage(), parent.getLocation());

        if (prevMessage != null && !prevMessage.equals("")) {
            setText(prevMessage);
        }

        //Starts the background app and makes the stop button visible
        startBackgroundService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.startBackgroundServices();
                stopBackgroundService.setVisibility(View.VISIBLE);
            }
        });

        stopBackgroundService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.stopBackgroundServices();
            }
        });

        return fragB;
    }

    public void setText(String message) {
        textMessage.setText("Text Message:\n"+message);
    }

    public void setMap(final LatLng destination) {

                if (googleMap == null) {
                    return;
                }

                if (prev != null) {
                    prev.remove();
                }
                prev = googleMap.addMarker(new MarkerOptions().position(destination).title("Marker Title").snippet("Marker Description"));
                // For zooming automatically to the location of the marker
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(destination)
                        .zoom(12).build();
                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d("TEAMB","On Attach");
        try {
            parent = (flipcomm) context;
        }catch (Exception e) {

        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d("TEAMB","On Detach");
    }

    public void setData(String message, LatLng location) {
        if (textMessage != null) {
            if (message == null && location == null) {
                textMessage.setText("Please set a location & text message");
            } else if (message == null) {
                textMessage.setText("Please set a text message");
                setMap(location);
            } else if (location == null) {
                textMessage.setText("Please set a location");
                setText(message);
            } else {
                setMap(location);
                setText(message);
            }
        }
        prevMessage = message;
        prevLocation = location;
    }
}
