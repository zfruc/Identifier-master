package com.example.zhou.identifier;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button cbutton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cbutton = (Button)findViewById(R.id.button);
        Intent gsmintent = new Intent();
        gsmintent.setClass(this,GSMCellLocationActivity.class);
        startActivity(gsmintent);
    }
}
