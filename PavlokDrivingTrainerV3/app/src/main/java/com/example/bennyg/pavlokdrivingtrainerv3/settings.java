package com.example.bennyg.pavlokdrivingtrainerv3;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Iterator;

public class settings extends Fragment {

    private
        SeekBar sbVib,sbZap;
        TextView vib, zap;
        Button btnSetting;
        dataExchange comm;

    public settings() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragV =  inflater.inflate(R.layout.fragment_settings, container, false);

        zap = (TextView) fragV.findViewById(R.id.tvZapV);
        vib = (TextView) fragV.findViewById(R.id.tvVibV);
        sbVib = (SeekBar) fragV.findViewById(R.id.sbVibrate);
        sbZap = (SeekBar) fragV.findViewById(R.id.sbZap);

        btnSetting = (Button) fragV.findViewById(R.id.btnSetSettings);

        sbZap.setOnSeekBarChangeListener(new zapListener());

        sbVib.setOnSeekBarChangeListener(new vibListener());

        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Calls the parent Homescreen and sets the vibration and zap values
                //Note we are maxing the Zap/Shock value at 200 instead of the 255 for Safety reasons
                comm.setDataP((int)(sbVib.getProgress()/10.0)*255,(int)(sbZap.getProgress()/10.0)*200);
                Toast.makeText(getContext(), "Settings set", Toast.LENGTH_SHORT).show();
            }
        });

        //This gets the value from the Database if it exists and sets the slider and textview to that number
        setData();

        return fragV;
    }


    /*
    Gets the saved zap and vib values from the database and sets them
     */
    private void setData() {
            DatabaseReference ref = FirebaseDatabase
                    .getInstance()
                    .getReference()
                    .child("Settings");

            ref.child(comm.getUser().split("@")[0])
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for (DataSnapshot d: dataSnapshot.getChildren()) {
                                Iterator<DataSnapshot> children = dataSnapshot.getChildren().iterator();

                                while (children.hasNext()) {
                                    DataSnapshot childSnapshot = children.next();
                                    if (childSnapshot.getKey().equals("ZAP")) {
                                        //sets the Zap textview and slider
                                        sbZap.setProgress(Integer.parseInt(childSnapshot.getValue().toString()));
                                        zap.setText(sbZap.getProgress()*10+"");
                                    }else if (childSnapshot.getKey().equals("VIB")) {
                                        //sets the Vibrate textview and slider
                                        sbVib.setProgress(Integer.parseInt(childSnapshot.getValue().toString()));
                                        vib.setText(sbVib.getProgress()*10+"");
                                    }
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
    }

    /*
    The listners for the sliders that update the TextView when the data is changed
     */

    private class zapListener implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            zap.setText(progress*10+"");
            Log.d("TEAMB",progress+"");
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }

    private class vibListener implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            vib.setText(progress*10+"");
            Log.d("TEAMB",progress+"");
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            comm = (dataExchange) context;
        }catch(Exception e){

        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        //Saves the values to the Firebase database on detach
        FirebaseDatabase
                .getInstance()
                .getReference()
                .child("Settings")
                .child(comm.getUser().split("@")[0])
                .child("ZAP")
                .setValue((int)(sbZap.getProgress()));

        FirebaseDatabase
                .getInstance()
                .getReference()
                .child("Settings")
                .child(comm.getUser().split("@")[0])
                .child("VIB")
                .setValue((int)(sbVib.getProgress()));
    }
}
