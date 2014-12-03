package ch.eiafr.hugginess.listtests;

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
import ch.eiafr.hugginess.sql.Hug;
import ch.eiafr.hugginess.sql.Hugger;
import ch.eiafr.hugginess.sql.HuggersDataSource;
import ch.eiafr.hugginess.sql.HugsDataSource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static ch.eiafr.hugginess.bluetooth.BluetoothState.*;

/**
 * @author: Lucy Linder
 * @date: 29.11.2014
 */
public class ListFragment extends Fragment {
    private ListView mList;
    private List<Hug> hugs;
    private Map<String, Hugger> huggers;
    private HugsListAdapter mHugsListAdapter;


    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ){
        super.onCreate( savedInstanceState );
        View view = inflater.inflate( R.layout.hugs_list, container, false );
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
        LocalBroadcastManager.getInstance( getActivity() ).registerReceiver( mBroadcastReceiver, new
                IntentFilter( BTSERVICE_INTENT_FILTER ) );
        super.onCreate( savedInstanceState );
    }

    @Override
    public void onDestroy(){
        LocalBroadcastManager.getInstance( getActivity() ).unregisterReceiver( mBroadcastReceiver );
        super.onDestroy();
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive( Context context, Intent intent ){
            switch( intent.getStringExtra( EXTRA_EVT_TYPE ) ){

                case EVT_HUGS_RECEIVED:
                    try( HuggersDataSource huggersDataSource = new HuggersDataSource( getActivity(), true ) ){
                        Hug[] newHugs = ( Hug[] ) intent.getSerializableExtra( EVT_EXTRA_HUGS_LIST );

                        for( Hug hug : newHugs ){
                            hugs.add( 0, hug );
                            if( !huggers.containsKey( hug.getHuggerID() ) ){
                                Hugger hugger = huggersDataSource.getHugger( hug.getHuggerID() );
                                if(hugger != null) huggers.put( hugger.getId(), hugger );
                            }
                        }//end for

                        mHugsListAdapter.notifyDataSetChanged();
                    }catch( SQLException e ){
                        e.printStackTrace();
                    }

                    break;
            }

        }
    };

}