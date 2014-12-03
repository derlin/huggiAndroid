package ch.eiafr.hugginess.prefs;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import ch.eiafr.hugginess.HuggiBTActivity;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.bluetooth.DeviceList;
import ch.eiafr.hugginess.bluetooth.HuggiBluetoothService;

import java.util.List;

import static ch.eiafr.hugginess.bluetooth.BluetoothState.EXTRA_DEVICE_ADDRESS;
import static ch.eiafr.hugginess.bluetooth.BluetoothState.REQUEST_CONNECT_DEVICE;

/**
 * @author: Lucy Linder
 * @date: 30.11.2014
 */
public class PrefsActivity extends PreferenceActivity implements HuggiBTActivity{


    //-------------------------------------------------------------

    private HuggiBluetoothService mSPP;

    protected ServiceConnection mServiceConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected( ComponentName name, IBinder binder ){
            if( mSPP == null ){
                mSPP = ( ( HuggiBluetoothService.BTBinder ) binder ).getService();
//                findPreference( "manage_pref_header" ).setEnabled( true );
            }
        }


        @Override
        public void onServiceDisconnected( ComponentName name ){
            mSPP = null;
//            findViewById( R.id.manage_pref_header ).setEnabled( false );
        }
    };


    @Override
    protected void onDestroy(){
        this.unbindService( mServiceConnection );
        super.onDestroy();
    }

    // ----------------------------------------------------

    @Override
    protected void onCreate( Bundle savedInstanceState ){
        super.onCreate( savedInstanceState );

        Intent i = new Intent( this, HuggiBluetoothService.class );
        this.bindService( i, mServiceConnection, 0 ); //Context.BIND_AUTO_CREATE );

        getActionBar().setDisplayHomeAsUpEnabled( true );



        // Display the fragment as the main content.
        //getFragmentManager().beginTransaction().replace( android.R.id.content, new PrefsFragment() ).commit();
    }


    @Override
    public void onBuildHeaders(List<PreferenceActivity.Header> target) {
        loadHeadersFromResource( R.xml.header_prefs, target );
    }


    @Override
    public boolean onOptionsItemSelected( MenuItem item ){
        switch( item.getItemId() ){
            case android.R.id.home:{
                //getFragmentManager().popBackStack();
                onBackPressed();
                return true;
            }
            default:
                return super.onOptionsItemSelected( item );
        }
    }

    //-------------------------------------------------------------
    @Override
    protected boolean isValidFragment(String fragmentName) {
        return "ch.eiafr.hugginess.prefs.ManageShirtPrefFragment".equals( fragmentName ) ||
         "ch.eiafr.hugginess.prefs.PrefsActivity$PrefsFragment".equals( fragmentName );
    }
    //-------------------------------------------------------------

    @Override
    public HuggiBluetoothService getHuggiService(){
        return mSPP;
    }

    /* *****************************************************************
     * fragment
     * ****************************************************************/

    public static class PrefsFragment extends PreferenceFragment{

        Preference mTshirtAddPref;

        @Override
        public void onCreate( Bundle savedInstanceState ){
            super.onCreate( savedInstanceState );

            // Load the preferences from an XML resource
            addPreferencesFromResource( R.xml.core_prefs );

            mTshirtAddPref = findPreference( getString( R.string.pref_paired_tshirt ) );
            mTshirtAddPref.setOnPreferenceClickListener( new Preference.OnPreferenceClickListener(){


                @Override
                public boolean onPreferenceClick( Preference preference ){
                    Intent intent = new Intent( getActivity(), DeviceList.class );
                    startActivityForResult( intent, REQUEST_CONNECT_DEVICE );
                    return true;
                }
            } );

        }


        @Override
        public void onActivityResult( int requestCode, int resultCode, Intent data ){

            if( requestCode == REQUEST_CONNECT_DEVICE ){

                if( resultCode == Activity.RESULT_OK ){
                    String address = data.getExtras().getString( EXTRA_DEVICE_ADDRESS );
                    PreferenceManager.getDefaultSharedPreferences( getActivity() ).edit().putString( getString( R.string.pref_paired_tshirt ), address ) //
                            .putBoolean( getString( R.string.pref_is_configured ), true ) //
                            .commit();

                    mTshirtAddPref.getOnPreferenceChangeListener().onPreferenceChange( mTshirtAddPref, address );
                }

            }
        }
    } // end static class


}