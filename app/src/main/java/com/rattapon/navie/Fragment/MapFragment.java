package com.rattapon.navie.Fragment;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
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

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

/**
 * A simple {@link Fragment} subclass.
 */
public class MapFragment extends Fragment {

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
    private WifiList List;
    private String APs = "";
    private ArrayList<WifiPoint> APFiltered = new ArrayList<WifiPoint>();

    private WifiManager manager;
    private WifiReceiver receiver;
    private java.util.List<ScanResult> result;

    private static final int REQUEST_FINE_LOCATION = 124;
    private String eName;

    public MapFragment() {
        // Required empty public constructor
    }

    public static MapFragment newInstance() {
        MapFragment fragment = new MapFragment();
        return fragment;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            eName = bundle.getString("eName");
        }
        mContext = getActivity();
        List = new WifiList();
        manager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        receiver = new WifiReceiver();
        getActivity().registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        initAPData();
        mWorker = new Worker(mContext);
        mWorker.start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(eName);
        myFragmentView = inflater.inflate(R.layout.fragment_map, container, false);
//        ivMap = myFragmentView.findViewById(R.id.iv_fm_map);
        linear = myFragmentView.findViewById(R.id.linear);
        fabLocation = myFragmentView.findViewById(R.id.fab_location);
        return myFragmentView;
    }

    @Override
    public void onResume() {
        getActivity().registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mWorker.shouldContinue = true;
        super.onResume();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(receiver);
        mWorker.shouldContinue = false;
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

    public void initAPData() {
        DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
        mRootRef.child("events").child(eName).child("Wifi").addValueEventListener(new ValueEventListener() {
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

    public void calculatePosition() {
        int l = result.size();
        for (int i = 0; i < result.size(); i++) {
            int index = List.isAvailable(result.get(i).BSSID);
            if (index != -1) {
                List.updateAt(index, result.get(i).level);
            } else {
                List.insertNew(result.get(i).BSSID, result.get(i).SSID, result.get(i).level);
            }
        }

        // List of Wifi Accesspoints
        ArrayList<WifiPoint> AP = List.List;
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


        double[][] positions = new double[3][2];
        double[] distances = new double[3];

        HashMap<String, Double> RangeFromAps = new HashMap<String, Double>();
        int n = 0;
        for (int i = 0; i < APFiltered.size(); i++) {
            // System.out.println("i="+i);
            if (M.contains(APFiltered.get(i).BSSID)) {

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
            for (int i = 0; i <= 25; i++) {
                if (i % 5 == 0) {
                    paint.setColor(Color.GREEN);
                } else {
                    paint.setColor(Color.GRAY);
                }
                c.drawLine(xmin + (i * img.getWidth() / 25), ymax - (0 * img.getHeight() / 20), xmin + (i * img.getWidth() / 25), ymax - (20 * img.getHeight() / 20), paint);
            }
            for (int i = 0; i <= 20; i++) {
                if (i % 5 == 0) {
                    paint.setColor(Color.GREEN);
                } else {
                    paint.setColor(Color.GRAY);
                }
                c.drawLine(xmin + (0 * img.getWidth() / 25), ymax - (i * img.getHeight() / 20), xmin + (25 * img.getWidth() / 25), ymax - (i * img.getHeight() / 20), paint);
            }

            //draw dot
            paint.setColor(Color.GRAY);
            for (int i = 0; i <= 25; i++) {
                for (int j = 0; j <= 20; j++) {
                    c.drawPoint(xmin + (i * img.getWidth() / 25), ymax - (j * img.getHeight() / 20), paint);
                }
            }

            //draw APs
            paint.setColor(Color.YELLOW);
            for (int i = 0; i < M.size(); i++) {
                double xx = apX.get(M.get(i));
                double yy = apY.get(M.get(i));
                c.drawCircle(xmin + ((int) xx * img.getWidth() / 25), ymax - ((int) yy * img.getHeight() / 20), 10, paint);
            }
            paint.setColor(Color.GREEN);
            for (int i = 0; i < APFiltered.size(); i++) {
                double xx = apX.get(APFiltered.get(i).BSSID);
                double yy = apY.get(APFiltered.get(i).BSSID);
                if (M.contains(APFiltered.get(i).BSSID)) {
                    c.drawCircle(xmin + ((int) xx * img.getWidth() / 25), ymax - ((int) yy * img.getHeight() / 20), 10, paint);
                }
            }

            //draw user
            paint.setColor(Color.BLUE);
            c.drawCircle(xmin + ((int) x * img.getWidth() / 25), ymax - ((int) y * img.getHeight() / 20), 15, paint);

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

    public void scanNetworks() {
        boolean scan = manager.startScan();

        if (scan) {
            result = manager.getScanResults();
        }
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
