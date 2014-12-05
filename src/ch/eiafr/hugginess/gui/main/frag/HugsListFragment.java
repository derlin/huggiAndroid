package ch.eiafr.hugginess.gui.main.frag;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.tools.adapters.HugsListAdapter;
import ch.eiafr.hugginess.sql.entities.Hug;
import ch.eiafr.hugginess.sql.entities.Hugger;
import ch.eiafr.hugginess.sql.helpers.HuggersDataSource;
import ch.eiafr.hugginess.sql.helpers.HugsDataSource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static ch.eiafr.hugginess.services.bluetooth.BluetoothConstants.*;

/**
 * @author: Lucy Linder
 * @date: 29.11.2014
 */
public class HugsListFragment extends Fragment{
    private ListView mList;
    private List<Hug> hugs;
    private Map<String, Hugger> huggers;
    private HugsListAdapter mHugsListAdapter;


    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ){
        super.onCreate( savedInstanceState );
        View view = inflater.inflate( R.layout.activity_main_frag_hugslist, container, false );
        mList = ( ListView ) view.findViewById( R.id.tab_contact_list );

        loadDataFromDb();
        mHugsListAdapter = new HugsListAdapter( getActivity(), hugs, huggers );
        mList.setAdapter( mHugsListAdapter );


        return view;
    }


    private void loadDataFromDb(){
        try{
            HugsDataSource db = new HugsDataSource( getActivity() );
            db.open();
            hugs = db.getHugs();
            db.close();
        }catch( SQLException e ){
            e.printStackTrace();
            hugs = new ArrayList<>();
        }


        try{
            HuggersDataSource db = new HuggersDataSource( getActivity() );
            db.open();
            huggers = db.getHuggersMap();
            db.close();
        }catch( SQLException e ){
            e.printStackTrace();
            huggers = new TreeMap<>();
        }
    }//end loadDataFromDb

    //-------------------------------------------------------------


    @Override
    public void onCreate( Bundle savedInstanceState ){
        LocalBroadcastManager.getInstance( getActivity() ).registerReceiver( mBroadcastReceiver, new IntentFilter(
                BTSERVICE_INTENT_FILTER ) );
        super.onCreate( savedInstanceState );
    }


    @Override
    public void onDestroy(){
        LocalBroadcastManager.getInstance( getActivity() ).unregisterReceiver( mBroadcastReceiver );
        super.onDestroy();
    }


    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive( Context context, Intent intent ){
            switch( intent.getStringExtra( EXTRA_EVT_TYPE ) ){

                case EVT_HUGS_RECEIVED:
                    // clean and fast : simply replace adapter and let the
                    // db do the sorting/ordering stuff
                    loadDataFromDb();
                    mHugsListAdapter = new HugsListAdapter( getActivity(), hugs, huggers );
                    mList.setAdapter( mHugsListAdapter );
                    break;
            }

        }
    };

}