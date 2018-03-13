package com.rattapon.navie;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeader;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.rattapon.navie.Fragment.EventsFragment;
import com.rattapon.navie.Fragment.MapFragment;

public class EventsActivity extends AppCompatActivity {

    private android.support.v7.widget.Toolbar toolbar;
    private Drawer.Result navigationDrawerLeft;
    private AccountHeader.Result headerNavigationLeft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        initInstance();
        initNavLeft(savedInstanceState);
        setMapFragment();
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
                            Toast.makeText(EventsActivity.this, "Profile", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(EventsActivity.this, EventsActivity.class));
                        } else if (drawerItem.getIdentifier() == 200) {
                            Toast.makeText(EventsActivity.this, "Events", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(EventsActivity.this, EventsActivity.class));
                        } else if (drawerItem.getIdentifier() == 300) {
                            Toast.makeText(EventsActivity.this, "About us", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(EventsActivity.this, EventsActivity.class));
                        } else if (drawerItem.getIdentifier() == 400) {
                            Toast.makeText(EventsActivity.this, "Logout", Toast.LENGTH_SHORT).show();
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

    public void setMapFragment() {
        Fragment eventsFragment = EventsFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.layout_fragment_container, eventsFragment)
                .commit();
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
                        startActivity(new Intent(EventsActivity.this, LoginActivity.class));
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
