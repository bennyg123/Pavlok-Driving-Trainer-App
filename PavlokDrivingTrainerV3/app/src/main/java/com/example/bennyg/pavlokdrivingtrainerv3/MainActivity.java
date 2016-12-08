package com.example.bennyg.pavlokdrivingtrainerv3;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import cz.msebera.android.httpclient.Header;
//Please note our app will not work with Android nougat as one of the libaries
//we use is not compatible above marshmallow
public class MainActivity extends AppCompatActivity {

    String TAG = "TEAMB";
    private String client_secret = "023dd287e956a9de23e0578ca7ff5bb07491a30cdf127e218663f8799d6efe61";
    private String client_id = "51be8ffdfe1458b453c7452e8b9486389cfc5f2fccb2d4b7e281bf4749fc6e5c";
    private String redirect_uri = "http://pavlok-bu/auth/pavlok/result";

    Dialog dialog;
    EditText username, password;
    String u,token;
    TextView register;
    Button signin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        //Grabs the EditText, TextView and Buttons the same way we been doing all year
        username = (EditText) findViewById(R.id.etUser);
        password = (EditText) findViewById(R.id.etPass);

        register = (TextView) findViewById(R.id.tvRegister);

        signin = (Button) findViewById(R.id.btnSignIn);

        //Mainly used with registration, after the user has registered we will
        //put the password and username text in textboxes
        if (    getIntent().getExtras() != null &&
                getIntent().getExtras().getString("username") != null &&
                getIntent().getExtras().getString("password") != null) {
            username.setText(getIntent().getExtras().getString("username"));
            password.setText(getIntent().getExtras().getString("password"));
        }

        //The button on click listener
        //Starts a new intent for the Register Activity
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent reg = new Intent(getApplicationContext(), Register.class);
                startActivity(reg);
            }
        });

        //The sign in on click listener
        //grabs the string values from the edit text boxes
        signin.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String un = username.getText().toString();
                String pw = password.getText().toString();

                //Checks if the user did not enter a username or password
                //and prompts them via a toast
                if (un.equals("")) {
                    Toast.makeText(getApplicationContext(),
                            "Username is empty, please type a username",
                            Toast.LENGTH_SHORT);
                } else if (pw.equals("")) {
                    Toast.makeText(getApplicationContext(),
                            "Password is empty, please type a password",
                            Toast.LENGTH_SHORT);
                } else {
                    u = un;
                    Oauth();
                }
            }
        });
    }

    /*  setup for the oauth authetication, this creates a webview in the background
     *   For the first time it will prompt the user to authorize this app for
     *   use with Pavlok, then for subsequent uses it will get the Oauth code
     *   automatically
     */
    public void Oauth() {
        String url = "http://pavlok-mvp.herokuapp.com/oauth/authorize?client_id="
                + client_id + "&redirect_uri="
                + redirect_uri + "&response_type=code";
        Uri uri = Uri.parse(url);
        WebView webView = new WebView(MainActivity.this);
        //setups the webclient which will get the redirect with the code at then end
        WebViewClient client = new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String redirect = request.getUrl().toString();
                Log.e("redirect:", redirect);
                if (redirect.contains(redirect_uri)) {
                    dialog.dismiss();
                    handleRedirect(request.getUrl());
                    return false;
                } else {
                    return super.shouldOverrideUrlLoading(view, request);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains(redirect_uri)) {
                    dialog.dismiss();
                    Uri uri = Uri.parse(url);
                    handleRedirect(uri);
                    return false;
                } else {
                    return super.shouldOverrideUrlLoading(view, url);
                }
            }
        };

        //setups the initial login screen for the user to login in to pavlok
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(client);
        webView.requestFocus(View.FOCUS_DOWN);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                getWindowManager().getDefaultDisplay().getWidth(),
                getWindowManager().getDefaultDisplay().getHeight()
        );
        webView.loadUrl(url);
        dialog = new Dialog(MainActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.addContentView(webView, params);
        dialog.show();

    }

    //Once the oauth token is obtained, we would make a post request to PAVLOK's api
    //To get the  access_token used to interact with the pavlok device
    protected void handleRedirect(Uri uri) {
        final String code = uri.getQueryParameter("code");

        //Setting request parameters as per the PAVLOK API instruction
        RequestParams params = new RequestParams();
        params.put("client_id",client_id);
        params.put("client_secret",client_secret);
        params.put("code", code);
        params.put("grant_type","authorization_code");
        params.put("redirect_uri",redirect_uri);

        //We used a user created libary to make POST & GET REQUEST's
        //In this case we are making a post request to get the access token for Pavlok
        new AsyncHttpClient().post(
                "http://pavlok-mvp.herokuapp.com/oauth/token",
                params,
                new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        try {
                            //We get back a JSON String that contains the token if sucessful
                            JSONObject output = new JSONObject(new String(responseBody,"UTF-8"));
                            token = output.getString("access_token");
                            permissionCheck();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                    }
                });
    }

    /*
    This method checks for permissions once the we obtain a access token
    For our app we use quite a few permissions but the only one's we need to request
    are COARSE & FINE location for the google maps api & GPS, the READ PHONE STATE &
    SEND SMS are for blocking phone calls and sending sms respectivly
     */
    public void permissionCheck() {

        int coarselocal = android.support.v4.content.ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        int finelocal = android.support.v4.content.ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int readphonestate = android.support.v4.content.ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE);
        int sendsms = android.support.v4.content.ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.SEND_SMS);

        //checks if the permissions are granted else starts the dialog window to request them

        if (    coarselocal != PackageManager.PERMISSION_GRANTED ||
                finelocal != PackageManager.PERMISSION_GRANTED ||
                sendsms != PackageManager.PERMISSION_GRANTED ||
                readphonestate != PackageManager.PERMISSION_GRANTED) {

            /*  This method is similar to startActivityforRequest in that we would
            *   send in a request code to be handle upon completion of requestPermissions
            */

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.PROCESS_OUTGOING_CALLS,
                            Manifest.permission.MODIFY_PHONE_STATE,
                            Manifest.permission.SEND_SMS},
                    1010);
        }else {
            //If the user already allowed the permissions go straight to login
            login();
        }
    }

    /*  If the permissions are requested and the user allows them
     *  calls the login method
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1010: {
                login();
            }
        }
    }

    /*
     *  This method is so if the user signs out and goes into the main activity they
     *  cant click back and go back to the previous screen or click back in general
     *  at the login screen
     */

    @Override
    public void onBackPressed() {
        return;
    }

    //Login method when all is all set
    //If the token is obtained and permissions are all set
    //Previous method will call login
    public void login() {

        //creates Intent for the Homescreen class and starts it
        //also passes in the username and access_token for the
        //next activity to use

        Intent in = new Intent(this, Homescreen.class);
        in.putExtra("token", token);
        in.putExtra("username", u);
        startActivity(in);
    }

}