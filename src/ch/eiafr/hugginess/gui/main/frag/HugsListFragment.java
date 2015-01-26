package ch.eiafr.hugginess.gui.main.frag;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.services.bluetooth.HuggiBroadcastReceiver;
import ch.eiafr.hugginess.sql.entities.Hug;
import ch.eiafr.hugginess.sql.entities.Hugger;
import ch.eiafr.hugginess.sql.helpers.HugComparator;
import ch.eiafr.hugginess.sql.helpers.HuggiDataSource;
import ch.eiafr.hugginess.tools.adapters.HugsListAdapter;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * This class is the second fragment displayed in the main activity.
 * It holds a list of all the hugs, sorted by date (descending).
 * <p/>
 * If a hugger is a local contact, its name and picture will be displayed.
 * <p/>
 * By clicking on an item, the user has the option of viewing or saving the contact on his phone.
 * <p/>
 * creation date    29.11.2014
 * context          Projet de semestre Hugginess, EIA-FR, I3 2014-2015
 *
 * @author Lucy Linder
 */
public class HugsListFragment extends Fragment{

    private static final int HUGSLIST_FRAG_GROUP_ID = 'H'; // uniquely identify events from this list
    public static final int CONTACT_DETAILS_REQUEST_CODE = 1984; // used when starting the Contact Activity

    private ListView mListview;

    private HugsListAdapter mHugsListAdapter;
    private List<Hug> mHugsList;
    private Map<String, Hugger> mHuggersMap;

    private LoadDataAsyncTask mAsyncTask;

    private HugComparator mHugComparator;
    private ArrayAdapter<CharSequence> mSortAdapter;

    // ----------------------------------------------------

    private HuggiBroadcastReceiver mBroadcastReceiver = new HuggiBroadcastReceiver(){
        @Override
        public void onBtHugsReceived( int cnt ){
            // clean and fast : simply replace adapter and let the
            // db do the sorting/ordering stuff
            startLoadingData();


        }
    };

    // ----------------------------------------------------


    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ){
        super.onCreate( savedInstanceState );
        View view = inflater.inflate( R.layout.activity_main_frag_hugslist, container, false );
        mListview = ( ListView ) view.findViewById( R.id.tab_contact_list );

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( getActivity() );
        mHugComparator = new HugComparator();
        mHugComparator.setSortAscending( prefs.getBoolean( "sortOrder", false ) );
        mHugComparator.setSortType( prefs.getInt( "sortType", 0 ) );

        setHasOptionsMenu( true );
        registerForContextMenu( mListview );

        startLoadingData();
        return view;
    }


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



    @Override
    public void onSaveInstanceState( Bundle outState ){
        // keep track of the current tab
        PreferenceManager.getDefaultSharedPreferences( getActivity() ).edit() //
                .putInt( "sortType", mHugComparator.getSortType() ) //
                .putBoolean( "sortOrder", mHugComparator.isSortAscending() )  //
                .commit();
        super.onSaveInstanceState( outState );
    }

    @Override
    public void onActivityResult( int requestCode, int resultCode, Intent data ){
        if( requestCode == CONTACT_DETAILS_REQUEST_CODE ){
            startLoadingData(); // refresh TODO: refresh only the hugger ?
            notifyDataSetHasChanged();
        }else{
            super.onActivityResult( requestCode, resultCode, data );
        }
    }

    /* *****************************************************************
     * Handle sort menu in actionbar
     * ****************************************************************/

    @Override
    public void onCreateOptionsMenu( Menu menu, MenuInflater inflater ){
        if( menu == null ){
            inflater.inflate( R.menu.activity_main_menu, menu );
        }

        if( mSortAdapter == null){
           mSortAdapter = ArrayAdapter.createFromResource( getActivity(), R.array
                   .sort_array,  R.layout.adapter_simple_small_item_1);
        }

        // -- sort type: by date or duration

        MenuItem menuSort = menu.findItem( R.id.menu_sort_hug );
        menuSort.setVisible( true );

        final Spinner spinner = ( Spinner )  menuSort.getActionView();
        spinner.setAdapter( mSortAdapter );
        spinner.setSelection( mHugComparator.getSortType() );
        spinner.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected( AdapterView<?> adapterView, View view, int i, long l ){
                mHugComparator.setSortType( i );
                mHugsListAdapter.sort( mHugComparator );
            }


            @Override
            public void onNothingSelected( AdapterView<?> adapterView ){
            }
        } );

        // -- sort order: ascending or descending

        final MenuItem menuSortOrder = menu.findItem( R.id.menu_sort_order_hug );
        menuSortOrder.setVisible( true );
        menuSortOrder.setIcon( mHugComparator.isSortAscending() ? R.drawable.arrow_up : R.drawable.arrow_down );
        menuSortOrder.setOnMenuItemClickListener( new MenuItem.OnMenuItemClickListener(){
            @Override
            public boolean onMenuItemClick( MenuItem menuItem ){
                mHugComparator.setSortAscending( !mHugComparator.isSortAscending() );
                menuSortOrder.setIcon( mHugComparator.isSortAscending() ? R.drawable.arrow_up : R.drawable.arrow_down );
                mHugsListAdapter.sort( mHugComparator );
                return true;
            }
        } );


        super.onCreateOptionsMenu( menu, inflater );
    }

    /* *****************************************************************
     * Handling the context menu -- show/save contact
     * ****************************************************************/


    @Override
    public void onCreateContextMenu( ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo ){
        super.onCreateContextMenu( menu, v, menuInfo );

        int position = ( ( AdapterView.AdapterContextMenuInfo ) menuInfo ).position;
        try{
            Hug hug = mHugsListAdapter.getItem( position );
            Hugger hugger = mHuggersMap.get( hug.getHuggerID() );


            menu.setHeaderTitle( getActivity().getString( R.string.options ) );
            menu.add( HUGSLIST_FRAG_GROUP_ID, v.getId(), 0, hugger.isLocalContact() ?  //
                    getString( R.string.activity_main_frag_hugslist_option_menu_view_contact ) : //
                    getString( R.string.activity_main_frag_hugslist_option_menu_save_contact ) );

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
        Hugger selectedHugger = mHuggersMap.get( hug.getHuggerID() );
        showOrSaveContact( selectedHugger );

        return true;
    }

    // ----------------------------------------------------


    private void showOrSaveContact( Hugger hugger ){
        // choose what action to take after a click on an item
        if( hugger.isLocalContact() ){
            showContact( hugger.getDetails().getContactId() );
        }else{
            saveContact( hugger.getId() );
        }
    }


    private void showContact( long contactId ){
        // start the show contact detail activity
        // using startActivityForResult, we can detect when the user
        // comes back and handle potential changes to the contact info
        Intent intent = new Intent( Intent.ACTION_VIEW, //
                Uri.withAppendedPath( ContactsContract.Contacts.CONTENT_URI, //
                        "" + contactId ) );
        startActivityForResult( intent, CONTACT_DETAILS_REQUEST_CODE );

    }//end showContact


    private void saveContact( String phone ){
        // ask the system to launch the "save contact" view.
        // Using startActivityForResult, we can detect when the user
        // comes back and handle potential changes to the contact info
        phone = PhoneNumberUtils.formatNumber( "0041" + phone.substring( 1 ) );

        Intent intent = new Intent( ContactsContract.Intents.SHOW_OR_CREATE_CONTACT, Uri.parse( "tel:" + phone ) );
        // we decided to force the contact creation (on certain device, without this flag, the OS just
        // displays the list of contact and the user need a lot of clicks to get the job done)
        // TODO really a good idea ?
        intent.putExtra( ContactsContract.Intents.EXTRA_FORCE_CREATE, true );
        startActivityForResult( intent, CONTACT_DETAILS_REQUEST_CODE );
    }

    // ----------------------------------------------------


    private void notifyDataSetHasChanged(){
        // toggle a field in the sharedpreference when a contact might have changed.
        // This allows other activities/fragments displaying huggers to listen for
        // changes and update their view (see the HomeTabFragment for example)
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( getActivity() );
        boolean flag = prefs.getBoolean( getString( R.string.flag_data_set_changed ), false );
        prefs.edit().putBoolean( getString( R.string.flag_data_set_changed ), !flag ).apply();
    }



    /* *****************************************************************
     * Data loading
     * ****************************************************************/


    private void startLoadingData(){
        // query the database in a background process
        if( mAsyncTask == null ){
            mAsyncTask = new LoadDataAsyncTask();
            mAsyncTask.execute();

        }else{
            // a task was already running (should not happen)
            Log.e( getActivity().getPackageName(), "Trying to launch async task : LoadData in " + //
                    getClass().getName() + " while another one is executing" );
        }
    }//end loadDataFromDb

    // ----------------------------------------------------

    // asynctask which loads data from the sql database in the background and
    // then setup the adapter and listview
    private class LoadDataAsyncTask extends AsyncTask<Void, Void, Boolean>{

        @Override
        protected Boolean doInBackground( Void... voids ){
            // query the database in the background
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
            // update the view: setup the list and adapter
            super.onPostExecute( ok );
            if( ok ){
                assert mHugsList != null && mHuggersMap != null;
                // always recreate an adapter TODO maybe a better way ?
                mHugsListAdapter = new HugsListAdapter( getActivity(), mHugsList, mHuggersMap );
                mHugsListAdapter.sort( mHugComparator );
                mListview.setAdapter( mHugsListAdapter );
                mAsyncTask = null; // mark the job as done
            }
        }
    }
}