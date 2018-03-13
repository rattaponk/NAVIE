package com.rattapon.navie;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeader;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.rattapon.navie.Fragment.MapFragment;
import com.rattapon.navie.Fragment.SearchFragment;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainActivity extends AppCompatActivity {

    private Drawer.Result navigationDrawerLeft;
    private AccountHeader.Result headerNavigationLeft;
    private Drawer result = null;

    private android.support.v7.widget.Toolbar toolbar;
    private String eID;

    private static final int REQUEST_FINE_LOCATION = 124;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        eID = getIntent().getStringExtra("eID");
        initInstance();
        initNavLeft(savedInstanceState);
        if (!mayRequestLocation()) ;
        setMapFragment();
    }

    @Override
    public void onBackPressed() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        toolbar.setTitle(eID);
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.layout_fragment_container);

        if (f instanceof MapFragment) ;
        else
            super.onBackPressed();
    }

    public void initInstance() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }


    public void initNavLeft(Bundle savedInstanceState) {

        headerNavigationLeft = new AccountHeader().withActivity(this).withCompactStyle(false).withSelectionListEnabledForSingleProfile(false)
                .withSavedInstance(savedInstanceState).withHeaderBackground(R.color.colorAccent)
                .addProfiles(
                        new ProfileDrawerItem().withName("Rattapon Kaewpinaji").withEmail("rattapon.k@gmail.com")
                                .withIcon(getResources().getDrawable(R.drawable.navie_logo))
                ).build();
        navigationDrawerLeft = new Drawer().withActivity(this).withToolbar(toolbar)
                .withDisplayBelowToolbar(false).withActionBarDrawerToggleAnimated(true)
                .withDrawerGravity(Gravity.LEFT).withSavedInstance(savedInstanceState)
                .withAccountHeader(headerNavigationLeft).withSelectedItem(-1)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                        if (drawerItem.getIdentifier() == 100) {
                            Toast.makeText(MainActivity.this, "Profile", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(MainActivity.this, EventsActivity.class));
                        } else if (drawerItem.getIdentifier() == 200) {
                            Toast.makeText(MainActivity.this, "Events", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(MainActivity.this, EventsActivity.class));
                        } else if (drawerItem.getIdentifier() == 300) {
                            Toast.makeText(MainActivity.this, "About us", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(MainActivity.this, EventsActivity.class));
                        } else if (drawerItem.getIdentifier() == 400) {
                            Toast.makeText(MainActivity.this, "Logout", Toast.LENGTH_SHORT).show();
                            signOut();
                        }
                    }
                })
                .build();


        navigationDrawerLeft.addItem(new PrimaryDrawerItem().withName("Profile").withIcon(getResources().getDrawable(R.drawable.ic_account_circle_white_24dp)).withIdentifier(100));
        navigationDrawerLeft.addItem(new PrimaryDrawerItem().withName("Events").withIcon(getResources().getDrawable(R.drawable.ic_event_note_white_24dp)).withIdentifier(200));
        navigationDrawerLeft.addItem(new PrimaryDrawerItem().withName("About us").withIcon(getResources().getDrawable(R.drawable.ic_group_white_24dp)).withIdentifier(300));
        navigationDrawerLeft.addItem(new PrimaryDrawerItem().withName("Logout").withIcon(getResources().getDrawable(R.drawable.ic_exit_to_app_white_24dp)).withIdentifier(400));
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
        if (id == R.id.action_to_search) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            setSearchFragment();
            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
            hideSoftKeyboard(findViewById(R.id.relative_main));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setMapFragment() {
        Fragment mapFragment = MapFragment.newInstance();
        Bundle bundle = new Bundle();
        bundle.putString("eID", eID);
        mapFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.layout_fragment_container, mapFragment)
                .commit();
    }

    public void setSearchFragment() {
        SearchFragment searchFragment = new SearchFragment();
        Bundle bundle = new Bundle();
        bundle.putString("eID", eID);
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

    private void signOut() {
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
    }
}
