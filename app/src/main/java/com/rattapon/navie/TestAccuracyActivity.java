package com.rattapon.navie;

import android.content.BroadcastReceiver;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class TestAccuracyActivity extends AppCompatActivity implements View.OnClickListener, BeaconConsumer, RangeNotifier {

    private android.support.v7.widget.Toolbar toolbar;
    private LinearLayout linear;
    private ImageView ivMap;
    private FloatingActionButton fabCancel;
    private Button btFind;
    private Button btClear;
    private Button btSaveAll;
    private EditText etRealX;
    private EditText etRealY;
    private TextView tvRound;

    private Drawer.Result navigationDrawerLeft;
    private AccountHeader.Result headerNavigationLeft;

    private String tName;
    private double rX, rY;
    private double x, y, fs_x, fs_y;
    private ArrayList<String> M = new ArrayList<String>();
    private HashMap<String, Double> apX = new HashMap<String, Double>();
    private HashMap<String, Double> apY = new HashMap<String, Double>();
    private HashMap<String, Integer> apRssi = new HashMap<String, Integer>();
    private HashMap<String, String> apName = new HashMap<String, String>();
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
                        linear.removeAllViews();
                        scanNetworks();
                        calculatePosition();


                        TextView tvInfo = new TextView(TestAccuracyActivity.this);
                        String Info = "Blue: x=" + String.valueOf((int) x) + " , y=" + String.valueOf((int) y) + "\n" + "Red:  x=" + String.valueOf((int) fs_x) + " , y=" + String.valueOf((int) fs_y) + "\n" + APs;
                        tvInfo.setText(Info);
                        tvInfo.setTextColor(Color.BLACK);
                        tvInfo.setBackgroundColor(Color.WHITE);

                        linear.setOrientation(LinearLayout.VERTICAL);
                        linear.addView(tvInfo);
                        TestAccuracyActivity.Draw2d d = new TestAccuracyActivity.Draw2d(TestAccuracyActivity.this);
                        linear.addView(d);

//                        setContentView(d);

                        Log.v("position", x + " : " + y);
                        Log.d("APs", APs);
                    }
                });
                try {
                    this.sleep(500);
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

    public class Draw2d extends View {
        public Draw2d(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas c) {

            super.onDraw(c);
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);

            int xmin, xmax, ymin, ymax;

            // make the entire canvas white
            paint.setColor(Color.WHITE);
            c.drawPaint(paint);
            paint.setAntiAlias(true);

            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.cpe_floor4);
            int xscale = 0;
            int yscale = 0;
            if (eID.equals("-L7V3BWZDZOAv2bqsQA1")) {
                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.nsc_th2018);
                xscale = 15;
                yscale = 25;
            } else if (eID.equals("-L5Xqa4d-PriW5Xcsq37")) {
                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.nsc_n2018);
                xscale = 25;
                yscale = 15;
            } else if (eID.equals("-L5mQoCyRp7LP-RSYaKn")) {
                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.cpe_floor4);
                xscale = 25;
                yscale = 20;
            }

            Bitmap img;
            if (getWidth() / getHeight() <= bmp.getWidth() / bmp.getHeight()) {
                img = Bitmap.createScaledBitmap(bmp, getWidth(),
                        bmp.getHeight() * getWidth() / bmp.getWidth(), true);

                c.drawBitmap(img, 0, (getHeight() - img.getHeight()) / 2, paint);
                xmin = 0;
                //ymin = (getHeight() - img.getHeight()) / 2;
                ymax = ((getHeight() - img.getHeight()) / 2) + img.getHeight();
            } else {
                img = Bitmap.createScaledBitmap(bmp, bmp.getWidth() * getHeight() / bmp.getHeight(),
                        getHeight(), true);
                c.drawBitmap(img, (getWidth() - img.getWidth()) / 2, 0, paint);
                xmin = (getWidth() - img.getWidth()) / 2;
                //ymin = 0;
                ymax = img.getHeight();
            }

            //draw Line
            for (int i = 0; i <= xscale; i++) {
                if (i % 5 == 0) {
                    paint.setColor(Color.GREEN);
                } else {
                    paint.setColor(Color.GRAY);
                }
                c.drawLine(xmin + (i * img.getWidth() / xscale), ymax - (0 * img.getHeight() / yscale), xmin + (i * img.getWidth() / xscale), ymax - (yscale * img.getHeight() / yscale), paint);
            }
            for (int i = 0; i <= yscale; i++) {
                if (i % 5 == 0) {
                    paint.setColor(Color.GREEN);
                } else {
                    paint.setColor(Color.GRAY);
                }
                c.drawLine(xmin + (0 * img.getWidth() / xscale), ymax - (i * img.getHeight() / yscale), xmin + (xscale * img.getWidth() / xscale), ymax - (i * img.getHeight() / yscale), paint);
            }

            //draw dot
            paint.setColor(Color.GRAY);
            for (int i = 0; i <= xscale; i++) {
                for (int j = 0; j <= yscale; j++) {
                    c.drawPoint(xmin + (i * img.getWidth() / xscale), ymax - (j * img.getHeight() / yscale), paint);
                }
            }
            //draw APs
            paint.setColor(Color.YELLOW);
            for (int i = 0; i < M.size(); i++) {
                double xx = apX.get(M.get(i));
                double yy = apY.get(M.get(i));
                c.drawCircle(xmin + ((int) xx * img.getWidth() / xscale), ymax - ((int) yy * img.getHeight() / yscale), 8, paint);
            }

            paint.setColor(Color.GREEN);
            for (int i = 0; i < APFiltered.size(); i++) {
                double xx = apX.get(APFiltered.get(i).BSSID);
                double yy = apY.get(APFiltered.get(i).BSSID);
                if (M.contains(APFiltered.get(i).BSSID)) {
                    c.drawCircle(xmin + ((int) xx * img.getWidth() / xscale), ymax - ((int) yy * img.getHeight() / yscale), 8, paint);
                }
            }

            if (APFiltered.size() >= 3) {
                //draw user
                paint.setColor(Color.BLUE);
                c.drawCircle(xmin + ((int) x * img.getWidth() / xscale), ymax - ((int) y * img.getHeight() / yscale), 15, paint);

                //draw free-space
                paint.setColor(Color.RED);
                c.drawCircle(xmin + ((int) fs_x * img.getWidth() / xscale), ymax - ((int) fs_y * img.getHeight() / yscale), 15, paint);
            }

            String sx = etRealX.getText().toString();
            String sy = etRealY.getText().toString();
            if (!(TextUtils.isEmpty(sx) || TextUtils.isEmpty(sy))) {
                paint.setColor(Color.DKGRAY);
                c.drawCircle(xmin + ((int) rX * img.getWidth() / xscale), ymax - ((int) rY * img.getHeight() / yscale), 15, paint);
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_accuracy);

        eID = getIntent().getStringExtra("eID");
        manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        receiver = new WifiReceiver();
        registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        mContext = this;
        wifiList = new WifiList();

        initInstance();
        initAPData();
        initBeaconData();
        initNavLeft(savedInstanceState);

        mWorker = new Worker(mContext);
        mWorker.start();
    }

    public void initInstance() {
        linear = findViewById(R.id.linear);
        fabCancel = findViewById(R.id.fab_cancel);
        btFind = findViewById(R.id.bt_find);
        btClear = findViewById(R.id.bt_clear);
        btSaveAll = findViewById(R.id.bt_saveall);
        toolbar = findViewById(R.id.toolbar);
        etRealX = findViewById(R.id.et_rx);
        etRealY = findViewById(R.id.et_ry);
        tvRound = findViewById(R.id.tv_round);
        setSupportActionBar(toolbar);

        fabCancel.setOnClickListener(this);
        btFind.setOnClickListener(this);
        btClear.setOnClickListener(this);
        btSaveAll.setOnClickListener(this);
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
                    Integer _rssi = Integer.parseInt(db.child("rssi").getValue().toString());

                    M.add(_bssid);
                    apName.put(_bssid, _name);
                    apX.put(_bssid, _x);
                    apY.put(_bssid, _y);
                    apRssi.put(_bssid, _rssi);
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
                    Integer _rssi = Integer.parseInt(db.child("rssi").getValue().toString());

                    M.add(_bssid);
                    apName.put(_bssid, _name);
                    apX.put(_bssid, _x);
                    apY.put(_bssid, _y);
                    apRssi.put(_bssid, _rssi);
                }
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
                            startActivity(new Intent(TestAccuracyActivity.this, EventsActivity.class));
                        } else if (drawerItem.getIdentifier() == 200) {
                            startActivity(new Intent(TestAccuracyActivity.this, EventsActivity.class));
                        } else if (drawerItem.getIdentifier() == 300) {
                            startActivity(new Intent(TestAccuracyActivity.this, EventsActivity.class));
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
            Intent intent = new Intent(TestAccuracyActivity.this, MainActivity.class);
            intent.putExtra("eID", eID);
            startActivity(intent);
        } else if (view == btFind) {
            hideSoftKeyboard(findViewById(R.id.relative_main));
            String sx = etRealX.getText().toString();
            String sy = etRealY.getText().toString();
            if (!(TextUtils.isEmpty(sx) || TextUtils.isEmpty(sy))) {
                rX = Double.parseDouble(etRealX.getText().toString());
                rY = Double.parseDouble(etRealY.getText().toString());
                double error = EuclideanDistance(x, y);
                double fs_error = EuclideanDistance(fs_x, fs_y);
                Toast.makeText(mContext, "1m: " + error + "\n" + "FS: " + fs_error, Toast.LENGTH_LONG).show();
            }
        } else if (view == btClear) {
            wifiList.List.clear();
        } else if (view == btSaveAll) {
            btSaveAll.setVisibility(View.INVISIBLE);
            String sx = etRealX.getText().toString();
            String sy = etRealY.getText().toString();
            if (!(TextUtils.isEmpty(sx) || TextUtils.isEmpty(sy))) {
                rX = Double.parseDouble(etRealX.getText().toString());
                rY = Double.parseDouble(etRealY.getText().toString());

                final WifiList wifi_list = new WifiList();
                final ArrayList<WifiPoint> APSave = new ArrayList<WifiPoint>();
                while (true) {
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            //TODO your background code
                            scanNetworks();
                            APs = "";
                            wifi_list.List.clear();
                            for (int i = 0; i < result.size(); i++) {
                                wifi_list.insertNew(result.get(i).BSSID, result.get(i).SSID, result.get(i).level);

                            }
                            ArrayList<WifiPoint> AP = wifi_list.List;
                            for (int i = 0; i < AP.size(); i++) {
                                if (M.contains(AP.get(i).BSSID)) {
                                    APSave.add(AP.get(i));
                                }
                            }
//                            tvRound.setText(APSave.size());

                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    Log.d("round", "round : " + APSave.size());
                    if (APSave.size() >= 50) {
                        break;
                    }
                }
                String dir = Environment.getExternalStorageDirectory()
                        .toString();
                File folder = new File(dir + "/NavieRSSI");
                if (!folder.exists()) {
                    folder.mkdirs();
                }

                String file = dir + "/NavieRSSI/" + "x" + (int)rX + "_" + "y" + (int)rY + ".txt";
                File save = new File(file);
                Log.v("directory", file);
                try {
                    save.createNewFile();
                    FileWriter writer1 = new FileWriter(save, true);

                    for (int i = 0; i < APSave.size(); i++) {
                        writer1.write(apName.get(APSave.get(i).BSSID) + ", " + APSave.get(i).BSSID + ", " + APSave.get(i).rssi + "\n");
                    }
                    writer1.close();
                    Toast.makeText(mContext, "Save file complete.", Toast.LENGTH_LONG).show();

                } catch (IOException e) {
                    Toast.makeText(mContext, "Failed: " + e.toString(), Toast.LENGTH_LONG).show();
                }
            }
            btSaveAll.setVisibility(View.VISIBLE);
        }
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

        ArrayList<WifiPoint> AP_ref = new ArrayList<>();
        for(int i = 0; i < AP.size(); i++){
            if (M.contains(AP.get(i).BSSID)) {
                AP_ref.add(AP.get(i));
            }
        }

        Collections.sort(AP_ref, new Comparator<WifiPoint>() {
            public int compare(WifiPoint arg0, WifiPoint arg1) {
                return (arg1.rssi - apRssi.get(arg1.BSSID)) - (arg0.rssi - apRssi.get(arg0.BSSID));
            }
        });

        APFiltered.clear();
        APs = "";
        int count = 0;
        for (int i = 0; i < AP_ref.size(); i++) {
            if (count < 3) {
                count++;
                APFiltered.add(AP_ref.get(i));
            }
        }

        double[][] positions = new double[3][2];
        double[] distances = new double[3];

        double[][] fs_positions = new double[3][2];
        double[] fs_distances = new double[3];

        HashMap<String, Double> RangeFromAps = new HashMap<String, Double>();
        int n = 0;
        for (int i = 0; i < APFiltered.size(); i++) {
            // System.out.println("i="+i);
            if (M.contains(APFiltered.get(i).BSSID)) {

                double pld0 = apRssi.get(APFiltered.get(i).BSSID);
                double pld = APFiltered.get(i).average;
//                double Ldbm = apRssi.get(APFiltered.get(i).BSSID) - APFiltered.get(i).average;
                double Ldbm = pld0 - pld;
                double Range = Math.pow(10, (Ldbm + 7.36) / 26) / 2;
                double fs_Range = Math.pow(10, (Ldbm) / 20) / 2;
                RangeFromAps.put(APFiltered.get(i).BSSID, Range);

                distances[n] = Range;
                positions[n][0] = apX.get(APFiltered.get(i).BSSID);
                positions[n][1] = apY.get(APFiltered.get(i).BSSID);

                fs_distances[n] = fs_Range;
                fs_positions[n][0] = apX.get(APFiltered.get(i).BSSID);
                fs_positions[n][1] = apY.get(APFiltered.get(i).BSSID);
                n++;
                String range = new DecimalFormat("##.####").format(Range);
                double diff = apRssi.get(APFiltered.get(i).BSSID) - APFiltered.get(i).rssi;
                APs += apName.get(APFiltered.get(i).BSSID) + "\t\t" + APFiltered.get(i).BSSID + "\t\t" + APFiltered.get(i).average + "\t" + diff +  "\t" + APFiltered.get(i).round + "\t\t" + range + "\n";
            }
        }

        if (APFiltered.size() >= 3) {
            try {
                NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
                LeastSquaresOptimizer.Optimum optimum = solver.solve();

                // the answer
                double[] calculatedPosition = optimum.getPoint().toArray();
                x = calculatedPosition[0];
                y = calculatedPosition[1];

                NonLinearLeastSquaresSolver fs_solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(fs_positions, fs_distances), new LevenbergMarquardtOptimizer());
                LeastSquaresOptimizer.Optimum fs_optimum = fs_solver.solve();

                // the answer
                double[] fs_calculatedPosition = fs_optimum.getPoint().toArray();
                fs_x = fs_calculatedPosition[0];
                fs_y = fs_calculatedPosition[1];
            } catch (Throwable e) {
                e.printStackTrace();
            }

            if (x < 0) x = 0;
            if (y < 0) y = 0;
            if (fs_x < 0) fs_x = 0;
            if (fs_y < 0) fs_y = 0;
            System.out.println("X:" + x);
            System.out.println("Y:" + y);
        }

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
        Intent intent = new Intent(TestAccuracyActivity.this, MainActivity.class);
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
//            if (beacon.getExtraDataFields().size() > 0) {
//                long telemetryVersion = beacon.getExtraDataFields().get(0);
//                long batteryMilliVolts = beacon.getExtraDataFields().get(1);
//                long pduCount = beacon.getExtraDataFields().get(3);
//                long uptime = beacon.getExtraDataFields().get(4);
//
//                Log.d("Beacon", "The above beacon is sending telemetry version " + telemetryVersion +
//                        ", has been up for : " + uptime + " seconds" +
//                        ", has a battery level of " + batteryMilliVolts + " mV" +
//                        ", and has transmitted " + pduCount + " advertisements.");
//
//            }
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
                        startActivity(new Intent(TestAccuracyActivity.this, LoginActivity.class));
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

    double EuclideanDistance(double _x, double _y) {
        double ycoord = Math.abs(rY - _y);
        double xcoord = Math.abs(rX - _x);
        double distance = Math.sqrt(Math.pow(ycoord, 2) + Math.pow(xcoord, 2)) * 2;
        return distance;
    }
}
