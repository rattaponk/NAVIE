package com.rattapon.navie;

import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.rattapon.navie.Fragment.EventsFragment;
import com.rattapon.navie.Fragment.MapFragment;

public class EventsActivity extends AppCompatActivity {

    private android.support.v7.widget.Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        initInstance();
        setMapFragment();
    }

    public void initInstance() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    public void setMapFragment() {
        Fragment eventsFragment = EventsFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.layout_fragment_container, eventsFragment)
                .commit();
    }
}
