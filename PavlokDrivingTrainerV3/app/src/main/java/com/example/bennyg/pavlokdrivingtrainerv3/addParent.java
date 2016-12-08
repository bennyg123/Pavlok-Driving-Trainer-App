package com.example.bennyg.pavlokdrivingtrainerv3;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class addParent extends Fragment{

    /*
    This is the fragment where the user sets the parent they want to text
    if they are texting and driving
     */

    private
        ListView lvParent;
        Button btnAdd;
        ArrayList<String> values;
        ArrayAdapter<String> adapter;
        int max;
        showDialog parent;

    public addParent() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            parent = (showDialog) context;
            //Cast the context passed from homescreen to the interface class
            //so we can access homescreen safely
        }catch (Exception e) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragV =  inflater.inflate(R.layout.fragment_addparent, container, false);

        //If there are parents in the array list gets them and place them in values
        if (parent.getParents() == null || parent.getParents().size() == 0) {
            values = new ArrayList<String>(); //sets the parents array to null
        }else {
            values = parent.getParents(); //grabs the parents array from Homescreen
        }

        lvParent = (ListView) fragV.findViewById(R.id.lvParent);
        btnAdd = (Button) fragV.findViewById(R.id.btnAdd);

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Shows the dialog fragment by calling the Homescreen
                //It is easier and safer to create the dialog from Homescreen
                parent.showDialogFrag(adapter);
            }
        });

        //This is the adapter for the listview
        adapter = new ArrayAdapter<String>(
                getContext(),
                android.R.layout.simple_list_item_1,
                values
        );

        //sets the adapter to the listview
        lvParent.setAdapter(adapter);

        //deletes the parent that the user clicked
        lvParent.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> par, View view, int position, long id) {
                String clicked = adapter.getItem(position);
                parent.showDeleteDialog(adapter, clicked);
            }

        });

        return fragV;
    }

    @Override
    public void onDetach() {
        ArrayList<String> parentNumber = new ArrayList<String>();

        //When the fragment is detached
        //Calls the Firebase database and sets the values there
        for (int i = 0; i < adapter.getCount(); i++) {
            //gets the parents name from the adapter
            String s = adapter.getItem(i);
            //adds to the database
            FirebaseDatabase
                    .getInstance()
                    .getReference()
                    .child("Parents")
                    .child(parent.getUsername().split("@")[0])
                    .child(s.split(":")[0]).setValue(s.split(":")[1]);
            parentNumber.add(adapter.getItem(i));
        }

        //sets the arraylist inside the Homescreen class
        //So that if we open the fragment again we can see the values we previously set
        parent.setParents(parentNumber);
        super.onDetach();
    }
}
