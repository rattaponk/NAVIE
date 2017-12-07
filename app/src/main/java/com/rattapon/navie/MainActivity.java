package com.rattapon.navie;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.rattapon.navie.Fragment.MapFragment;
import com.rattapon.navie.Fragment.SearchFragment;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainActivity extends AppCompatActivity {

    private android.support.v7.widget.Toolbar toolbar;
    private String eName;

    private static final int REQUEST_FINE_LOCATION = 124;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        eName = getIntent().getStringExtra("eName");
        initInstance();
        if (!mayRequestLocation()) ;
        setMapFragment();
    }

    @Override
    public void onBackPressed() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        toolbar.setTitle(eName);
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.layout_fragment_container);

        if (f instanceof MapFragment) ;
        else
            super.onBackPressed();
    }

    public void initInstance() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

               //noinspection SimplifiableIfStatement
        if (id == R.id.action_logout) {
            AlertDialog.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder = new AlertDialog.Builder(this, R.style.MyAlertDialogStyle);
            } else {
                builder = new AlertDialog.Builder(this);
            }
            builder.setTitle("Sign Out")
                    .setMessage("Are you sure you want to sign out?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            FirebaseAuth.getInstance().signOut();
                            startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return true;
        }
        if (id == R.id.action_to_search) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            setSearchFragment();
            return true;
        }
        if (id == R.id.action_to_events) {
            startActivity(new Intent(MainActivity.this, EventsActivity.class));
            return true;
        }
        if (id == android.R.id.home) {
            onBackPressed();
            hideSoftKeyboard(findViewById(R.id.relative_main));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setMapFragment() {
        Fragment mapFragment = MapFragment.newInstance();
        Bundle bundle = new Bundle();
        bundle.putString("eName", eName);
        mapFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.layout_fragment_container, mapFragment)
                .commit();
    }

    public void setSearchFragment() {
        SearchFragment searchFragment = new SearchFragment();
        Bundle bundle = new Bundle();
        bundle.putString("eName", eName);
        searchFragment.setArguments(bundle);
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.layout_fragment_container);
        if (fragment instanceof SearchFragment == false) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.layout_fragment_container, searchFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    public void hideSoftKeyboard(final View view) {
        final InputMethodManager imm = (InputMethodManager) getSystemService(this.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // The requested permission is granted.

                } else {
                    // The user disallowed the requested permission.
                    mayRequestLocation();
                }
                return;
            }
        }
    }

    private boolean mayRequestLocation() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        requestPermissions(new String[]{ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
        return false;
    }
}
