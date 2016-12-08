package com.example.bennyg.pavlokdrivingtrainerv3;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import cz.msebera.android.httpclient.Header;

public class Register extends AppCompatActivity {

    String TAG = "TEAMB";

    EditText name,user,pass;
    Button reg;

    /*  Class to register the user in case they do not have a pavlok account
    *   Please note that the user does login in through Oauth here as they
    * */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        name = (EditText) findViewById(R.id.etRegName);
        user = (EditText) findViewById(R.id.etRegUser);
        pass = (EditText) findViewById(R.id.etRegPass);

        reg = (Button) findViewById(R.id.btnRegister);

        //Makes a http post request to register the user and if sucessful sends them to the Homescreen
        reg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String yourname = name.getText().toString();
                final String username = user.getText().toString();
                final String password = pass.getText().toString();

                if (yourname.equals("")) {
                    Toast.makeText(getApplicationContext(),
                            "Name is empty, please type a name",
                            Toast.LENGTH_SHORT);
                }else if (username.equals("")) {
                    Toast.makeText(getApplicationContext(),
                            "Username is empty, please type a username",
                            Toast.LENGTH_SHORT);
                }else if(password.equals("")){
                    Toast.makeText(getApplicationContext(),
                            "Password is empty, please type a password",
                            Toast.LENGTH_SHORT);
                }else {

                    RequestParams params = new RequestParams();
                    params.put("username", username);
                    params.put("password", password);
                    params.put("name", yourname);

                    new AsyncHttpClient().post(
                            "http://pavlok-mvp.herokuapp.com/api/v1/sign_up",
                            params,
                            new AsyncHttpResponseHandler() {
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                                    //registration is sucessful sends the user to the login page
                                    //just for security we dont log them in automatically
                                    Toast.makeText(
                                            getApplicationContext(),
                                            "Registration Sucessful",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                    login(username,password);
                                }

                                @Override
                                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                                }
                            }
                    );
                }
            }
        });
    }

    public void login(String username, String password) {
        Intent in = new Intent(this, MainActivity.class);
        in.putExtra("password", password);
        in.putExtra("username", username);
        startActivity(in);
    }

}
