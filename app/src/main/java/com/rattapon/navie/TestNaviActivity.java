package com.rattapon.navie;

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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;
import com.rattapon.navie.JavaClass.DijkstraAlgorithm;
import com.rattapon.navie.JavaClass.Edge;
import com.rattapon.navie.JavaClass.Graph;
import com.rattapon.navie.JavaClass.Vertex;
import com.rattapon.navie.JavaClass.WifiList;
import com.rattapon.navie.JavaClass.WifiPoint;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static junit.framework.Assert.assertNotNull;

public class TestNaviActivity extends AppCompatActivity implements View.OnClickListener {

    private Toolbar toolbar;
    private LinearLayout linear;
    private ImageView ivMap;
    private FloatingActionButton fabCancel;
    private EditText etSX;
    private EditText etSY;
    private EditText etDX;
    private EditText etDY;
    private Button btFind;

    private String tName;
    private double tX, tY;
    private double x, y;
    private String eID;

    private List<Vertex> nodes;
    private List<Edge> edges;
    private Graph graph;
    private LinkedList<Vertex> path;
    private DijkstraAlgorithm dijkstra;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testnavi);

        eID = getIntent().getStringExtra("eID");
        tName = getIntent().getStringExtra("tName");
        tX = getIntent().getDoubleExtra("X", 0);
        tY = getIntent().getDoubleExtra("Y", 0);

        nodes = new ArrayList<Vertex>();
        edges = new ArrayList<Edge>();
        path = new LinkedList<Vertex>();

        initInstance();
        initVertex();
        initEdge();
    }

    public void initInstance() {
        linear = findViewById(R.id.li_test);
        fabCancel = findViewById(R.id.fab_cancel);
        etSX = findViewById(R.id.et_sx);
        etSY = findViewById(R.id.et_sy);
        etDX = findViewById(R.id.et_dx);
        etDY = findViewById(R.id.et_dy);
        btFind = findViewById(R.id.bt_find);

//        toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        fabCancel.setOnClickListener(this);
        btFind.setOnClickListener(this);
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


    @Override
    public void onClick(View view) {
        if (view == fabCancel) {
            Intent intent = new Intent(TestNaviActivity.this, MainActivity.class);
            intent.putExtra("eID", eID);
            startActivity(intent);
        }
        if (view == btFind) {
            try {
                path.clear();
            } catch (Exception e) {
                e.printStackTrace();
            }
            hideSoftKeyboard(findViewById(R.id.relative_test));

            x = Double.parseDouble(etSX.getText().toString());
            y = Double.parseDouble(etSY.getText().toString());
            tX = Double.parseDouble(etDX.getText().toString());
            tY = Double.parseDouble(etDY.getText().toString());

            graph = new Graph(nodes, edges);
            dijkstra = new DijkstraAlgorithm(graph);

            int source = findVertex(x, y);
            int destination = findVertex(tX, tY);
            dijkstra.execute(nodes.get(source));
            path = dijkstra.getPath(nodes.get(destination));

            if (path != null) {
                Draw2d d = new Draw2d(this);
                linear.removeAllViews();
                linear.addView(d);
            } else
                Toast.makeText(this, "Path NULL", Toast.LENGTH_SHORT).show();
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

            //draw path
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);
            paint.setColor(Color.RED);
            double old_x = x;
            double old_y = y;
            if(path != null) {
                for (Vertex vertex : path) {
                    double next_x = vertex.getX();
                    double next_y = vertex.getY();
                    c.drawLine(xmin + ((int) old_x * img.getWidth() / xscale), ymax - ((int) old_y * img.getHeight() / yscale), xmin + ((int) next_x * img.getWidth() / xscale), ymax - ((int) next_y * img.getHeight() / yscale), paint);
                    old_x = next_x;
                    old_y = next_y;
                }
            } else {
                Toast.makeText(TestNaviActivity.this, "Can't find path.", Toast.LENGTH_SHORT).show();
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
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(TestNaviActivity.this, MainActivity.class);
        intent.putExtra("eID", eID);
        startActivity(intent);
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

    public void hideSoftKeyboard(final View view) {
        final InputMethodManager imm = (InputMethodManager) getSystemService(this.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

}
