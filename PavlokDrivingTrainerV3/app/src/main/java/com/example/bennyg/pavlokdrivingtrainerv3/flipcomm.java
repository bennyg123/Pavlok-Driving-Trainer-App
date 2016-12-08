package com.example.bennyg.pavlokdrivingtrainerv3;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by bennyg on 11/30/16.
 */

/*
Inteface used to communicate with the destination, message and startBackground fragment
 */

public interface flipcomm {
    void setData(String message, LatLng location);
    String getMessage();
    LatLng getLocation();

    void setAdapter(wrapper.PagerAdapter fpa);
    void startBackgroundServices();
    void stopBackgroundServices();
}
