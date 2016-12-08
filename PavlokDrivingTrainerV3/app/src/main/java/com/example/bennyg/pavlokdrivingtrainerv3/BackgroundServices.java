package com.example.bennyg.pavlokdrivingtrainerv3;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jaredrummler.android.processes.AndroidProcesses;
import com.jaredrummler.android.processes.models.AndroidAppProcess;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cz.msebera.android.httpclient.Header;

/**
 * Created by bennyg on 11/30/16.
 * The background service class is the code that runs in the background, leaving
 * the user free to open up google maps or waze or a gps app of their choice while
 * not being distracted by calls, being notify of the speed limit and other's bad
 * driving
 */
public class BackgroundServices extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String ACTION = "android.test.BROADCAST";

    static String speedlimit = null;
    static float yourspeed = 0.0f;
    static boolean paused = false;

    /*
    Static methods for use for communicating with the Http Request methods as they are
    done inside of a thread, as such these need to be syncronized and static
     */

    public static synchronized void setSpeed(String s) {
        speedlimit = s;
    }

    public static synchronized String getSpeed() {
        return speedlimit;
    }

    public static synchronized void setYourSpeed(float s) {
        yourspeed = s;
    }

    public static synchronized float getYourspeed(){
        return yourspeed;
    }

    public static synchronized void setPaused(Boolean b) {
        paused = b;
    }

    public static synchronized boolean isPaused() {return paused;}

    private
        Context c;
        String previouslocation,token, message, username;
        LatLng destination;
        Timer t1,t2;

        int zapvalue, vibvalue;

        GoogleApiClient mGoogleApiClient;
        Location mLastLocation;
        LocationRequest mLocationRequest;

        ArrayList<String> parentSMS;
        ArrayList<String> parentName;
        boolean texted = false,textdriving = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /*
    Start of the background service, here we get all the data we passed into the
    Bundle(the token, text message, destination, zap and shock values)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        super.onStartCommand(intent,flags,startid);

        if (intent.getExtras() == null) {
            onDestroy();
        }

        token = intent.getExtras().getString("token");
        message = intent.getExtras().getString("message");
        destination = new LatLng(intent.getExtras().getDouble("latitude"),
                intent.getExtras().getDouble("longitude"));
        username = intent.getExtras().getString("username").split("@")[0];
        zapvalue = Math.max(1,intent.getExtras().getInt("zap"));
        vibvalue = Math.max(1,intent.getExtras().getInt("vib"));

        parentName = new ArrayList<>();
        parentSMS = new ArrayList<>();
        setParentSMS();

        return START_STICKY;
    }

    @Override
    public void onCreate() {

        //Just to alert the user that the background service has started
        Toast.makeText(this,"Background services started",Toast.LENGTH_SHORT).show();

        //Grabs the context for use later in the app this is mainly to show text
        c = getApplicationContext();

        //Calls the notification wrapper class to display the notification
        //false is used to show that the service is NOT paused
        NotificationWrapper.displayNotification(c,false);

        //Setups the telephone manager and listener which we defined
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        listener callStateListener = new listener();

        //binds the listener
        telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        //setups the google api client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        //createNotification();

        //connects the google api client
        mGoogleApiClient.connect();

        /*
        T1 is used for the text messaging it will check if the user is texting every 5 seconds
         */
        t1 = new Timer();
        t2 = new Timer();

        t1.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!isPaused())
                    getStats();
            }
        },0,5000);

        //T2 is used for speeding, itll check the users speed every minute and see if that is
        //higher than the speed limit
        //It passes in the speed and speedlimit from the static methods above
        t2.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!isPaused())
                    checkSpeedLimit(getSpeed(),getYourspeed());
                textdriving = false;
                //we reset every 1 minutes so the people in the area dont get overwhelemed by someone texting
            }
        },0, 60000);

    }

    @Override
    public void onDestroy() {
        Log.d("CS591", "BackgroundServices is destroyed");
        //Discconects the google api client
        mGoogleApiClient.disconnect();
        //Stops the two timers
        t1.cancel();
        t2.cancel();
        //Cancels the notification
        NotificationWrapper.cancel();
        super.onDestroy();

    }

    /*
    Google Maps Functions
     */
    //Creates the google maps location request
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onConnected(Bundle arg0) {
        // Once connected with google api, get the location and creates request
        createLocationRequest();
        //noinspection MissingPermission
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    /*
    Methods that need to be overidden from the google api interfaces
     */
    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    //Location changed is called every time the location is changed
    @Override
    public void onLocationChanged(Location location) {
        // Assign the new location

        //sendStimuli("beep",255);

        //if the service is paused dont do anything
        if (paused) {
            return;
        }

        //mLastLocation = location;
        //grabs the lasat known location
        //noinspection MissingPermission
        mLastLocation = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);

        //dr.updateChildren();
        if (mLastLocation != null) {
            //Gets the lat and long from the location
            double latitude = mLastLocation.getLatitude();
            double longitude = mLastLocation.getLongitude();

            //debug code
            /*Toast.makeText(c,
                    "latitude : " + latitude + "longitude : " + longitude ,
                    Toast.LENGTH_SHORT).show();*/

            //Stores the previous location to see if we should update the database or not
            //What we have here is taking the lat and long, dividing by 5 and creating a string
            //based off that number, this string would be unique to 5 lat by 5 long block
            //This is used for the alerting the user if someone is texting and driving
            if (previouslocation == null) {
                previouslocation = ((int) latitude / 5) + "" + ((int) longitude / 5);
                FirebaseDatabase.getInstance().getReference().child(previouslocation).child(username).setValue(token);
            } else if (!(previouslocation.equals(((int) latitude / 5) + "" + ((int) longitude / 5)))) {
                previouslocation = ((int) latitude / 5) + "" + ((int) longitude / 5);
                FirebaseDatabase.getInstance().getReference().child(previouslocation).child(username).setValue(token);
            }

            //Checks if we arrived at the destination yet
            if (Math.abs(location.getLatitude() - destination.latitude) <= .01 &&
                    Math.abs(location.getLongitude() - destination.longitude) <= 0.01) {
                paused = true;
                Toast.makeText(
                        c,
                        "Destination reached",
                        Toast.LENGTH_SHORT
                ).show();
                onDestroy();
            }

            //Make a call to the TomTom api to get the speed limit and see if its differnt from the previous one
            new AsyncHttpClient().get("https://api.tomtom.com/search/2/reverseGeocode/" + latitude + "," + longitude +
                            ".json?key=3j7npjesugrzb9ajh7ce83uy&returnSpeedLimit=true&roadUse=[\"Arterial\"]",
                    new AsyncHttpResponseHandler() {

                        @Override
                        public void onStart() {
                            // called before request is started
                        }

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                            JSONObject j;
                            try {
                                //The output of TomTom is in a deep json tree thus we have to
                                //break up the json tree to get to the speed limit
                                String s;
                                s = new String(response, "UTF-8");
                                j = new JSONObject(s);
                                j = (JSONObject) j.getJSONArray("addresses").get(0);
                                s = j.getJSONObject("address").get("speedLimit").toString();

                                //if the speed limit is different from the previous speed limit
                                if (!s.equals(getSpeed())) {
                                    setSpeed(s);//sets the speed in the static method since this is inside a thread call
                                    Toast.makeText(
                                            c,
                                            getSpeed() + " New speed limit",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }

                                //If we currently have a speed, calls the speed limit check method
                                if (mLastLocation.hasSpeed()) {
                                    setYourSpeed(mLastLocation.getSpeed());

                                }

                        /*Toast.makeText(c,
                                "Got SPEED LIMIT IS" + output,
                                Toast.LENGTH_SHORT).show();*/
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                            Log.d("TeamB", "" + statusCode);
                        }

                        @Override
                        public void onRetry(int retryNo) {
                            // called when request is retried
                        }
                    });
            Log.d("CS591", latitude + ", " + longitude);

        } else {

            Log.d("CS591", "(Couldn't get the location. Make sure location is enabled on the device)");
        }

        // Displaying the new location on UI
        //displayLocation();
    }



    /*
    Phone manager methods
     */

    //This is the listener that gets called when there is a change in phone state
    //For example, calling, recieving, hanging up
    public class listener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            //if(logAtive) Log.i(LOG_TAG,incomingNumber + " " + state);

            if (isPaused()) {
                return;
            }

            if(state== TelephonyManager.CALL_STATE_RINGING){
                //If someone is calling us this will get called
                /*Toast.makeText(c,"Phone is ringing." + incomingNumber,
                        Toast.LENGTH_LONG).show();//displays the number in a text*/

                //kills the phone call
                killCall(getApplicationContext());

                //If the caller is not private then we can send them a text message
                if (incomingNumber != null) {
                    Log.d("CS591", incomingNumber);
                    SmsManager smsManager = SmsManager.getDefault();//grabs the default sms app
                    smsManager.sendTextMessage(incomingNumber, null, message, null, null);
                    //sends the predefined sms message through said app
                }

            }
            if(state==TelephonyManager.CALL_STATE_OFFHOOK){
                //Toast.makeText(c,"You are in a call. "+incomingNumber,Toast.LENGTH_LONG).show();
                //debug code
            }
            if(state==TelephonyManager.CALL_STATE_IDLE){
                //Toast.makeText(c,"You are in idle state… ", Toast.LENGTH_LONG).show();
                //Debug code
            }
        }
    }

    //Method used to kill the phone call
    //Not recommended , the code was obtained from
    //http://stackoverflow.com/questions/23097944/can-i-hang-up-a-call-programmatically-in-android
    public boolean killCall(Context context) {
        try {
            // Get the boring old TelephonyManager
            TelephonyManager telephonyManager =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            // Get the getITelephony() method
            Class classTelephony = Class.forName(telephonyManager.getClass().getName());
            Method methodGetITelephony = classTelephony.getDeclaredMethod("getITelephony");

            // Ignore that the method is supposed to be private
            methodGetITelephony.setAccessible(true);

            // Invoke getITelephony() to get the ITelephony interface
            Object telephonyInterface = methodGetITelephony.invoke(telephonyManager);

            // Get the endCall method from ITelephony
            Class telephonyInterfaceClass =
                    Class.forName(telephonyInterface.getClass().getName());
            Method methodEndCall = telephonyInterfaceClass.getDeclaredMethod("endCall");

            // Invoke endCall()
            methodEndCall.invoke(telephonyInterface);

        } catch (Exception ex) { // Many things can go wrong with reflection calls
            Log.d("CS591","PhoneStateReceiver **" + ex.toString());
            return false;
        }
        return true;
    }

    /*
    Speed Limit methods
     */

    public void checkSpeedLimit(String output, float speed) {

        //sendStimuli("beep",255); debugging

        Double speedlimit = 0.0;

        if (output == null) {
            return;
        }

        //parses the code if its in MPH or KM and converts the speed
        //from location manager which is in meters/second to the appropiate
        //unit
        if (output.contains("MPH")) {
            speed *= 2.23694;
            speedlimit = Double.parseDouble(output.split("MPH")[0]);
        }else if (output.contains("KM")) {
            speed *= 3.6;
            speedlimit = Double.parseDouble(output.split("KM")[0]); // converts speed limit to double
        }

        if (speed >= speedlimit) {
            sendStimuli("shock",zapvalue);// if the user is over the speed limit shock the user
        }else if((speed/speedlimit) >= .70){
            sendStimuli("vibration",vibvalue); // if they are close to the speed limit beeps and vibrate to tell them
            sendStimuli("beep",255); // to slow down
        }

        Log.d("TEAMB", "Speed is " + speed + " Speed limit is " + speedlimit); //debug purposes
    }

    /*
    Texting & Driving methods
     */

    //Uses a libary to get the apps running does not work on versions above Marshmallow
    private void getStats() {
        List<AndroidAppProcess> processes = AndroidProcesses.getRunningForegroundApps(getApplicationContext());
        for (AndroidAppProcess apps: processes) {
            //From the apps that are running checks if they are the default sms app
            if (apps.getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(c))) {

                Log.d("TEAMB", "Messaging app is open Shock user");

                Log.d("TEAMB","DEFAULT:"+Telephony.Sms.getDefaultSmsPackage(c));

                //shocks the user since they should not be texting and driving
                //calls the server to alert others
                //also text the people the user listed as parents

                sendStimuli("shock",zapvalue);
                if (!textdriving) {
                    textdriving = true;
                    serverCall(previouslocation, username.split("@")[0]);
                }
                textParents();

                return;
            }
        }
    }

    private void textParents() {
        //To avoid having to text the parents over and over again
        //If we texted the parents before set it equal to true
        if (texted) {
            return;
        }

        //From our previous array list grab the names and numbers
        //and send a message saying they are texting and driving
        String name, number;
        for (int x = 0; x < parentName.size();x++) {
            number = parentSMS.get(x);
            name = parentName.get(x);

            if (number != null) {
                try {
                    //sends the sms message
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(
                            number,
                            null,
                            "Hi " + name + ". I am texting and driving!!)",
                            null,
                            null);
                }catch (Exception e) {
                    return;
                }
            }
        }
        texted = true;
    }

    /*
    Http Post request methods
     */

    //Calls the pavlok api with the appropiate stimuli and value we pass in
    private void sendStimuli(final String stimuli, int value) {
        RequestParams params = new RequestParams();
        params.put("access_token",token);

        new SyncHttpClient().post(
                "http://pavlok-mvp.herokuapp.com/api/v1/stimuli/"+ stimuli +"/" + value,
                params,
                new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        Log.d("TEAMB",statusCode+" sucess " + stimuli);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        Log.d("TEAMB",statusCode+" failed " + stimuli);
                    }
                }
        );
    }

    //calls our server to inform others that the user is texting and driving
    private void serverCall(String location, String id) {
        new SyncHttpClient().post(
                "https://www.pavlokdrivingtrainerv3.appspot.com/alert-user?id="+id+"&location="+location,
                null,
                new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        Log.d("TEAMB",statusCode+"WORK!!!!!!!!!!!!!");
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        Log.d("TEAMB",statusCode+"DID NOT WORK");
                    }
                }
        );
    }

    /*
    Database caller that grabs the Parent SMS's from the database,
    There was an issue with grabbing an ArrayList Object from Bundle,
    Also this will be the most up to date list
     */

    public void setParentSMS() {
        DatabaseReference ref = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("Parents");

        //sendStimuli("vibration",255);

        ref.child(username.split("@")[0])
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot d: dataSnapshot.getChildren()) {
                            Iterator<DataSnapshot> children = dataSnapshot.getChildren().iterator();

                            while (children.hasNext()) {
                                DataSnapshot childSnapshot = children.next();
                                //Grabs the parents names and numbers from the server
                                //And place them in the respective array list respectively
                                if (!parentName.contains((String) childSnapshot.getKey())) {
                                    parentName.add((String) childSnapshot.getKey());
                                    parentSMS.add((String) childSnapshot.getValue());
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

}
