package com.example.bennyg.pavlokdrivingtrainerv3;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.util.ArrayList;
import java.util.Iterator;

import cz.msebera.android.httpclient.Header;

public class Homescreen extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        flipcomm, dataExchange, showDialog{

    //Implements 3 userdefined interfaces for use for communication with fragments

    //flipcomm is used for communication with the Wrapper class
    //which is where the user sets the destination, text message and starts the app

    //dataExchange is for use with communciation with the Settings fragment

    //showDialog is for use with the addParent fragment where the user sets parents/child

    private
        int zapvalue = 20, vibvalue = 20;

        LatLng destinationLoc;
        String textmessage,token,username;

        wrapper.PagerAdapter vfadapt;

        TextView display;

        ArrayList<String> parentSMS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homescreen);

        //grabs the token and username from the Bundle
        token = getIntent().getStringExtra("token");
        username = getIntent().getStringExtra("username");

        //At the very start of the call we need to set
        //a default fragment or else it will cause an error
        //That is because the menu layout does not have a default layout for
        //main content area
        Fragment fragment = null;
        Class fragmentClass = null;
        fragmentClass = wrapper.class;

        //Instantiates the fragment so we can interact with the fragment
        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Setup the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Replaces the the main fragment with the wrapper fragment we defined above
        //This inflate sthe fragment while we instatiate above
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.frHomescreen, fragment).commit();

        //Setup the left drawer we pull from the side, we have a xml drawer layout that defines the menu
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        //drawer.setDrawerListener(toggle);
        toggle.syncState();

        //Setup the navigation view
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //This is the box at the top of the menu, just to add a little personal touch and for confirmation
        //That the user logged in correctly we display their email as their username
        display = (TextView)navigationView.getHeaderView(0).findViewById(R.id.tvDisplayName);
        display.setText(getIntent().getStringExtra("username").toString());

        //This is the parent child data, we instantiate the ArrayList
        //then set the values
        parentSMS = new ArrayList<String>();
        setParentSMS();

    }

    /*
        Calls the firebase database where the parents data is stored and sets them in parentSMS
     */
    public void setParentSMS() {
        //Firebase path
        DatabaseReference ref = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("Parents");

        //method to get the data from database
        ref.child(username.split("@")[0])
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot d: dataSnapshot.getChildren()) {
                            Iterator<DataSnapshot> children = dataSnapshot.getChildren().iterator();

                            while (children.hasNext()) {
                                //This converts the data into a common format
                                //as in Firebase it is in a key:value setup
                                DataSnapshot childSnapshot = children.next();
                                String s =((String) childSnapshot.getKey() + ":" +
                                        (String) childSnapshot.getValue() );
                                if (!parentSMS.contains(s)) {
                                    parentSMS.add(s); //adds it to the arraylist
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
         Start of flipcomm Interface methods for use when commincating with the
         destination, message and startBackground services
     */

    //sets the message and location for the startBackground fragment which is the conformation screen
    @Override
    public void setData(String message, LatLng location) {
        if (message != null && !message.equals("")) {
            textmessage = message;
        }
        if (location != null) {
            destinationLoc = location;
        }
        //calls the adapter to communication with the fragment as
        //we should not communicate with fragments directly inside a viewpager
        vfadapt.setData(textmessage,destinationLoc);
    }

    //getter methods for the message and location, this way we arent accessing values directly
    @Override
    public String getMessage() {
        return textmessage;
    }

    @Override
    public LatLng getLocation() {
        return destinationLoc;
    }

    //sets the adapter from wrapper so Homescreen can communicate with it
    //The adapter is like the manager for the viewpager
    @Override
    public void setAdapter(wrapper.PagerAdapter fpa) {
        vfadapt = fpa;
    }

    //This method is called when the user clicks the button on startBackground
    /*
        Start background services will start the service that will run in the background
        while also launching a implicit intent so the user can choose which App they
        want to use for navigation
     */
    @Override
    public void startBackgroundServices() {
        //Intent to launch the navigation app
        Intent maps = new Intent(Intent.ACTION_VIEW,
                Uri.parse("geo:" + destinationLoc.latitude + ","+
                        destinationLoc.longitude));

        //Creates the intent for the background services
        //Also puts all the information gained from the previous fragments
        //in the bundle
        Intent i = new Intent(getApplicationContext(), BackgroundServices.class);
        Bundle b = new Bundle();

        //puts the values inside a bundle
        b.putString("token",token);
        b.putString("message",textmessage);
        b.putDouble("latitude",destinationLoc.latitude);
        b.putDouble("longitude",destinationLoc.longitude);
        b.putString("username",username);
        b.putInt("zap",zapvalue);
        b.putInt("vib",vibvalue);

        //attaches the bundle to the intent
        i.putExtras(b);

        //starts the geo location intent and the Background service
        startActivity(maps);
        startService(i);
    }

    @Override
    public void stopBackgroundServices() {
        stopService(new Intent(getApplicationContext(), BackgroundServices.class));
        Toast.makeText(this, "Background service stopped!", Toast.LENGTH_SHORT).show();
        if (NotificationWrapper.notificationManager != null) {
            NotificationWrapper.notificationManager.cancel(NotificationWrapper.NOTIFICATION_ID);
        }
    }

    /*
         Start of dataExchange Interface methods
     */
    //Gets the zap and vib threshould set by the user in the settings fragment
    @Override
    public void setDataP(int zap, int vib) {
        zapvalue = zap;
        vibvalue = vib;
    }

    //getter method for the username
    @Override
    public String getUser() {
        return  username;
    }

    //if the user clicks the back button signs them out and closes the menu
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
        signout();
    }

    /*
        This is the listener method for when the user
        clicks one of the menu icons. This is done by fragments,
        When the user one of the menus icons we will instantiate
        the fragment and replace it using the fragment manager
     */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        //Fragment manager
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        Fragment fragment = null;
        Class fragmentClass = null;

        //checks which menu button was clicked
        if (id == R.id.nav_settings) {
            fragmentClass = settings.class; //sets the fragments
        } else if (id == R.id.nav_startDriving) {
            fragmentClass = wrapper.class; //sets the fragments
        } else if (id == R.id.nav_signout) {
            signout(); //signs the user out
            return true;
        }else if (id == R.id.nav_parent) {
            fragmentClass = addParent.class; //sets the fragments
        }

        try {
            fragment = (Fragment) fragmentClass.newInstance();
            //Instantiate the fragment so we can interact with them
        } catch (Exception e) {
            e.printStackTrace();
        }

        //replaces the current fragment with the one that was clicked
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.frHomescreen, fragment).commit();

        //closes the drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);

        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);

        return true;
    }

    /*
        Signout the user by sending a POST request to the API
     */

    public void signout() {

        RequestParams params = new RequestParams();
        params.put("access_token", token);
        params.put("token", token);

        new AsyncHttpClient().post(
                "http://pavlok-mvp.herokuapp.com/api/v1/sign_out",
                params,
                new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        //if the user is sucessfully signed out then
                        //stops the background service and goes to the login screen
                        stopService(new Intent(getApplicationContext(), BackgroundServices.class));
                        Intent i = new Intent(getApplicationContext(), MainActivity.class);
                        if (NotificationWrapper.notificationManager != null) {
                            NotificationWrapper.notificationManager.cancel(NotificationWrapper.NOTIFICATION_ID);
                        }
                        startActivity(i);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                    }
                }
        );


    }

    /*
        Start of showDialog interface methods
     */

    /*
        We cannot create a Dialog within the fragment class thus, we create here in the
        Homescreen class
    */
    @Override
    public void showDialogFrag(final ArrayAdapter<String> adapter) {

        //grabs the layout inflater
        LayoutInflater li = LayoutInflater.from(this);
        //get the view of the dialog fragment
        View promptsView = li.inflate(R.layout.fragment_dialog_get_parent, null);
        //Builds the alertDialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        //sets the alert Dialog to the layout
        alertDialogBuilder.setView(promptsView);

        //Grabs the  EditText to get the name and number the user entered
        final EditText etUser = (EditText) promptsView.findViewById(R.id.etNameP);
        final EditText etNumber = (EditText) promptsView.findViewById(R.id.etNumberP);

        //Builds the alert dialog
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                /*
                                    If the user enters the correct data and clicks ok,
                                    saves the data and sends the information to the
                                    adapter to set name and number in the listview
                                 */
                                String name = etUser.getText().toString();
                                String number = etNumber.getText().toString();

                                if (name == null || name.equals("") ||
                                        number == null || number.equals("")) {
                                    Toast.makeText(getApplicationContext()
                                            , "You've entered something wrong try again"
                                            , Toast.LENGTH_SHORT);
                                    return;
                                }

                                adapter.add(name + ":" + number);
                                adapter.notifyDataSetChanged();
                                Toast.makeText(getApplicationContext()
                                        , "Parent added"
                                        , Toast.LENGTH_SHORT);
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel(); // cancels the dialog
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show(); //shows and creates the dialog
    }

    @Override
    public void showDeleteDialog(final ArrayAdapter<String> adapter,final String s) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to delete parent")
                .setTitle("Delete Parent");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                adapter.remove(s);
                FirebaseDatabase
                        .getInstance()
                        .getReference()
                        .child("Parents")
                        .child(getUsername().split("@")[0])
                        .child(s.split(":")[0]).removeValue();
                Toast.makeText(getApplicationContext()
                        , "Parent deleted"
                        , Toast.LENGTH_SHORT);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                dialog.cancel();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //Getter method for username
    @Override
    public String getUsername() {
        return username; // returns username
    }

    //sets the parent array
    @Override
    public void setParents(ArrayList<String> pars) {
        parentSMS = pars; //sets the parent sms list
    }

    //gets the parent array
    @Override
    public ArrayList<String> getParents() {
        return parentSMS;
    }


}
