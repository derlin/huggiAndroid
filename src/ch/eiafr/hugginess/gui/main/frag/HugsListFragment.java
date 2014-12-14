package ch.eiafr.hugginess.gui.main.frag;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.sql.entities.Hug;
import ch.eiafr.hugginess.sql.entities.Hugger;
import ch.eiafr.hugginess.sql.helpers.HuggiDataSource;
import ch.eiafr.hugginess.tools.adapters.HugsListAdapter;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static ch.eiafr.hugginess.services.bluetooth.BluetoothConstants.*;

/**
 * @author: Lucy Linder
 * @date: 29.11.2014
 */
public class HugsListFragment extends Fragment{

    private ListView mListview;
    private List<Hug> mHugsList;
    private Map<String, Hugger> mHuggersMap;
    private HugsListAdapter mHugsListAdapter;


    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ){
        super.onCreate( savedInstanceState );
        View view = inflater.inflate( R.layout.activity_main_frag_hugslist, container, false );
        mListview = ( ListView ) view.findViewById( R.id.tab_contact_list );
        startLoadingData();
        return view;
    }


    private void startLoadingData(){
        new LoadDataAsyncTask().execute();
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
                    startLoadingData();
                    break;
            }

        }
    };


    // ----------------------------------------------------

    private class LoadDataAsyncTask extends AsyncTask<Void, Void, Boolean>{

        @Override
        protected Boolean doInBackground( Void... voids ){
            try(HuggiDataSource ds = new HuggiDataSource( getActivity(), true )){

                mHuggersMap = ds.getHuggersMap();
                mHugsList = ds.getHugs();

                Log.d( getActivity().getPackageName(),
                        String.format( "HugsList: data loaded. Huggers: %d, Hugs: %d.", //
                                mHuggersMap.size(), mHugsList.size() ));
                return true;

            }catch( SQLException e ){
                Log.d( getActivity().getPackageName(), "Error while loading data from db." );
                Log.d( getActivity().getPackageName(), e.toString()) ;
            }

            return false;
        }


        @Override
        protected void onPostExecute( Boolean ok ){
            super.onPostExecute( ok );
            if(ok){
                assert  mHugsList != null && mHuggersMap != null;
                mHugsListAdapter = new HugsListAdapter( getActivity(), mHugsList, mHuggersMap );
                mListview.setAdapter( mHugsListAdapter );
            }
        }
    }
}