package com.example.bennyg.pavlokdrivingtrainerv3;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.model.LatLng;


/*
This is the wrapper fragment class
This fragment class sets the Viewpager fragment so we can
smoothly transition between destination,message and startBackground.
 */
public class wrapper extends Fragment {

    private
        ViewPager mViewPager;
        flipcomm parent;

    public wrapper() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            parent = (flipcomm) context;
        }catch (Exception e) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragv = inflater.inflate(R.layout.fragment_wrapper, container, false);

        //Instantiate the ViewPager and the adapter we defined below
        mViewPager = (ViewPager) fragv.findViewById(R.id.view_pager);
        PagerAdapter pa = new PagerAdapter(getChildFragmentManager());
        mViewPager.setAdapter(pa);

        //sets the adapter
        parent.setAdapter(pa);

        return fragv;
    }

    //page adapter class which is used for interfragmetn commuinications
    //The fragments themselves would communicate with Homescreen which will then
    //commincate to the other fragments through this adapter
    public class PagerAdapter extends FragmentPagerAdapter implements flipcomm{

        destination getdest;
        message getmess;
        startBackground startB;

        public PagerAdapter(FragmentManager fm) {
            super(fm);

            //Instantiates the fragments
            getdest = new destination();
            getmess = new message();
            startB = new startBackground();
        }

        /*
        Checks the users current position inside the ViewPager and returns the corresponsing fragment
         */
        @Override
        public Fragment getItem(int position) {
            //We are returning fragments we previously instatiated instead of creating new ones each time
            //that way we are preserving data if the user clicks another menu button or something
            if (position == 0) {
                return getdest;
            }else if (position == 1) {
                return getmess;
            }else if(position == 2) {
                return startB;
            }
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }

        /*
            Flipcomm interface methods
         */
        @Override
        public void setData(String message, LatLng location) {
            startB.setData(message,location);
        }

        /*
        Only setData is used as we do not have access to the message or location or need access
        but we need to implement flipcomm to communicate with startBackground services
         */

        @Override
        public String getMessage() {
            return null;
        }

        @Override
        public LatLng getLocation() {
            return null;
        }

        @Override
        public void setAdapter(PagerAdapter fpa) {}

        @Override
        public void startBackgroundServices() {}

        @Override
        public void stopBackgroundServices() {

        }
    }

}
