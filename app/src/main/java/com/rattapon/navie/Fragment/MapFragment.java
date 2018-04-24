package com.rattapon.navie.Fragment;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;
import com.rattapon.navie.JavaClass.Participants;
import com.rattapon.navie.JavaClass.User;
import com.rattapon.navie.JavaClass.WifiList;
import com.rattapon.navie.JavaClass.WifiPoint;
import com.rattapon.navie.LoginActivity;
import com.rattapon.navie.NavigationActivity;
import com.rattapon.navie.R;

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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class MapFragment extends Fragment implements BeaconConsumer, RangeNotifier {

    private View myFragmentView;
    private LinearLayout linear;
    private FloatingActionButton fabLocation;
//    private ImageView ivMap;

    private double x, y;

    private ArrayList<String> P = new ArrayList<String>();
    private ArrayList<String> apM = new ArrayList<String>();
    private HashMap<String, Double> apX = new HashMap<String, Double>();
    private HashMap<String, Double> apY = new HashMap<String, Double>();
    private HashMap<String, Integer> apRssi = new HashMap<String, Integer>();
    private HashMap<String, String> apName = new HashMap<String, String>();
    private ArrayList<String> bM = new ArrayList<String>();
    private HashMap<String, Double> bX = new HashMap<String, Double>();
    private HashMap<String, Double> bY = new HashMap<String, Double>();
    private HashMap<String, Integer> bRssi = new HashMap<String, Integer>();
    private HashMap<String, String> bName = new HashMap<String, String>();


    private Context mContext;
    private Worker mWorker;
    private Handler mHandler;
    private WifiList wifiList;
    private String APs = "";
    private ArrayList<WifiPoint> APFiltered = new ArrayList<WifiPoint>();

    private WifiManager manager;
    private WifiReceiver receiver;
    private java.util.List<ScanResult> result;
    private BeaconManager mBeaconManager;

    private static final int REQUEST_FINE_LOCATION = 124;
    private String eID;
    private boolean chk = true;

    private int[] x_ref = new int[]{7, 10, 5, 10, 15, 3, 5, 10, 15, 14, 14, 18, 23, 3, 15, 5, 15, 10, 6, 19, 23, 17};
    private int[] y_ref = new int[]{2, 2, 5, 5, 5, 5, 9, 9, 9, 12, 15, 14, 14, 8, 2, 7, 7, 11, 11, 16, 16, 11};

    public MapFragment() {
        // Required empty public constructor
    }

    public static MapFragment newInstance() {
        MapFragment fragment = new MapFragment();
        return fragment;
    }

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

                        TextView tvInfo = new TextView(getActivity());
                        String Info = "position: x=" + String.valueOf((int) x) + " , y=" + String.valueOf((int) y) + " p=" + P.size() + "\n" + APs;
                        tvInfo.setText(Info);
                        tvInfo.setTextColor(Color.BLACK);
                        tvInfo.setBackgroundColor(Color.WHITE);

                        linear.setOrientation(LinearLayout.VERTICAL);
                        linear.addView(tvInfo);
                        Draw2d d = new Draw2d(getActivity());
                        linear.addView(d);

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
                img = Bitmap.createScaledBitmap(bmp, getWidth(), bmp.getHeight() * getWidth() / bmp.getWidth(), true);

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
            for (int i = 0; i < apM.size(); i++) {
                double xx = apX.get(apM.get(i));
                double yy = apY.get(apM.get(i));
                c.drawCircle(xmin + ((int) xx * img.getWidth() / xscale), ymax - ((int) yy * img.getHeight() / yscale), 10, paint);
            }

            paint.setColor(Color.GREEN);
            for (int i = 0; i < APFiltered.size(); i++) {
                double xx = apX.get(APFiltered.get(i).BSSID);
                double yy = apY.get(APFiltered.get(i).BSSID);
                if (apM.contains(APFiltered.get(i).BSSID)) {
                    c.drawCircle(xmin + ((int) xx * img.getWidth() / xscale), ymax - ((int) yy * img.getHeight() / yscale), 10, paint);
                }
            }

            //draw Beacon
            paint.setColor(Color.GRAY);
            paint.setStyle(Paint.Style.FILL);
            RectF rect = new RectF();
            rect.set(100, 100, 100, 100);
            for (int i = 0; i < bM.size(); i++) {
                double xx = bX.get(bM.get(i));
                double yy = bY.get(bM.get(i));
                c.drawCircle(xmin + ((int) xx * img.getWidth() / xscale), ymax - ((int) yy * img.getHeight() / yscale), 8, paint);
            }
            paint.setColor(Color.GREEN);
            for (int i = 0; i < APFiltered.size(); i++) {
                double xx = bX.get(APFiltered.get(i).BSSID);
                double yy = bY.get(APFiltered.get(i).BSSID);
                if (bM.contains(APFiltered.get(i).BSSID)) {
                    c.drawCircle(xmin + ((int) xx * img.getWidth() / xscale), ymax - ((int) yy * img.getHeight() / yscale), 8, paint);
                }
            }


            if (APFiltered.size() >= 3) {
                //draw user
                paint.setColor(Color.BLUE);
                c.drawCircle(xmin + ((int) x * img.getWidth() / xscale), ymax - ((int) y * img.getHeight() / yscale), 15, paint);
            } else {

            }
//            paint.setColor(Color.DKGRAY);
//            for (int i = 0; i < x_ref.length ; i++) {
//                c.drawCircle(xmin + (x_ref[i] * img.getWidth() / xscale), ymax - (y_ref[i] * img.getHeight() / yscale), 10, paint);
//            }
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

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            eID = bundle.getString("eID");
        }
        mContext = getActivity();
        wifiList = new WifiList();
        manager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        receiver = new WifiReceiver();
        getActivity().registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        initEName();
        initAPData();
        initBeaconData();
        initParticipant();
        mWorker = new Worker(mContext);
        mWorker.start();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        myFragmentView = inflater.inflate(R.layout.fragment_map, container, false);
//        ivMap = myFragmentView.findViewById(R.id.iv_fm_map);
        linear = myFragmentView.findViewById(R.id.linear);
        fabLocation = myFragmentView.findViewById(R.id.fab_location);
        fabLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiList.List.clear();
            }
        });
        return myFragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mBeaconManager = BeaconManager.getInstanceForApplication(getActivity().getApplicationContext());
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
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(receiver);
        mBeaconManager.unbind(this);

    }

    @Override
    public void onDestroy() {
//        mWorker.shouldContinue = false;
        pushPositionData(false, 505, 505);
        try {
            mWorker.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    public void initEName() {
        DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
        mRootRef.child("events").child(eID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot db : dataSnapshot.getChildren()) {
                    String _eid = db.getKey().toString();
                    if (_eid.equals("title")) {
                        String _ename = db.getValue().toString();
                        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(_ename);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });

    }

    public void initAPData() {
        DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
        mRootRef.child("events").child(eID).child("Wifi").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                apM.clear();
                apName.clear();
                apX.clear();
                apY.clear();
                apRssi.clear();
                for (DataSnapshot db : dataSnapshot.getChildren()) {
                    String _bssid = db.getKey().toString();
                    String _name = db.child("name").getValue().toString();
                    Double _x = Double.parseDouble(db.child("x").getValue().toString());
                    Double _y = Double.parseDouble(db.child("y").getValue().toString());
                    Integer _rssi = Integer.parseInt(db.child("rssi").getValue().toString());

                    apM.add(_bssid);
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
                bM.clear();
                bName.clear();
                bX.clear();
                bY.clear();
                bRssi.clear();
                for (DataSnapshot db : dataSnapshot.getChildren()) {
                    String _bssid = db.getKey().toString().toUpperCase(); //All Upper
                    String _name = db.child("name").getValue().toString();
                    Double _x = Double.parseDouble(db.child("x").getValue().toString());
                    Double _y = Double.parseDouble(db.child("y").getValue().toString());
                    Integer _rssi = Integer.parseInt(db.child("rssi").getValue().toString());

                    bM.add(_bssid);
                    bName.put(_bssid, _name);
                    bX.put(_bssid, _x);
                    bY.put(_bssid, _y);
                    bRssi.put(_bssid, _rssi);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }

    public void initParticipant() {
        DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
        mRootRef.child("events").child(eID).child("participants").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                P.clear();
                for (DataSnapshot db : dataSnapshot.getChildren()) {
                    String _ukey = db.getKey().toString();

                    P.add(_ukey);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });

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
        for (int i = 0; i < AP.size(); i++) {
            if (apM.contains(AP.get(i).BSSID) || bM.contains(AP.get(i).BSSID)) {
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

        HashMap<String, Double> RangeFromAps = new HashMap<String, Double>();
        int n = 0;
        for (int i = 0; i < APFiltered.size(); i++) {
            // System.out.println("i="+i);
            if (apM.contains(APFiltered.get(i).BSSID)) {
                double pld0 = apRssi.get(APFiltered.get(i).BSSID);
                double pld = APFiltered.get(i).average;
                double Ldbm = apRssi.get(APFiltered.get(i).BSSID) - APFiltered.get(i).average;
                double Range = Math.pow(10, (Ldbm + 7.36) / 26) / 2;
                RangeFromAps.put(APFiltered.get(i).BSSID, Range);

                distances[n] = Range;
                positions[n][0] = apX.get(APFiltered.get(i).BSSID);
                positions[n][1] = apY.get(APFiltered.get(i).BSSID);
                n++;
                String range = new DecimalFormat("##.####").format(Range);
                APs += apName.get(APFiltered.get(i).BSSID) + "\t\t" + APFiltered.get(i).BSSID + "\t\t" + APFiltered.get(i).average + "\t" + APFiltered.get(i).round + "\t\t" + range + "\n";
            }
            else if (bM.contains(APFiltered.get(i).BSSID)) {
                double pld0 = bRssi.get(APFiltered.get(i).BSSID);
                double pld = APFiltered.get(i).average;
                double Ldbm = bRssi.get(APFiltered.get(i).BSSID) - APFiltered.get(i).average;
                double Range = Math.pow(10, (Ldbm + 7.36) / 26) / 2;
                RangeFromAps.put(APFiltered.get(i).BSSID, Range);

                distances[n] = Range;
                positions[n][0] = bX.get(APFiltered.get(i).BSSID);
                positions[n][1] = bY.get(APFiltered.get(i).BSSID);
                n++;
                String range = new DecimalFormat("##.####").format(Range);
                APs += bName.get(APFiltered.get(i).BSSID) + "\t\t" + APFiltered.get(i).BSSID + "\t\t" + APFiltered.get(i).average + "\t" + APFiltered.get(i).round + "\t\t" + range + "\n";
            }
        }
        String key = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (APFiltered.size() >= 3) {
            try {
                NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
                LeastSquaresOptimizer.Optimum optimum = solver.solve();

                // the answer
                double[] calculatedPosition = optimum.getPoint().toArray();
                x = calculatedPosition[0];
                y = calculatedPosition[1];

                if (x < 0) x = 0;
                if (y < 0) y = 0;
            } catch (Throwable e) {
                e.printStackTrace();
            }
            System.out.println("X:" + x);
            System.out.println("Y:" + y);

            if (P.contains(key)) {
                pushPositionData(true, (int) x, (int) y);
            } else if (chk) {
                registerDialog();
                chk = false;
            }
        } else if (P.contains(key)) pushPositionData(false, 404, 404);

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

    @Override
    public Context getApplicationContext() {
        return getActivity().getApplicationContext();
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {
        getActivity().unbindService(serviceConnection);
    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return getActivity().bindService(intent, serviceConnection, i);
    }

    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(double x, double y);

    }

    public void pushPositionData(boolean a, int x, int y) {
//        FirebaseUser currentUser = mAuth.getCurrentUser();
//        String key = currentUser.getUid();
        String key = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference mUsersRef = mRootRef.child("events").child(eID).child("participants");
        Participants participants = new Participants(a, x, y);
        mUsersRef.child(key).setValue(participants);
    }

    public void registerDialog() {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(getActivity(), R.style.MyAlertDialogStyle);
        } else {
            builder = new AlertDialog.Builder(getActivity());
        }
        builder.setTitle("Register")
                .setMessage("Register to this event success.")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        pushPositionData(true, (int) x, (int) y);
                        chk = true;
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        switch (requestCode) {
//            case REQUEST_FINE_LOCATION: {
//                // If request is cancelled, the result arrays are empty.
//                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    // The requested permission is granted.
//                    setMapFragment();
//                } else {
//                    // The user disallowed the requested permission.
//                    mayRequestLocation();
//                }
//                return;
//            }
//        }
//    }

    //TODO fix them
//    private boolean mayRequestLocation() {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
//            return true;
//        }
//        if (ActivityCompat.checkSelfPermission(getContext(),
//                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
//                ActivityCompat.checkSelfPermission(getContext(),
//                        android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            return true;
//        }
//        requestPermissions(new String[]{ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
//        return false;
//    }

}
