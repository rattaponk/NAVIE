package com.rattapon.navie;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
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
import com.rattapon.navie.JavaClass.DijkstraAlgorithm;
import com.rattapon.navie.JavaClass.Edge;
import com.rattapon.navie.JavaClass.Graph;
import com.rattapon.navie.JavaClass.Vertex;
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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class NavigationActivity extends AppCompatActivity implements View.OnClickListener, BeaconConsumer, RangeNotifier {

    private android.support.v7.widget.Toolbar toolbar;
    private LinearLayout linear;
    private ImageView ivMap;
    private Button btFind;
    private FloatingActionButton fabCancel;

    private Drawer.Result navigationDrawerLeft;
    private AccountHeader.Result headerNavigationLeft;

    private String tName;
    private double tX, tY;
    private double x, y;
    private ArrayList<String> M = new ArrayList<String>();
    private HashMap<String, Double> apX = new HashMap<String, Double>();
    private HashMap<String, Double> apY = new HashMap<String, Double>();
    private HashMap<String, Double> apRssi = new HashMap<String, Double>();
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

    private List<Vertex> nodes;
    private List<Edge> edges;
    private Graph graph;
    private LinkedList<Vertex> path;
    private DijkstraAlgorithm dijkstra;

    private static final int REQUEST_FINE_LOCATION = 0;
    private String eID;

    class Worker extends Thread {
        Context ctx;
        boolean shouldContinue;

        public Worker(Context ctx) {
            mHandler = new Handler();
            this.ctx = ctx;
            this.shouldContinue=true;
        }

        @SuppressWarnings("static-access")
        public void run() {

            while (shouldContinue) {
                mHandler.post(new Runnable() {
                    public void run() {
                        linear.removeAllViews();
                        scanNetworks();
                        calculatePosition();
                        Draw2d d = new Draw2d(NavigationActivity.this);

                        TextView tvInfo = new TextView(NavigationActivity.this);
                        String Info = "position: x=" + String.valueOf((int) x) + " , y=" + String.valueOf((int) y) + "\n" + APs;
                        tvInfo.setText(Info);
                        tvInfo.setTextColor(Color.BLACK);
                        tvInfo.setBackgroundColor(Color.WHITE);

                        linear.setOrientation(LinearLayout.VERTICAL);
//                        linear.addView(tvInfo);
                        linear.addView(d);

//                        setContentView(d);

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
//
//            //draw dot
//            paint.setColor(Color.GRAY);
//            for (int i = 0; i <= xscale; i++) {
//                for (int j = 0; j <= yscale; j++) {
//                    c.drawPoint(xmin + (i * img.getWidth() / xscale), ymax - (j * img.getHeight() / yscale), paint);
//                }
//            }
            paint.setColor(Color.GREEN);
            for (int i = 0; i < APFiltered.size(); i++) {
                double xx = apX.get(APFiltered.get(i).BSSID);
                double yy = apY.get(APFiltered.get(i).BSSID);
                if (M.contains(APFiltered.get(i).BSSID)) {
                    c.drawCircle(xmin + ((int) xx * img.getWidth() / xscale), ymax - ((int) yy * img.getHeight() / yscale), 10, paint);
                }
            }

            if(!(path == null || path.isEmpty())) {
               //draw path
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5);
                paint.setColor(Color.RED);
                double old_x = path.getFirst().getX();
                double old_y = path.getFirst().getY();
                for (Vertex vertex : path) {
                    double next_x = vertex.getX();
                    double next_y = vertex.getY();
                    c.drawLine(xmin + ((int) old_x * img.getWidth() / xscale), ymax - ((int) old_y * img.getHeight() / yscale), xmin + ((int) next_x * img.getWidth() / xscale), ymax - ((int) next_y * img.getHeight() / yscale), paint);
                    old_x = next_x;
                    old_y = next_y;
                }
            }
            paint.setStyle(Paint.Style.FILL);

            //draw user
            paint.setColor(Color.BLUE);
            c.drawCircle(xmin + ((int) x * img.getWidth() / xscale), ymax - ((int) y * img.getHeight() / yscale), 15, paint);

            //draw destination
            paint.setColor(Color.RED);
            c.drawCircle(xmin + ((int) tX * img.getWidth() / xscale), ymax - ((int) tY * img.getHeight() / yscale), 15, paint);
            paint.setColor(Color.BLACK);
            c.drawText(tName, xmin + ((int) tX * img.getWidth() / xscale) - 5, ymax - ((int) tY * img.getHeight() / yscale) + 1, paint);

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        eID = getIntent().getStringExtra("eID");
        tName = getIntent().getStringExtra("tName");
        tX = getIntent().getDoubleExtra("X", 0);
        tY = getIntent().getDoubleExtra("Y", 0);
        manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        receiver = new WifiReceiver();
        registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        mContext = this;
        wifiList = new WifiList();

        nodes = new ArrayList<Vertex>();
        edges = new ArrayList<Edge>();
        path = new LinkedList<Vertex>();

        initInstance();
        initAPData();
        initBeaconData();
        initNavLeft(savedInstanceState);
        initVertex();
        initEdge();
        mWorker = new Worker(mContext);
        mWorker.start();
    }

    public void initInstance() {
        linear = findViewById(R.id.linear);
        fabCancel = findViewById(R.id.fab_cancel);
        btFind = findViewById(R.id.bt_find);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fabCancel.setOnClickListener(this);
        btFind.setOnClickListener(this);
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

    public void initVertex() {
        DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
        mRootRef.child("events").child(eID).child("Graph").child("Vertexs").orderByChild("id").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot db : dataSnapshot.getChildren()) {
                    String _name = db.getKey().toString();
                    int _id = Integer.parseInt(db.child("id").getValue().toString());
                    Double _x = Double.parseDouble(db.child("x").getValue().toString());
                    Double _y = Double.parseDouble(db.child("y").getValue().toString());

                    Vertex vertex = new Vertex(_id, _name, _x, _y);
                    nodes.add(vertex);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }

    public void initEdge() {
        DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
        mRootRef.child("events").child(eID).child("Graph").child("Edges").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot db : dataSnapshot.getChildren()) {
                    String _name = db.getKey().toString();
                    int _id = Integer.parseInt(db.child("id").getValue().toString());
                    int _sid = Integer.parseInt(db.child("sid").getValue().toString()) - 1; //Start id at 1
                    int _did = Integer.parseInt(db.child("did").getValue().toString()) - 1; //Start id at 1

                    //bidirectional graph
                    addEdge(_name + "_1", _sid, _did, 1);
                    addEdge(_name + "_2", _did, _sid, 1);
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
                            Toast.makeText(NavigationActivity.this, "Profile", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(NavigationActivity.this, EventsActivity.class));
                        } else if (drawerItem.getIdentifier() == 200) {
                            Toast.makeText(NavigationActivity.this, "Events", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(NavigationActivity.this, EventsActivity.class));
                        } else if (drawerItem.getIdentifier() == 300) {
                            Toast.makeText(NavigationActivity.this, "About us", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(NavigationActivity.this, EventsActivity.class));
                        } else if (drawerItem.getIdentifier() == 400) {
                            Toast.makeText(NavigationActivity.this, "Logout", Toast.LENGTH_SHORT).show();
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
            Intent intent = new Intent(NavigationActivity.this, MainActivity.class);
            intent.putExtra("eID", eID);
            startActivity(intent);
        }
        else if (view == btFind) {
            try {
                path.clear();
            } catch (Exception e) {
                e.printStackTrace();
            }
            graph = new Graph(nodes, edges);
            dijkstra = new DijkstraAlgorithm(graph);

            int source = findVertex((int)x, (int)y);
            int destination = findVertex((int)tX, (int)tY);
            dijkstra.execute(nodes.get(source));
            path = dijkstra.getPath(nodes.get(destination));
            if(path == null || path.isEmpty())
                Toast.makeText(NavigationActivity.this, "Can't find path.", Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onResume() {
        mWorker.shouldContinue=true;
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
        mWorker.shouldContinue=false;
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
        Intent intent = new Intent(NavigationActivity.this, MainActivity.class);
        intent.putExtra("eID", eID);
        startActivity(intent);
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

    private void addEdge(String laneId, int sourceLocNo, int destLocNo, int duration) {
        Edge lane = new Edge(laneId, nodes.get(sourceLocNo), nodes.get(destLocNo), duration);
        edges.add(lane);
    }

    private int findVertex(double x, double y) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getX() == x && nodes.get(i).getY() == y)
                return i;
        }
        return -1;
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
                        startActivity(new Intent(NavigationActivity.this, LoginActivity.class));
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
