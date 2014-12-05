package ch.eiafr.hugginess.gui.prefs.frag;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputFilter;
import android.widget.Toast;
import ch.eiafr.hugginess.HuggiBTActivity;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.services.bluetooth.HuggiBluetoothService;

import static ch.eiafr.hugginess.services.bluetooth.BluetoothConstants.*;

/**
 * @author: Lucy Linder
 * @date: 01.12.2014
 */
public class ManageShirtPrefFragment extends PreferenceFragment implements Preference
        .OnPreferenceClickListener, Preference.OnPreferenceChangeListener{

    // TODO: disable prefs if not connected ? or better: disable whole menu

    Preference mCalibratePref, mSleepPref, mShowConfigPref, mForceSync;
    EditTextPreference mSentDataPref;
    private HuggiBTActivity activity;
    private ProgressDialog mProgressDialog;


    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive( Context context, Intent intent ){
            switch( intent.getStringExtra( EXTRA_EVT_TYPE ) ){

                case EVT_DATA_RECEIVED:
                    String data = intent.getStringExtra( EVT_EXTRA_DATA );
                    if( data.startsWith( DATA_PREFIX + CMD_DUMP_ALL ) ){
                        dismissProgressDialog();
                        String[] split = data.split( DATA_SEP );
                        if( split.length == 3 ){ // TODO
                            // split[0] is @A
                            showConfigDialog( split[ 1 ], split[ 2 ] );
                        }
                    }
                    break;
                case EVT_ACK_RECEIVED:
                    dismissProgressDialog();
                    char cmd = intent.getCharExtra( EVT_EXTRA_ACK_CMD, '-' );
                    boolean ok = intent.getBooleanExtra( EVT_EXTRA_ACK_STATUS, false );

                    Toast.makeText( getActivity(), "Cmd " + cmd + " : " + ( ok ? "ack" : "nak" ), Toast
                            .LENGTH_SHORT ).show();
                    break;

                case EVT_HUGS_RECEIVED:
                    Object test = intent.getSerializableExtra( EVT_EXTRA_HUGS_LIST );
                    break;
            }
        }
    };



    private void showConfigDialog( String id, String data ){
        // TODO nicer
        AlertDialog.Builder builder = new AlertDialog.Builder( getActivity() );
        builder.setTitle( "Current configuration" );
        builder.setMessage( String.format( "\nID: %s\nDATA: %s\n", id, data ) );
        builder.setCancelable( true );
        builder.create().show();
    }


    public void onCreate( Bundle savedInstanceState ){
        super.onCreate( savedInstanceState );

        // Load the preferences from an XML resource
        addPreferencesFromResource( R.xml.activity_pref_frag_manageshirt );


        mCalibratePref = findPreference( getString( R.string.pref_calibrate ) );
        mSleepPref = findPreference( getString( R.string.pref_sleep ) );
        mSentDataPref = ( EditTextPreference ) findPreference( getString( R.string.pref_sent_data ) );
        mShowConfigPref = findPreference( getString( R.string.pref_show_config ) );
        mForceSync = findPreference( getString( R.string.pref_get_hug ) );
        mCalibratePref.setOnPreferenceClickListener( this );
        mSleepPref.setOnPreferenceClickListener( this );
        mShowConfigPref.setOnPreferenceClickListener( this );
        mSentDataPref.getEditText().setFilters( new InputFilter[]{ new InputFilter.LengthFilter(
                DATA_MAX_SIZE ) } );



        mSentDataPref.setOnPreferenceChangeListener( this );

        activity = ( ( HuggiBTActivity ) getActivity() );

        LocalBroadcastManager.getInstance( getActivity() ).registerReceiver( mBroadcastReceiver, new
                IntentFilter( BTSERVICE_INTENT_FILTER ) );
    }


    private void showProgressDialog(){
        if( mProgressDialog == null ){
            mProgressDialog = new ProgressDialog( getActivity() );
            mProgressDialog.setIndeterminate( true );
        }    // end if

        mProgressDialog.setMessage( "Executing..." );
        mProgressDialog.show();

    }


    private void dismissProgressDialog(){
        if( mProgressDialog != null && mProgressDialog.isShowing() ){
            mProgressDialog.dismiss();
        }
    }


    @Override
    public boolean onPreferenceClick( Preference preference ){

        HuggiBluetoothService mSPP = activity.getHuggiService();

        if( mSPP == null || !mSPP.isConnected() ){
            Toast.makeText( getActivity(), "Not connected...", Toast.LENGTH_SHORT ).show();
            return true;
        }

        if( preference == mCalibratePref ){
            mSPP.executeCommand( CMD_CALIBRATE );
            showProgressDialog();

        }else if( preference == mSleepPref ){
            mSPP.executeCommand( CMD_SLEEP );

        }else if( preference == mShowConfigPref ){
            mSPP.executeCommand( CMD_DUMP_ALL );
            showProgressDialog();

        }else if(preference == mForceSync){
            mSPP.executeCommand( CMD_SEND_HUGS );
        }

        return true;
    }

    @Override
    public boolean onPreferenceChange( Preference preference, Object newValue ){

        HuggiBluetoothService mSPP = activity.getHuggiService();

        if( mSPP == null || !mSPP.isConnected() ){
            Toast.makeText( getActivity(), "Not connected...", Toast.LENGTH_SHORT ).show();
            return true;
        }

        String val = newValue.toString();
        if( val.length() >= DATA_MAX_SIZE ){
            Toast.makeText( getActivity(), "Input too long...", Toast.LENGTH_SHORT ).show();
            return false;
        }

        mSPP.executeCommand( CMD_SET_DATA, val );
        showProgressDialog();

        return true;
    }

    @Override
    public void onDestroy(){
        LocalBroadcastManager.getInstance( getActivity() ).unregisterReceiver( mBroadcastReceiver );
        super.onDestroy();
    }




}//end class
