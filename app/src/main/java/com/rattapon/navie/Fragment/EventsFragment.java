package com.rattapon.navie.Fragment;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
import com.rattapon.navie.MainActivity;
import com.rattapon.navie.NavigationActivity;
import com.rattapon.navie.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class EventsFragment extends android.support.v4.app.ListFragment implements SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener {

    private View myFragmentView;
    private ListView listView;
    private TextView emptyTextView;

    private ArrayAdapter<String> mAdapter;
    private Context mContext;

    private List<String> mAllValues;
    private HashMap<String, String> events = new HashMap<String, String>();

    public EventsFragment() {
        // Required empty public constructor
    }

    public static EventsFragment newInstance() {
        EventsFragment fragment = new EventsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        setHasOptionsMenu(true);
        initListData();
        Log.d("Events", "Events : " + mAllValues);
    }

    @Override
    public void onListItemClick(ListView listView, View v, int position, long id) {
        String item = (String) listView.getAdapter().getItem(position);
        if (getActivity() instanceof SearchFragment.OnItem1SelectedListener) {
            ((SearchFragment.OnItem1SelectedListener) getActivity()).OnItem1SelectedListener(item);
        }
        String eid = events.get(item);
//        Toast.makeText(mContext, item + "\nID : " + eid, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.putExtra("eID", eid);
        startActivity(intent);
        hideSoftKeyboard(getView());
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        myFragmentView = inflater.inflate(R.layout.fragment_events, container, false);
        listView = myFragmentView.findViewById(android.R.id.list);
        emptyTextView = myFragmentView.findViewById(android.R.id.empty);
        listView.setEmptyView(emptyTextView);
        return myFragmentView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.search_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);
        searchView.setQueryHint("Search");

        super.onCreateOptionsMenu(menu, inflater);
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

    private void initListData() {
        mAllValues = new ArrayList<>();
        DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
        mRootRef.child("events").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot db : dataSnapshot.getChildren()) {
                    String eid = db.getKey().toString();
                    String ename =  db.child("title").getValue().toString();

                    events.put(ename,eid);
                    mAllValues.add(ename);
                    Log.d("Events", "Add : " + ename);
                }
                mAdapter = new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, mAllValues);
                setListAdapter(mAdapter);
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }

    public void hideSoftKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(getActivity().INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
