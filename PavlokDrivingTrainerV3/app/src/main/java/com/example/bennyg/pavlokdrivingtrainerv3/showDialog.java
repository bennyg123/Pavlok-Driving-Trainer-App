package com.example.bennyg.pavlokdrivingtrainerv3;

import android.widget.ArrayAdapter;

import java.util.ArrayList;

/**
 * Created by Benny G on 12/3/2016.
 */

/*
Interface used to communicate with the addParent
 */

public interface showDialog {
    public void showDialogFrag(ArrayAdapter<String> adapter);
    public void showDeleteDialog(final ArrayAdapter<String> adapter, String s);
    public String getUsername();
    public void setParents(ArrayList<String> parentSMS);
    public ArrayList<String> getParents();
}
