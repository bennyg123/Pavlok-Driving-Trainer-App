/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Servlet Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloWorld
*/

package com.example.BennyG.myapplication.backend;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MyServlet extends HttpServlet {

    //the get method doesnt do anything but was used just to test the server out

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setServiceAccount(getServletContext().getResourceAsStream("/WEB-INF/CS591B-d3c87ccc5794.json"))
                .setDatabaseUrl("https://cs591b.firebaseio.com/")
                .build();

        try {
            FirebaseApp.getInstance();
        }
        catch (Exception error){
            //Log.info("doesn't exist...");
        }

        try {
            FirebaseApp.initializeApp(options);
        }
        catch(Exception error){
            //Log.info("already exists...");
        }

        // As an admin, the app has access to read and write all data, regardless of Security Rules
        DatabaseReference ref = FirebaseDatabase
                .getInstance()
                .getReference();

        resp.getWriter().println("GET REQUEST");
        // As an admin, the app has access to read and write all data, regardless of Security Rule
    }


    //The Post method for our server it grabs all the users but us from the server and sends a vibrate call to their pavloks
    //via the API
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String location = req.getParameter("location");
        String id = req.getParameter("id");

        //Connects to our firebase through the crenditials in the included json file so we dont have to sign in
        //each time we call our server
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setServiceAccount(getServletContext().getResourceAsStream("/WEB-INF/CS591B-d3c87ccc5794.json"))
                .setDatabaseUrl("https://cs591b.firebaseio.com/")
                .build();

        try {
            FirebaseApp.getInstance();
        }
        catch (Exception error){
            //Log.info("doesn't exist...");
        }

        try {
            FirebaseApp.initializeApp(options);
        }
        catch(Exception error){
            //Log.info("already exists...");
        }

        // As an admin, the app has access to read and write all data, regardless of Security Rules
        DatabaseReference ref = FirebaseDatabase
                .getInstance()
                .getReference();
        //final String[] x = new String[1];
        final String id2 = id;
        final ArrayList<String> a = new ArrayList<>();


        ref.child(location).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (int i = 0; i < 2; i++) {

                    Iterator<DataSnapshot> children = dataSnapshot.getChildren().iterator();


                    while(children.hasNext()){
                        DataSnapshot childSnapshot = children.next();
                        //x[0] = childSnapshot.getValue().toString();
                        //grabs the other users in the general vicinity and if they arent the user
                        //get their tokens and calls the api
                        if(!(id2.equals(childSnapshot.getKey()))) {
                            try {
                                vibrate(255, childSnapshot.getValue().toString());
                            }catch(Exception e) {

                            }
                        }
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //printout for debugging purposes
        resp.getWriter().println(id);
    }

    //Pavlok API call to send a vibration to users

    public void vibrate(int intensity, String token) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost("http://pavlok-mvp.herokuapp.com/api/v1/stimuli/vibration/" + intensity);
        List<NameValuePair> params = new ArrayList<NameValuePair>(1);
        params.add(new BasicNameValuePair("access_token", token));
        httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        HttpResponse response = httpclient.execute(httppost);

    }
}
