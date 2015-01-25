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
import ch.eiafr.hugginess.tools.preferences.IntEditTextPreference;

import java.sql.SQLException;

import static ch.eiafr.hugginess.services.bluetooth.BluetoothConstants.*;


/**
 * This class is the fragment responsible for the App Settings.
 * <p/>
 * There are three kind of settings:
 * <ul>
 * <li>Core Settings: id of the HuggiShirt and data the former sends during a hug</li>
 * <li>Commands: allow the user to send predefined commands to the HuggiSHirt</li>
 * <li>Reset: clear database, reset application</li>
 * </ul>
 * <p/>
 * creation date    01.12.2014
 * context          Projet de semestre Hugginess, EIA-FR, I3 2014-2015
 *
 * @author Lucy Linder
 */
public class PrefsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener, Preference
        .OnPreferenceChangeListener{

    // TODO: disable prefs if not connected ? or better: disable whole menu

    private Preference mCalibratePref, mSleepPref, mShowConfigPref, mForceSyncPref;
    private Preference mChangeHSPref, mResetAppPref, mClearDataPref;
    private EditTextPreference mSentDataPref;
    private IntEditTextPreference mTerminalMaxLinesPref;
    private ProgressDialog mProgressDialog;

    //-------------------------------------------------------------

    private HuggiBroadcastReceiver mBroadcastReceiver = new HuggiBroadcastReceiver(){
        @Override
        public void onBtAckReceived( char cmd, boolean ok ){
            // no notification, since the main activity is still running
            // --> a toast will appear
            dismissProgressDialog();
        }


        @Override
        public void onBtDataReceived( String line ){
            if( line.startsWith( DATA_PREFIX + CMD_DUMP_ALL ) ){
                // current configuration is in the format @A!id!data
                dismissProgressDialog();
                String[] split = line.split( DATA_SEP );
                if( split.length == 3 ){
                    // split[0] is @A
                    showCurrentConfigDialog( split[ 1 ], split[ 2 ] );
                }
            }
        }
    };

    //-------------------------------------------------------------


    public void onCreate( Bundle savedInstanceState ){
        super.onCreate( savedInstanceState );

        mBroadcastReceiver.registerSelf( getActivity() );

        // Load the preferences from an XML resource
        addPreferencesFromResource( R.xml.activity_pref_frag );


        // ---------- config

        mShowConfigPref = findPreference( getString( R.string.pref_show_config ) );
        mShowConfigPref.setOnPreferenceClickListener( this );

        mSentDataPref = ( EditTextPreference ) findPreference( getString( R.string.pref_sent_data ) );
        mSentDataPref.getEditText().setFilters( new InputFilter[]{ new InputFilter.LengthFilter( DATA_MAX_SIZE ) } );
        mSentDataPref.setOnPreferenceChangeListener( this );

        // ---------- commands

        mCalibratePref = findPreference( getString( R.string.pref_calibrate ) );
        mCalibratePref.setOnPreferenceClickListener( this );

        mSleepPref = findPreference( getString( R.string.pref_sleep ) );
        mSleepPref.setOnPreferenceClickListener( this );


        mForceSyncPref = findPreference( getString( R.string.pref_get_hug ) );
        mForceSyncPref.setOnPreferenceClickListener( this );

        // ---------- reset

        mChangeHSPref = findPreference( getString( R.string.pref_change_pairing ) );
        mChangeHSPref.setOnPreferenceClickListener( this );

        mResetAppPref = findPreference( getString( R.string.pref_reset_app ) );
        mResetAppPref.setOnPreferenceClickListener( this );

        mClearDataPref = findPreference( getString( R.string.pref_clear_db ) );
        mClearDataPref.setOnPreferenceClickListener( this );

        // ---------- misc


        mTerminalMaxLinesPref = ( IntEditTextPreference ) findPreference( getString( R.string.pref_terminal_max_lines
        ) );
        mTerminalMaxLinesPref.setOnPreferenceChangeListener( this );
    }


    @Override
    public void onDestroy(){
        mBroadcastReceiver.unregisterSelf( getActivity() );
        super.onDestroy();
    }


    // ---------------------------------------------------- dialogs


    private void showCurrentConfigDialog( String id, String data ){
        // display a simple dialog with the configuration (id + data)
        AlertDialog.Builder builder = new AlertDialog.Builder( getActivity() );
        builder.setTitle( getString( R.string.activity_pref_text_current_config ) );
        builder.setMessage( String.format( getString( R.string.activity_pref_text_current_config_format ), id, data ) );
        builder.setCancelable( true );
        builder.create().show();
    }


    private void showResetDialog( String title, final boolean clearData, final boolean resetApp ){
        // ask for confirmation before clearing data
        // param: title the dialog title (clear data or reset app)
        // param: clearData whether or not to clear the database
        // param: resetApp whether or not to clear the HuggiShirt mac address/start a new pairing process
        new AlertDialog.Builder( getActivity() ) //
                .setTitle( title ).setMessage( getString( R.string.activity_pref_dialog_confirm_delete ) ) //
                .setPositiveButton( getString( R.string.yes ), new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick( DialogInterface dialog, int which ){
                        if( clearData ){
                            // clear the sqlite database
                            clearData();
                        }
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
                .setNegativeButton( getString( R.string.cancel ), null ) //
                .show();

    }


    private void showProgressDialog(){
        // show a progress dialog to notify the user a command is
        // executing
        if( mProgressDialog == null ){
            mProgressDialog = new ProgressDialog( getActivity() );
            mProgressDialog.setIndeterminate( true );
        }

        mProgressDialog.setMessage( getString( R.string.executing ) );
        mProgressDialog.show();

    }


    private void dismissProgressDialog(){
        // dismiss the progress dialog after a ack/nak has been received
        if( mProgressDialog != null && mProgressDialog.isShowing() ){
            mProgressDialog.dismiss();
        }
    }

    // --------------------------------------- reset utils


    private void clearData(){
        // clear the database
        try( HuggiDataSource dbs = new HuggiDataSource( getActivity(), true ) ){
            dbs.clearAllData();
        }catch( SQLException e ){
            Log.e( getActivity().getPackageName(), "Preferences -- clearData: SQL Exception occurred: " + e );
        }
    }


    private void restart( int delay ){
        // restart the whole application (to be used after a reset)
        // param: delay, how many ms to wait before restart

        if( delay < 1 ) delay = 1; // we are never too careful

        // schedule a restart
        Intent launchIntent = new Intent( getActivity(), MainActivity.class );
        PendingIntent intent = PendingIntent.getActivity( getActivity().getApplicationContext(), 0, launchIntent, 0 );
        AlarmManager manager = ( AlarmManager ) getActivity().getSystemService( Context.ALARM_SERVICE );
        manager.set( AlarmManager.RTC, System.currentTimeMillis() + delay, intent );
        System.exit( 2 ); // shut down the app
    }

    // ----------------------------------- preference listeners


    @Override
    public boolean onPreferenceClick( Preference preference ){
        // handle the command and reset preferences

        HuggiBluetoothService mSPP = HuggiBluetoothService.getInstance();


        // -- reset

        if( preference == mChangeHSPref ){
            showResetDialog( getString( R.string.activity_pref_dialog_title_change_shirt ), false, true );

        }else if( preference == mClearDataPref ){
            showResetDialog( getString( R.string.activity_pref_dialog_title_clear_data ), true, false );

        }else if( preference == mResetAppPref ){
            showResetDialog( getString( R.string.activity_pref_dialog_title_reset_app ), true, true );

        }else{ // commands need a bluetooth connection

            if( mSPP == null || !mSPP.isConnected() ){
                // do nothing if the bluetooth connection is off
                Toast.makeText( getActivity(), getString( R.string.activity_pref_toast_error_not_connected ), //
                        Toast.LENGTH_SHORT ).show();
                return true;
            }

            // -- commands

            if( preference == mCalibratePref ){
                mSPP.executeCommand( CMD_CALIBRATE );
                showProgressDialog();

            }else if( preference == mSleepPref ){
                mSPP.executeCommand( CMD_SLEEP );
                Toast.makeText( getActivity(), getString( R.string.command_sent ), Toast.LENGTH_SHORT ).show();

            }else if( preference == mShowConfigPref ){
                mSPP.executeCommand( CMD_DUMP_ALL );
                showProgressDialog();

            }else if( preference == mForceSyncPref ){
                mSPP.executeCommand( CMD_SEND_HUGS );
                Toast.makeText( getActivity(), getString( R.string.command_sent ), Toast.LENGTH_SHORT ).show();

            }
        }
        return true;
    }


    @Override
    public boolean onPreferenceChange( Preference preference, Object newValue ){
        // handle preferences with input (configure HuggiShirt data + terminal nbr of lines)

        if( preference == mSentDataPref ){
            HuggiBluetoothService mSPP = HuggiBluetoothService.getInstance();

            if( mSPP == null || !mSPP.isConnected() ){
                Toast.makeText( getActivity(), getString( R.string.activity_pref_toast_error_not_connected ),//
                        Toast.LENGTH_SHORT ).show();
                return true;
            }

            String val = newValue.toString();
            if( val.length() >= DATA_MAX_SIZE ){
                Toast.makeText( getActivity(), String.format(//
                                getString( R.string.activity_pref_toast_error_input_too_long ), DATA_MAX_SIZE ),//
                        Toast.LENGTH_SHORT ).show();
                return false;
            }

            mSPP.executeCommand( CMD_SET_DATA, val );
            showProgressDialog();

        }else if( preference == mTerminalMaxLinesPref ){
            try{
                int maxLines = Integer.valueOf( newValue.toString() );
                if( maxLines >= 10 ){
                    Toast.makeText( getActivity(), getString( R.string.activity_pref_toast_prefs_saved ),//
                            Toast.LENGTH_SHORT ).show();
                    return true;
                }
            }catch( NullPointerException | NumberFormatException e ){
                ;
            }
            Toast.makeText( getActivity(), getString( R.string.activity_pref_toast_error_value_too_small ),//
                    Toast.LENGTH_SHORT ).show();
            return false;
        }

        return true;

    }


}//end class
