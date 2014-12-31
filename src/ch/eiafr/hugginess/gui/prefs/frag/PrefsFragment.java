package ch.eiafr.hugginess.gui.prefs.frag;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.util.Log;
import android.widget.Toast;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.gui.main.MainActivity;
import ch.eiafr.hugginess.services.bluetooth.HuggiBluetoothService;
import ch.eiafr.hugginess.services.bluetooth.HuggiBroadcastReceiver;
import ch.eiafr.hugginess.sql.helpers.HuggiDataSource;

import java.sql.SQLException;

import static ch.eiafr.hugginess.services.bluetooth.BluetoothConstants.*;

/**
 * @author: Lucy Linder
 * @date: 01.12.2014
 */
public class PrefsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener,
        Preference.OnPreferenceChangeListener{

    // TODO: disable prefs if not connected ? or better: disable whole menu

    private Preference mCalibratePref, mSleepPref, mShowConfigPref, mForceSync;
    private Preference mResetApp, mClearData;
    private EditTextPreference mSentDataPref;
    private ProgressDialog mProgressDialog;

    //-------------------------------------------------------------

    private HuggiBroadcastReceiver mBroadcastReceiver = new HuggiBroadcastReceiver(){
        @Override
        public void onBtAckReceived( char cmd, boolean ok ){
            dismissProgressDialog();
            Toast.makeText( getActivity(), "Cmd " + cmd + " : " + ( ok ? "ack" : "nak" ), Toast.LENGTH_SHORT ).show();
        }


        @Override
        public void onBtDataReceived( String line ){
            if( line.startsWith( DATA_PREFIX + CMD_DUMP_ALL ) ){
                dismissProgressDialog();
                String[] split = line.split( DATA_SEP );
                if( split.length == 3 ){ // TODO
                    // split[0] is @A
                    showConfigDialog( split[ 1 ], split[ 2 ] );
                }
            }
        }
    };

    //-------------------------------------------------------------


    private void showConfigDialog( String id, String data ){
        // TODO nicer ?
        AlertDialog.Builder builder = new AlertDialog.Builder( getActivity() );
        builder.setTitle( "Current configuration" );
        builder.setMessage( String.format( "\nID: %s\nDATA: %s\n", id, data ) );
        builder.setCancelable( true );
        builder.create().show();
    }


    public void onCreate( Bundle savedInstanceState ){
        super.onCreate( savedInstanceState );

        // Load the preferences from an XML resource
        addPreferencesFromResource( R.xml.activity_pref_frag );


        mCalibratePref = findPreference( getString( R.string.pref_calibrate ) );
        mCalibratePref.setOnPreferenceClickListener( this );

        mSleepPref = findPreference( getString( R.string.pref_sleep ) );
        mSleepPref.setOnPreferenceClickListener( this );


        mShowConfigPref = findPreference( getString( R.string.pref_show_config ) );
        mShowConfigPref.setOnPreferenceClickListener( this );

        mForceSync = findPreference( getString( R.string.pref_get_hug ) );
        mForceSync.setOnPreferenceClickListener( this );

        mResetApp = findPreference( getString( R.string.pref_reset_app ) );
        mResetApp.setOnPreferenceClickListener( this );

        mClearData = findPreference( getString( R.string.pref_clear_db ) );
        mClearData.setOnPreferenceClickListener( this );

        mSentDataPref = ( EditTextPreference ) findPreference( getString( R.string.pref_sent_data ) );
        mSentDataPref.getEditText().setFilters( new InputFilter[]{ new InputFilter.LengthFilter( DATA_MAX_SIZE ) } );
        mSentDataPref.setOnPreferenceChangeListener( this );

        mBroadcastReceiver.registerSelf( getActivity() );
    }


    private void showProgressDialog(){
        if( mProgressDialog == null ){
            mProgressDialog = new ProgressDialog( getActivity() );
            mProgressDialog.setIndeterminate( true );
        }

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

        HuggiBluetoothService mSPP = HuggiBluetoothService.getInstance();

        if( mSPP == null || !mSPP.isConnected() ){
            Toast.makeText( getActivity(), "Not connected...", Toast.LENGTH_SHORT ).show();
            return true;
        }

        if( preference == mCalibratePref ){
            mSPP.executeCommand( CMD_CALIBRATE );
            showProgressDialog();

        }else if( preference == mSleepPref ){
            mSPP.executeCommand( CMD_SLEEP );
            Toast.makeText( getActivity(), "Command sent!", Toast.LENGTH_SHORT ).show();

        }else if( preference == mShowConfigPref ){
            mSPP.executeCommand( CMD_DUMP_ALL );
            showProgressDialog();

        }else if( preference == mForceSync ){
            mSPP.executeCommand( CMD_SEND_HUGS );
            Toast.makeText( getActivity(), "Command sent!", Toast.LENGTH_SHORT ).show();

        }else if( preference == mResetApp ){
            showResetDialog( "Reset Application", true );

        }else if( preference == mClearData ){
            showResetDialog( "Clear Data", false );

        }

        return true;
    }


    private void restart( int delay ){
        Intent launchIntent = new Intent( getActivity(), MainActivity.class );
        PendingIntent intent = PendingIntent.getActivity( getActivity().getApplicationContext(), 0, launchIntent, 0 );
        AlarmManager manager = ( AlarmManager ) getActivity().getSystemService( Context.ALARM_SERVICE );
        manager.set( AlarmManager.RTC, System.currentTimeMillis() + delay, intent );
        System.exit( 2 );
    }


    private void clearData(){
        try( HuggiDataSource dbs = new HuggiDataSource( getActivity(), true ) ){
            dbs.clearAllData();
        }catch( SQLException e ){
            Log.e( getActivity().getPackageName(), "Preferences -- clearData: SQL Exception occurred: " + e );
        }
    }


    private void showResetDialog( String title, final boolean resetApp ){
        new AlertDialog.Builder( getActivity() ) //
                .setTitle( title ).setMessage( "This action cannot be undone. All data will be lost.\n" + //
                "Proceed anyway ?" ) //
                .setPositiveButton( "Yep!", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick( DialogInterface dialog, int which ){
                        clearData();
                        if( resetApp ){
                            // set the configured flag to false => first launch activity will
                            // show up upon restart
                            PreferenceManager.getDefaultSharedPreferences( getActivity() ).edit() //
                                    .putBoolean( getString( R.string.pref_is_configured ), false ) //
                                    .commit();
                        }
                        restart( 2 );
                    }

                } )  //
                .setNegativeButton( "Nope", null ) //
                .show();

    }


    @Override
    public boolean onPreferenceChange( Preference preference, Object newValue ){

        HuggiBluetoothService mSPP = HuggiBluetoothService.getInstance();

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
        mBroadcastReceiver.unregisterSelf( getActivity() );
        super.onDestroy();
    }


}//end class
