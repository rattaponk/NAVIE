package com.rattapon.navie.Fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rattapon.navie.NavigationActivity;
import com.rattapon.navie.R;
import com.rattapon.navie.TestNaviActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SearchFragment extends android.support.v4.app.ListFragment implements SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener {

    private View myFragmentView;
    private ListView listView;
    private TextView emptyTextView;

    private ArrayAdapter<String> mAdapter;
    private Context mContext;

    private double nX, nY;
    private List<String> mAllValues;
    private String eID;
    private HashMap<String, Double> sX = new HashMap<String, Double>();
    private HashMap<String, Double> sY = new HashMap<String, Double>();

    public SearchFragment() {
        // Required empty public constructor
    }

    public static SearchFragment newInstance() {
        SearchFragment fragment = new SearchFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        setHasOptionsMenu(true);
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            eID = bundle.getString("eID");
            nX =  bundle.getDouble("x");
            nY = bundle.getDouble("y");
        }
        initListData();
    }

    private void initListData(){
        mAllValues = new ArrayList<>();
        DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
        mRootRef.child("events").child(eID).child("Search").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot db : dataSnapshot.getChildren()) {
                    String name = db.child("name").getValue().toString();
                    Double x = Double.parseDouble(db.child("x").getValue().toString());
                    Double y = Double.parseDouble(db.child("y").getValue().toString());

                    mAllValues.add(name);
                    sX.put(name, x);
                    sY.put(name, y);
                }
                mAdapter = new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, mAllValues);
                setListAdapter(mAdapter);
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }

    @Override
    public void onListItemClick(ListView listView, View v, int position, long id) {
        String item = (String) listView.getAdapter().getItem(position);
        if (getActivity() instanceof OnItem1SelectedListener) {
            ((OnItem1SelectedListener) getActivity()).OnItem1SelectedListener(item);
        }
        Double x = sX.get(mAllValues.get(position));
        Double y = sY.get(mAllValues.get(position));

        String xx = sX.get(mAllValues.get(position)).toString();
        String yy = sY.get(mAllValues.get(position)).toString();
//        Toast.makeText(mContext, item , Toast.LENGTH_SHORT).show();
//        getFragmentManager().popBackStack();
        //To Navigation
        Intent intent = new Intent(getActivity(), NavigationActivity.class);
        intent.putExtra("tName", item);
        intent.putExtra("eID", eID);
        intent.putExtra("X", x);
        intent.putExtra("Y", y);
        startActivity(intent);
        hideSoftKeyboard(getView());
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Search");
        myFragmentView = inflater.inflate(R.layout.fragment_search, container, false);
        listView = myFragmentView.findViewById(android.R.id.list);
        emptyTextView = myFragmentView.findViewById(android.R.id.empty);
        listView.setEmptyView(emptyTextView);
        return myFragmentView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.search_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);
        searchView.setQueryHint("Search");


//        MenuItem mainItem = menu.findItem(R.id.action_to_search);
//        mainItem.setVisible(false);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (newText == null || newText.trim().isEmpty()) {
            resetSearch();
            return false;
        }

        List<String> filteredValues = new ArrayList<String>(mAllValues);
        for (String value : mAllValues) {
            if (!value.toLowerCase().contains(newText.toLowerCase())) {
                filteredValues.remove(value);
            }
        }

        mAdapter = new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, filteredValues);
        setListAdapter(mAdapter);

        return false;
    }

    public void resetSearch() {
        mAdapter = new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, mAllValues);
        setListAdapter(mAdapter);
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        return true;
    }

    public interface OnItem1SelectedListener {
        void OnItem1SelectedListener(String item);
    }


    public void hideSoftKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(getActivity().INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
