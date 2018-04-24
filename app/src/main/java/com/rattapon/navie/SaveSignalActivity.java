package com.rattapon.navie;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeader;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.rattapon.navie.JavaClass.WifiList;
import com.rattapon.navie.JavaClass.WifiPoint;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class SaveSignalActivity extends AppCompatActivity implements View.OnClickListener, BeaconConsumer, RangeNotifier, AdapterView.OnItemSelectedListener {

    private android.support.v7.widget.Toolbar toolbar;
    private FloatingActionButton fabCancel;
    private TextView tvInfo;
    private TextView tvCD;
    private Spinner dropdown;
    private EditText etM;
    private Button btSave;

    private Drawer.Result navigationDrawerLeft;
    private AccountHeader.Result headerNavigationLeft;

    private double x, y;
    private ArrayList<String> M = new ArrayList<String>();
    private final List<String> N = new ArrayList<String>();
    private HashMap<String, Double> apX = new HashMap<String, Double>();
    private HashMap<String, Double> apY = new HashMap<String, Double>();
    private HashMap<String, Double> apRssi = new HashMap<String, Double>();
    private HashMap<String, String> apName = new HashMap<String, String>();
    private HashMap<String, String> apSSID = new HashMap<String, String>();
    private Context mContext;
    private Handler mHandler;
    private Worker mWorker;
    private WifiList wifiList;
    private String APs = "";
    private ArrayList<WifiPoint> APFiltered = new ArrayList<WifiPoint>();
    private WifiManager manager;
    private WifiReceiver receiver;
    private List<ScanResult> result;
    private BeaconManager mBeaconManager;

    private String eID;
    private String select_name;
    private String ssid;
    ArrayList<Integer> rssi = new ArrayList<Integer>();

    class Worker extends Thread {
        Context ctx;
        boolean shouldContinue;

        public Worker(Context ctx) {
            mHandler = new Handler();
            this.ctx = ctx;
            this.shouldContinue = true;
        }

        @SuppressWarnings("static-access")
        public void run() {

            while (shouldContinue) {
                mHandler.post(new Runnable() {
                    public void run() {
//                        Toast.makeText(ctx, "Worker run.", Toast.LENGTH_SHORT).show();
                        scanNetworks();
                        calculatePosition();

                        String Info = "Name\t\t" + "MAC-Address\t\t\t" + "RSSI\t\t\t" + "AVG\t\t\t" + "PL" + "\n" + APs;
                        tvInfo.setText(Info);

//                        setContentView(d);

                        Log.v("position", x + " : " + y);
                        Log.d("APs", APs);
                    }
                });
                try {
                    this.sleep(200);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class WifiReceiver extends BroadcastReceiver {

        public List<ScanResult> getResults() {
            return result;
        }

        public WifiManager getManager() {
            return manager;
        }

        @Override
        public void onReceive(Context c, Intent intent) {
            result = manager.getScanResults();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_signal);

        eID = getIntent().getStringExtra("eID");
        manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        receiver = new WifiReceiver();
        registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        mContext = this;
        wifiList = new WifiList();

        initAPData();
        initBeaconData();
        initNavLeft(savedInstanceState);
        initInstance();

        mWorker = new Worker(mContext);
        mWorker.start();
    }

    public void initInstance() {
        fabCancel = findViewById(R.id.fab_cancel);
        toolbar = findViewById(R.id.toolbar);
        tvInfo = findViewById(R.id.tv_info);
        tvCD = findViewById(R.id.tv_cd);
        dropdown = findViewById(R.id.sp_mac);
        etM = findViewById(R.id.et_m);
        btSave = findViewById(R.id.bt_save);
        setSupportActionBar(toolbar);


//        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, N);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        dropdown.setAdapter(adapter);

        fabCancel.setOnClickListener(this);
        btSave.setOnClickListener(this);
        dropdown.setOnItemSelectedListener(this);
    }

    public void initAPData() {
        DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
        mRootRef.child("events").child(eID).child("Wifi").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot db : dataSnapshot.getChildren()) {
                    String _bssid = db.getKey().toString();
                    String _name = db.child("name").getValue().toString();
                    Double _x = Double.parseDouble(db.child("x").getValue().toString());
                    Double _y = Double.parseDouble(db.child("y").getValue().toString());
                    Double _rssi = Double.parseDouble(db.child("rssi").getValue().toString());

                    M.add(_bssid);
                    N.add(_name);
                    apName.put(_bssid, _name);
                    apX.put(_bssid, _x);
                    apY.put(_bssid, _y);
                    apRssi.put(_bssid, _rssi);
                    apSSID.put(_name, _bssid);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }

    public void initBeaconData() {
        DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
        mRootRef.child("events").child(eID).child("Beacon").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot db : dataSnapshot.getChildren()) {
                    String _bssid = db.getKey().toString().toUpperCase(); //All Upper
                    String _name = db.child("name").getValue().toString();
                    Double _x = Double.parseDouble(db.child("x").getValue().toString());
                    Double _y = Double.parseDouble(db.child("y").getValue().toString());
                    Double _rssi = Double.parseDouble(db.child("rssi").getValue().toString());

                    M.add(_bssid);
                    N.add(_name);
                    apName.put(_bssid, _name);
                    apX.put(_bssid, _x);
                    apY.put(_bssid, _y);
                    apRssi.put(_bssid, _rssi);
                    apSSID.put(_name, _bssid);
                }
                ArrayAdapter<String> areasAdapter = new ArrayAdapter<String>(SaveSignalActivity.this, android.R.layout.simple_spinner_item, N);
                areasAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                dropdown.setAdapter(areasAdapter);
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
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
                            startActivity(new Intent(SaveSignalActivity.this, EventsActivity.class));
                        } else if (drawerItem.getIdentifier() == 200) {
                            startActivity(new Intent(SaveSignalActivity.this, EventsActivity.class));
                        } else if (drawerItem.getIdentifier() == 300) {
                            startActivity(new Intent(SaveSignalActivity.this, EventsActivity.class));
                        } else if (drawerItem.getIdentifier() == 400) {
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
    public void onClick(View view) {
        if (view == fabCancel) {
            Intent intent = new Intent(SaveSignalActivity.this, MainActivity.class);
            intent.putExtra("eID", eID);
            startActivity(intent);
        } else if (view == btSave) {
            hideSoftKeyboard(findViewById(R.id.relative_main));
            String m = etM.getText().toString();
            if (!(TextUtils.isEmpty(m))) {
                saveSignalData();
            } else {
                Toast.makeText(mContext, "Please, Fill information", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        select_name = adapterView.getItemAtPosition(i).toString();
        Toast.makeText(mContext, "Select: " + select_name, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    public void calculatePosition() {

        for (int i = 0; i < result.size(); i++) {
            int index = wifiList.isAvailable(result.get(i).BSSID);
            if (index != -1) {
                wifiList.updateAt(index, result.get(i).level);
            } else {
                wifiList.insertNew(result.get(i).BSSID, result.get(i).SSID, result.get(i).level);
            }
        }

        // wifiList of Wifi Accesspoints
        ArrayList<WifiPoint> AP = wifiList.List;
        Collections.sort(AP, new Comparator<WifiPoint>() {
            public int compare(WifiPoint arg0, WifiPoint arg1) {
                // TODO Auto-generated method stub
                return arg1.average - arg0.average;
            }
        });

        APFiltered.clear();
        APs = "";
        int count = 0;
        for (int i = 0; i < AP.size(); i++) {
            if (M.contains(AP.get(i).BSSID)) {
                count++;
                APFiltered.add(AP.get(i));
            }
        }

        int lol = APFiltered.size();

        HashMap<String, Double> RangeFromAps = new HashMap<String, Double>();
        for (int i = 0; i < APFiltered.size(); i++) {
            // System.out.println("i="+i);
            if (M.contains(APFiltered.get(i).BSSID)) {

                double pld0 = apRssi.get(APFiltered.get(i).BSSID);
                double pld = APFiltered.get(i).average;
                double Ldbm = apRssi.get(APFiltered.get(i).BSSID) - APFiltered.get(i).average;
                double Range = Math.pow(10, (Ldbm + 7.36) / 26);
                RangeFromAps.put(APFiltered.get(i).BSSID, Range);

                String range = new DecimalFormat("##.####").format(Range);
                APs += apName.get(APFiltered.get(i).BSSID) + "\t\t" + APFiltered.get(i).BSSID + "\t\t" + APFiltered.get(i).rssi + "\t\t" + APFiltered.get(i).average + "\t\t" + range + "\n";
            }
        }
    }

    public void saveSignalData() {
        btSave.setVisibility(View.INVISIBLE);
        String metre = etM.getText().toString();

        WifiPoint selectAP = new WifiPoint();
        ssid = apSSID.get(select_name);
        rssi.clear();

        ArrayList<WifiPoint> AP = wifiList.List;
        for (int i = 0; i < AP.size(); i++) {
            if (ssid.equals(AP.get(i).BSSID)) {
                selectAP = AP.get(i);
                break;
            }
        }

        while (true) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    //TODO your background code
                    scanNetworks();
                    for (int i = 0; i < result.size(); i++) {
                        int index = wifiList.isAvailable(result.get(i).BSSID);
                        if (index != -1) {
                            wifiList.updateAt(index, result.get(i).level);
                        } else {
                            wifiList.insertNew(result.get(i).BSSID, result.get(i).SSID, result.get(i).level);
                        }
                    }

                    ArrayList<WifiPoint> AP = wifiList.List;
                    for (int i = 0; i < AP.size(); i++) {
                        if (ssid.equals(AP.get(i).BSSID) && rssi.size() < 10) {
                            rssi.add(AP.get(i).rssi);
                            break;
                        }
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            });
            int tmp = rssi.size();
            if (rssi.size() >= 10) {
                break;
            }
//            new CountDownTimer(5000, 500) {
//                public void onTick(long millisUntilFinished) {
//                    tvCD.setText("Seconds remaining: " + millisUntilFinished / 500);
//                }
//
//                public void onFinish() {
//                    tvCD.setText("Done!");
//                }
//            }.start();
        }
        int min = selectAP.min;
        int max = selectAP.max;


        double sum = 0.0, sd = 0.0;
        for (double num : rssi) {
            sum += num;
        }
        double mean = sum / 10;
        for (double num : rssi) {
            sd += Math.pow(num - mean, 2);
        }
        sd = Math.sqrt(sd / rssi.size());

        double avg = mean;
        double Ldbm = apRssi.get(selectAP.BSSID) - mean;
        double pathloss = Math.pow(10, (Ldbm + 7.36) / 26);

        String dir = Environment.getExternalStorageDirectory()
                .toString();
        File folder = new File(dir + "/navie");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        String file = dir + "/navie/" + select_name + "_" + metre + "m" + ".txt";
        File save = new File(file);
        Log.v("directory", file);
        try {
            save.createNewFile();
            FileWriter writer1 = new FileWriter(save, true);

            writer1.write("" + select_name + ", " + selectAP.BSSID + ", " + avg + ", " + sd + ", " + pathloss + ", ");
            for (int i = 0; i < rssi.size(); i++) {
                writer1.write(rssi.get(i) + ", ");
            }
            writer1.close();
            Toast.makeText(mContext, "Save file complete: " + select_name + "_" + metre + "m" + ".txt", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(mContext, "Failed: " + e.toString(), Toast.LENGTH_SHORT).show();
        }
        btSave.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mBeaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());
        // Detect the main Eddystone-UID frame:
        mBeaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        // Detect the telemetry Eddystone-TLM frame:
        mBeaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT));
        // Detect the URL frame:
        mBeaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT));
        mBeaconManager.bind(this);
        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(receiver);
        mBeaconManager.unbind(this);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mWorker.shouldContinue = false;
        try {
            mWorker.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(SaveSignalActivity.this, MainActivity.class);
        intent.putExtra("eID", eID);
        startActivity(intent);
    }

    public void hideSoftKeyboard(final View view) {
        final InputMethodManager imm = (InputMethodManager) getSystemService(this.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void scanNetworks() {
        boolean scan = manager.startScan();

        if (scan) {
            result = manager.getScanResults();
        }
    }

    public void onBeaconServiceConnect() {
        Region region = new Region("all-beacons-region", null, null, null);
        try {
            mBeaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mBeaconManager.addRangeNotifier(this);
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        for (Beacon beacon : beacons) {
            if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x00) {
                // This is a Eddystone-UID frame
                Identifier namespaceId = beacon.getId1();
                Identifier instanceId = beacon.getId2();
                Log.d("Beacon", "I see a beacon transmitting namespace id: " + namespaceId +
                        " and instance id: " + instanceId +
                        " RSSI: " + beacon.getRssi() +
                        " approximately " + beacon.getDistance() + " meters away.");
            }
            if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x10) {
                // This is a Eddystone-URL frame
                String mac = beacon.getBluetoothAddress();
                String url = UrlBeaconUrlCompressor.uncompress(beacon.getId1().toByteArray());
                Log.d("Beacon", "Mac Address: " + mac + " RSSI: " + beacon.getRssi() +
                        " url: " + url + " approximately " + beacon.getDistance() + " meters away.");
                int index = wifiList.isAvailable(mac);
                if (index != -1) {
                    wifiList.updateAt(index, beacon.getRssi());
                } else {
                    wifiList.insertNew(mac, beacon.getBluetoothName(), beacon.getRssi());
                }
            }
            // Do we have telemetry data?
            if (beacon.getExtraDataFields().size() > 0) {
                long telemetryVersion = beacon.getExtraDataFields().get(0);
                long batteryMilliVolts = beacon.getExtraDataFields().get(1);
                long pduCount = beacon.getExtraDataFields().get(3);
                long uptime = beacon.getExtraDataFields().get(4);

                Log.d("Beacon", "The above beacon is sending telemetry version " + telemetryVersion +
                        ", has been up for : " + uptime + " seconds" +
                        ", has a battery level of " + batteryMilliVolts + " mV" +
                        ", and has transmitted " + pduCount + " advertisements.");

            }
        }
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
                        startActivity(new Intent(SaveSignalActivity.this, LoginActivity.class));
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
