package com.rattapon.navie.Fragment;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;
import com.rattapon.navie.JavaClass.WifiList;
import com.rattapon.navie.JavaClass.WifiPoint;
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
    private ArrayList<String> M = new ArrayList<String>();
    private HashMap<String, Double> apX = new HashMap<String, Double>();
    private HashMap<String, Double> apY = new HashMap<String, Double>();
    private HashMap<String, Double> apRssi = new HashMap<String, Double>();
    private HashMap<String, String> apName = new HashMap<String, String>();
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
                        Draw2d d = new Draw2d(getActivity());

                        TextView tvInfo = new TextView(getActivity());
                        String Info = "position: x=" + String.valueOf((int) x) + " , y=" + String.valueOf((int) y) + "\n" + APs;
                        tvInfo.setText(Info);
                        tvInfo.setTextColor(Color.BLACK);
                        tvInfo.setBackgroundColor(Color.WHITE);

                        linear.setOrientation(LinearLayout.VERTICAL);
//                        linear.addView(tvInfo);
                        linear.addView(d);

                        Log.v("position", x + " : " + y);
                        Log.d("APs", APs);
                    }
                });
                try {
                    this.sleep(1000);
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
            if(eID.equals("-L7V3BWZDZOAv2bqsQA1")){
                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.nsc_th2018);
                xscale = 15;
                yscale = 25;
            }
            else if(eID.equals("-L5Xqa4d-PriW5Xcsq37")){
                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.nsc_n2018);
                xscale = 25;
                yscale = 15;
            }else if(eID.equals("-L5mQoCyRp7LP-RSYaKn")) {
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

//            //draw Line
//            for (int i = 0; i <= xscale; i++) {
//                if (i % 5 == 0) {
//                    paint.setColor(Color.GREEN);
//                } else {
//                    paint.setColor(Color.GRAY);
//                }
//                c.drawLine(xmin + (i * img.getWidth() / xscale), ymax - (0 * img.getHeight() / yscale), xmin + (i * img.getWidth() / xscale), ymax - (yscale * img.getHeight() / yscale), paint);
//            }
//            for (int i = 0; i <= yscale; i++) {
//                if (i % 5 == 0) {
//                    paint.setColor(Color.GREEN);
//                } else {
//                    paint.setColor(Color.GRAY);
//                }
//                c.drawLine(xmin + (0 * img.getWidth() / xscale), ymax - (i * img.getHeight() / yscale), xmin + (xscale * img.getWidth() / xscale), ymax - (i * img.getHeight() / yscale), paint);
//            }

//            //draw dot
//            paint.setColor(Color.GRAY);
//            for (int i = 0; i <= xscale; i++) {
//                for (int j = 0; j <= yscale; j++) {
//                    c.drawPoint(xmin + (i * img.getWidth() / xscale), ymax - (j * img.getHeight() / yscale), paint);
//                }
//            }

            //draw APs
            paint.setColor(Color.YELLOW);
            for (int i = 0; i < M.size(); i++) {
                double xx = apX.get(M.get(i));
                double yy = apY.get(M.get(i));
                c.drawCircle(xmin + ((int) xx * img.getWidth() / xscale), ymax - ((int) yy * img.getHeight() / yscale), 10, paint);
            }

            paint.setColor(Color.GREEN);
            for (int i = 0; i < APFiltered.size(); i++) {
                double xx = apX.get(APFiltered.get(i).BSSID);
                double yy = apY.get(APFiltered.get(i).BSSID);
                if (M.contains(APFiltered.get(i).BSSID)) {
                    c.drawCircle(xmin + ((int) xx * img.getWidth() / xscale), ymax - ((int) yy * img.getHeight() / yscale), 10, paint);
                }
            }

            //draw user
            paint.setColor(Color.BLUE);
            c.drawCircle(xmin + ((int) x * img.getWidth() / xscale), ymax - ((int) y * img.getHeight() / yscale), 15, paint);

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
        mWorker = new Worker(mContext);
        mWorker.start();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        myFragmentView = inflater.inflate(R.layout.fragment_map, container, false);
//        ivMap = myFragmentView.findViewById(R.id.iv_fm_map);
        linear = myFragmentView.findViewById(R.id.linear);
        fabLocation = myFragmentView.findViewById(R.id.fab_location);
        return myFragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mWorker.shouldContinue = true;
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
        mWorker.shouldContinue = false;
        mBeaconManager.unbind(this);

    }

    @Override
    public void onDestroy() {
//        mWorker.shouldContinue = false;
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
                    if (_eid .equals("title")){
                        String _ename = db.getValue().toString();
                        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(_ename);
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
                for (DataSnapshot db : dataSnapshot.getChildren()) {
                    String _bssid = db.getKey().toString();
                    String _name = db.child("name").getValue().toString();
                    Double _x = Double.parseDouble(db.child("x").getValue().toString());
                    Double _y = Double.parseDouble(db.child("y").getValue().toString());
                    Double _rssi = Double.parseDouble(db.child("rssi").getValue().toString());

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

    public void initBeaconData(){
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
            if (M.contains(AP.get(i).BSSID) && count < 3) {
                count++;
                APFiltered.add(AP.get(i));
            }
        }

        int lol = APFiltered.size();

        double[][] positions = new double[3][2];
        double[] distances = new double[3];

        HashMap<String, Double> RangeFromAps = new HashMap<String, Double>();
        int n = 0;
        for (int i = 0; i < APFiltered.size(); i++) {
            // System.out.println("i="+i);
            if (M.contains(APFiltered.get(i).BSSID)) {

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
        }

        try {
            NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
            LeastSquaresOptimizer.Optimum optimum = solver.solve();

            // the answer
            double[] calculatedPosition = optimum.getPoint().toArray();
            x = calculatedPosition[0];
            y = calculatedPosition[1];
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.out.println("X:" + x);
        System.out.println("Y:" + y);

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
        for (Beacon beacon: beacons) {
            if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x00) {
                // This is a Eddystone-UID frame
                Identifier namespaceId = beacon.getId1();
                Identifier instanceId = beacon.getId2();
                Log.d("Beacon", "I see a beacon transmitting namespace id: "+namespaceId+
                        " and instance id: "+instanceId+
                        " RSSI: " +beacon.getRssi()+
                        " approximately "+beacon.getDistance()+" meters away.");
            }
            if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x10) {
                // This is a Eddystone-URL frame
                String mac = beacon.getBluetoothAddress();
                String url = UrlBeaconUrlCompressor.uncompress(beacon.getId1().toByteArray());
                Log.d("Beacon", "Mac Address: " +mac+ " RSSI: " +beacon.getRssi()+
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

                Log.d("Beacon", "The above beacon is sending telemetry version "+telemetryVersion+
                        ", has been up for : "+uptime+" seconds"+
                        ", has a battery level of "+batteryMilliVolts+" mV"+
                        ", and has transmitted "+pduCount+" advertisements.");

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
