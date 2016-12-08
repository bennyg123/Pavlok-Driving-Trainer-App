package com.example.bennyg.pavlokdrivingtrainerv3;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class message extends Fragment {

    /*
    This method sets the text message the user wants to send to the persons that call them
     */

    private
        flipcomm parent;

        EditText text_message;
        Button button_message;

    public message() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragM = inflater.inflate(R.layout.fragment_message, container, false);

        //Grabs the edit text and button from the Layout
        text_message = (EditText) fragM.findViewById(R.id.etMessage);
        button_message = (Button) fragM.findViewById(R.id.btnMessage);

        //sets a text message if there was one before/one saved in the parent(Homescreen)
        if (parent.getMessage() != null && !parent.getMessage().equals("")) {
            text_message.setText(parent.getMessage());
        }

        button_message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Makes a toast telling the user to swipe left when they set the message
                Toast.makeText(getContext(), "Text message set, swipe left", Toast.LENGTH_SHORT).show();
                //sends the message to the Homescreen activity
                parent.setData(text_message.getText().toString(),null);

                //Drops down the keyboard after the user finished setting the message
                InputMethodManager inputManager = (InputMethodManager)getActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);

                inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });

        return fragM;

    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            parent = (flipcomm) context;
            //Instantite the context so we can communicate
            //with the parent activity
        }catch (Exception e){

        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
