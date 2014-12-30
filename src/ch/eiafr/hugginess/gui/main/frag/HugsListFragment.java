package ch.eiafr.hugginess.gui.main.frag;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ListView;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.services.bluetooth.HuggiBroadcastReceiver;
import ch.eiafr.hugginess.sql.entities.Hug;
import ch.eiafr.hugginess.sql.entities.Hugger;
import ch.eiafr.hugginess.sql.helpers.HuggiDataSource;
import ch.eiafr.hugginess.tools.adapters.HugsListAdapter;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @author: Lucy Linder
 * @date: 29.11.2014
 */
public class HugsListFragment extends Fragment {

    private static final int HUGSLIST_FRAG_GROUP_ID = 'H';
    private static final int CONTACT_DETAILS_REQUEST_CODE = 1984;

    private ListView mListview;
    private List<Hug> mHugsList;
    private Map<String, Hugger> mHuggersMap;
    private HugsListAdapter mHugsListAdapter;
    private LoadDataAsyncTask mAsyncTask;


    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ){
        super.onCreate( savedInstanceState );
        View view = inflater.inflate( R.layout.activity_main_frag_hugslist, container, false );
        mListview = ( ListView ) view.findViewById( R.id.tab_contact_list );
        setHasOptionsMenu( true );
        registerForContextMenu( mListview );
        startLoadingData();
        return view;
    }


    private void startLoadingData(){
        if( mAsyncTask == null ){
            mAsyncTask = new LoadDataAsyncTask();
            mAsyncTask.execute();
        }else{
            Log.e( getActivity().getPackageName(), "Trying to launch async task : LoadData in " + //
                    getClass().getName() + " while another one is executing" );
        }
    }//end loadDataFromDb

    //-------------------------------------------------------------


    private HuggiBroadcastReceiver mBroadcastReceiver = new HuggiBroadcastReceiver() {
        @Override
        public void onBtHugsReceived( int cnt ){
            // clean and fast : simply replace adapter and let the
            // db do the sorting/ordering stuff
            startLoadingData();


        }
    };


    @Override
    public void onCreate( Bundle savedInstanceState ){
        mBroadcastReceiver.registerSelf( getActivity() );
        super.onCreate( savedInstanceState );
    }


    @Override
    public void onDestroy(){
        mBroadcastReceiver.unregisterSelf( getActivity() );
        super.onDestroy();
    }


    // ----------------------------------------------------


    @Override
    public void onCreateContextMenu( ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo ){
        super.onCreateContextMenu( menu, v, menuInfo );

        int position = ( ( AdapterView.AdapterContextMenuInfo ) menuInfo ).position;
        try{
            Hug hug = mHugsListAdapter.getItem( position );
            Hugger hugger = mHuggersMap.get( hug.getHuggerID() );

            menu.setHeaderTitle( "Options" );
            menu.add( HUGSLIST_FRAG_GROUP_ID, v.getId(), 0, hugger.isLocalContact() ? "View contact" :
                    "Save contact" );

        }catch( Exception e ){
            e.printStackTrace(); // TODO
        }
    }


    @Override
    public boolean onContextItemSelected( MenuItem item ){
        // check that the event comes from our list
        if( item.getGroupId() != HUGSLIST_FRAG_GROUP_ID ) return false;

        int position = ( ( AdapterView.AdapterContextMenuInfo ) item.getMenuInfo() ).position;
        Hug hug = mHugsListAdapter.getItem( position );
        Hugger hugger = mHuggersMap.get( hug.getHuggerID() );
        showOrSaveContact( hugger );

        return true;
    }


    private void showOrSaveContact( Hugger hugger ){
        if( hugger.isLocalContact() ){
            showContact( hugger.getDetails().getContactId() );
        }else{
            saveContact( hugger.getId() );
        }
    }


    private void showContact( long contactId ){
        Intent intent = new Intent( Intent.ACTION_VIEW, Uri.withAppendedPath( ContactsContract.Contacts
                .CONTENT_URI, "" + contactId ) );
        startActivityForResult( intent, CONTACT_DETAILS_REQUEST_CODE );

    }//end showContact


    private void saveContact( String phone ){
        phone = PhoneNumberUtils.formatNumber( "0041" + phone.substring( 1 ) );

        Intent intent = new Intent( ContactsContract.Intents.SHOW_OR_CREATE_CONTACT, Uri.parse( "tel:" +
                phone ) );
        intent.putExtra( ContactsContract.Intents.EXTRA_FORCE_CREATE, true );
        startActivityForResult( intent, CONTACT_DETAILS_REQUEST_CODE );
    }


    @Override
    public void onActivityResult( int requestCode, int resultCode, Intent data ){
        if( requestCode == CONTACT_DETAILS_REQUEST_CODE ){
            startLoadingData(); // refresh TODO: refresh only the hugger ?
        }else{
            super.onActivityResult( requestCode, resultCode, data );
        }
    }

    // ----------------------------------------------------

    private class LoadDataAsyncTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground( Void... voids ){
            try( HuggiDataSource ds = new HuggiDataSource( getActivity(), true ) ){

                mHuggersMap = ds.getHuggersMap();
                mHugsList = ds.getHugs();

                Log.d( getActivity().getPackageName(), String.format( "HugsList: data loaded. Huggers: %d, " +
                                "Hugs: %d" + ".", //
                        mHuggersMap.size(), mHugsList.size() ) );
                return true;

            }catch( SQLException e ){
                Log.d( getActivity().getPackageName(), "Error while loading data from db." );
                Log.d( getActivity().getPackageName(), e.toString() );
            }

            return false;
        }


        @Override
        protected void onPostExecute( Boolean ok ){
            super.onPostExecute( ok );
            if( ok ){
                assert mHugsList != null && mHuggersMap != null;
                mHugsListAdapter = new HugsListAdapter( getActivity(), mHugsList, mHuggersMap );
                mListview.setAdapter( mHugsListAdapter );
                mAsyncTask = null; // finished
            }
        }
    }
}