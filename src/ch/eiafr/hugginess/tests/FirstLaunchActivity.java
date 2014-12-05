package ch.eiafr.hugginess.tests;


import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.bluetooth.DeviceList;
import ch.eiafr.hugginess.bluetooth.HuggiBluetoothService;

import static ch.eiafr.hugginess.bluetooth.BluetoothState.*;

/**
 * @author: Lucy Linder
 * @date: 01.12.2014
 */
public class FirstLaunchActivity extends FragmentActivity{

    private String mMyPhoneNumber;

    //-------------------------------------------------------------
    private interface FirstLaunchFragment{
        void onFail();
    }
    //-------------------------------------------------------------

    private HuggiBluetoothService mSPP;


    private FirstLaunchFragment mCurrentFragment;
    private String mHuggerId;
    private String mShirtAddress;

    private ServiceConnection mServiceConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected( ComponentName name, IBinder binder ){
            Log.d( "lala", "onServiceConnected" );
            mSPP = ( ( HuggiBluetoothService.BTBinder ) binder ).getService();
            step1();
        }


        @Override
        public void onServiceDisconnected( ComponentName name ){
            Log.d( "lala", "onServiceDisconnected" );
            mSPP = null;
        }
    };

    private boolean mine = false;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){
        private int mFailedCount = 0;


        @Override
        public void onReceive( Context context, Intent intent ){

            switch( intent.getStringExtra( EXTRA_EVT_TYPE ) ){
                case EVT_CONNECTED:
                    step3a();
                    break;

                case EVT_DISCONNECTED:
                    if( !mine ){
                        Toast.makeText( FirstLaunchActivity.this, "Disconnected", Toast.LENGTH_SHORT ).show();
                        finish(); // TODO
                    }else{
                        mine = false;
                    }
                    break;

                case EVT_CONNECTION_FAILED:
                    mFailedCount++;
                    if( mFailedCount < 3 ){
                        mSPP.connect( mShirtAddress );
                        Toast.makeText( FirstLaunchActivity.this, "Failed to connect, retrying", Toast.LENGTH_SHORT )
                                .show();
                    }else{
                        // TODO: dialog
                        //                        step1();
                        mFailedCount = 0;
                        mCurrentFragment.onFail();
                    }
                    break;

                case EVT_DATA_RECEIVED:
                    String data = intent.getStringExtra( EVT_EXTRA_DATA );
                    if( data.startsWith( DATA_PREFIX + CMD_DUMP_ALL ) ){
                        String[] split = data.split( DATA_SEP );
                        if( split.length == 3 ){ // TODO
                            // split[0] is @A
                            step3b( split[ 1 ], split[ 2 ] );
                        }else{
                            step3b( "", "" );
                        }
                    }
                    break;

                case EVT_ACK_RECEIVED:
                    if( intent.getCharExtra( EVT_EXTRA_ACK_CMD, ' ' ) == CMD_SET_ID ){ // TODO set id
                        Boolean ok = intent.getBooleanExtra( EVT_EXTRA_ACK_STATUS, false );
                        if( ok ){
                            finalStep();
                        }else{
                            mCurrentFragment.onFail();
                        }
                    }
            }
        }
    };


    // ----------------------------------------------------


    @Override
    public void onCreate( Bundle savedInstanceState ){
        super.onCreate( savedInstanceState );
        this.bindService( new Intent( this, HuggiBluetoothService.class ), mServiceConnection, 0 );
        LocalBroadcastManager.getInstance( this ).registerReceiver( mBroadcastReceiver, new IntentFilter(
                BTSERVICE_INTENT_FILTER ) );
        setContentView( R.layout.first_launch_activity );

        //        mTextView = ( TextView ) findViewById( R.id.text );
        //        mProgressBar = ( ProgressBar ) findViewById( R.id.progressBar );

    }

    // ----------------------------------------------------


    @Override
    public void onDestroy(){
        LocalBroadcastManager.getInstance( this ).unregisterReceiver( mBroadcastReceiver );
        this.unbindService( mServiceConnection );
        super.onDestroy();
    }


    @Override
    public void onSaveInstanceState( Bundle outState ){

    }


    /* *****************************************************************
     * onActivityResult
     * ****************************************************************/


    @Override
    public void onActivityResult( int requestCode, int resultCode, Intent data ){

        if( requestCode == REQUEST_ENABLE_BT ){
            if( resultCode == Activity.RESULT_OK ){
                step2();
            }else{
                finish();    // TODO
            }

        }else{
            super.onActivityResult( requestCode, resultCode, data );
        }
    }


    /* *****************************************************************
     * bluetooth enabled
     * ****************************************************************/


    private void step1(){
        if( !mSPP.isBluetoothEnabled() ){
            Intent btIntent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
            startActivityForResult( btIntent, REQUEST_ENABLE_BT );
        }else{
            step2();
        }
    }


    /* *****************************************************************
     * Choose device
     * ****************************************************************/


    private void step2(){

        if( true ){
            //            DialogFragment newFragment = MyAlertDialogFragment.newInstance( "lala" );
            //            newFragment.show( getSupportFragmentManager(), "dialog" );
            switchFragments( new ChooseTshirtFragment() );
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setCancelable( false );
        builder.setTitle( "Select your Huggi-Shirt" );
        builder.setMessage( "You will now choose your Huggi-Shirt." +  //
                "\nPlease, ensure that your Huggi-Shirt is powered and paired before proceeding." );
        builder.setCancelable( false );

        builder.setPositiveButton( "Select shirt", new DialogInterface.OnClickListener(){
            @Override
            public void onClick( DialogInterface dialog, int which ){
                Intent intent = new Intent( FirstLaunchActivity.this, DeviceList.class );
                startActivityForResult( intent, REQUEST_CONNECT_DEVICE );
            }
        } );

        builder.setNegativeButton( "Bluetooth settings", new DialogInterface.OnClickListener(){
            @Override
            public void onClick( DialogInterface dialog, int which ){
                Intent i = new Intent( Settings.ACTION_BLUETOOTH_SETTINGS );
                startActivity( i );
            }
        } );

        builder.create().show();


    }


    /* *****************************************************************
     * confirm/change ID
     * ****************************************************************/


    private void step3a(){
        mSPP.executeCommand( CMD_DUMP_ALL );
    }


    private void step3b( String id, String data ){
        mHuggerId = id;

        mMyPhoneNumber = getMyPhoneNumber();
        boolean compare = PhoneNumberUtils.compare( mMyPhoneNumber, id );
        if( false ){// compare ){
            finalStep();
        }else{
            switchFragments( new ConfirmIDFragment() );
        }


    }


    private void step3b_( String id, String data ){
        AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setCancelable( true );
        builder.setTitle( "Current configuration" );
        builder.setMessage( String.format( "\nID: %s\nDATA: %s\nPhone: %s\nIs this correct ?", id, data,
                getMyPhoneNumber() ) );

        builder.setPositiveButton( "Yep", new DialogInterface.OnClickListener(){
            @Override
            public void onClick( DialogInterface dialogInterface, int i ){
                finalStep();
            }
        } );
        builder.create().show();
    }

     /* *****************************************************************
     * final
     * ****************************************************************/


    private void finalStep(){
        Toast.makeText( this, "Configuration done. Welcome !", Toast.LENGTH_SHORT ).show();
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences( this ).edit();
        editor.putBoolean( getString( R.string.pref_is_configured ), true );
        editor.putString( getString( R.string.pref_paired_tshirt ), mShirtAddress );
        editor.commit();

        Intent intent = new Intent( this, TabTestActivity.class );
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity( intent );
        finish();
    }


    private String getMyPhoneNumber(){
        TelephonyManager mTelephonyMgr;
        mTelephonyMgr = ( TelephonyManager ) getSystemService( Context.TELEPHONY_SERVICE );
        return mTelephonyMgr.getLine1Number();
    }


    private void switchFragments( Fragment f ){
        // Execute a transaction, replacing any existing fragment
        // with this one inside the frame.
        mCurrentFragment = ( FirstLaunchFragment ) f;
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace( R.id.first_launch_fragment_layout, f );
        ft.setTransition( FragmentTransaction.TRANSIT_FRAGMENT_FADE );
        ft.commitAllowingStateLoss();
    }

    /* *****************************************************************
     * *****************************************************************
     * ****************************************************************/

    private class ChooseTshirtFragment extends Fragment implements FirstLaunchFragment{
        Button mLowerButton, mUpperButton;
        ProgressBar mProgressBar;
        TextView mTextView;


        @Override
        public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ){
            super.onCreateView( inflater, container, savedInstanceState );
            View view = inflater.inflate( R.layout.first_launch_fragment, container, false );

            mTextView = ( ( TextView ) view.findViewById( R.id.fl_textview ) );
            mTextView.setText( "You will now choose your Huggi-Shirt" + "" +
                    ".\nPlease, ensure that your Huggi-Shirt is powered and paired before proceeding." );

            mLowerButton = ( Button ) view.findViewById( R.id.fl_lower_button );
            mLowerButton.setText( "Open bluetooth settings" );

            mLowerButton.setOnClickListener( new View.OnClickListener(){
                @Override
                public void onClick( View view ){
                    Intent i = new Intent( Settings.ACTION_BLUETOOTH_SETTINGS );
                    FirstLaunchActivity.this.startActivity( i );
                }
            } );

            mUpperButton = ( Button ) view.findViewById( R.id.fl_upper_button );
            mUpperButton.setText( "Select Huggi-Shirt" );

            view.findViewById( R.id.fl_lower_button );
            mUpperButton.setOnClickListener( new View.OnClickListener(){
                @Override
                public void onClick( View view ){
                    Intent intent = new Intent( FirstLaunchActivity.this, DeviceList.class );
                    startActivityForResult( intent, REQUEST_CONNECT_DEVICE );
                }
            } );

            mProgressBar = ( ProgressBar ) view.findViewById( R.id.fl_progressbar );
            return view;
        }


        @Override
        public void onActivityResult( int requestCode, int resultCode, Intent data ){

            if( requestCode == REQUEST_CONNECT_DEVICE ){

                if( resultCode == Activity.RESULT_OK ){
                    mShirtAddress = data.getExtras().getString( EXTRA_DEVICE_ADDRESS );
                    if( mSPP.isConnected() ){
                        if( mShirtAddress.equals( mSPP.getDeviceAddress() ) ){
                            step3a();
                            return;
                        }
                        mine = true;
                        mSPP.disconnect();
                    }

                    mTextView.setText( "Trying to connect..." );
                    mProgressBar.setVisibility( View.VISIBLE );
                    mUpperButton.setEnabled( false );
                    mSPP.connect( mShirtAddress );

                }else{
                    finish();
                }
            }
        }


        @Override
        public void onFail(){
            mTextView.setText( "Could not connect...\nTry another device ?" );
            mProgressBar.setVisibility( View.INVISIBLE );
            mUpperButton.setEnabled( true );
        }
    }

    /* *****************************************************************
     * *****************************************************************
     * ****************************************************************/

    class ConfirmIDFragment extends Fragment implements FirstLaunchFragment, View.OnClickListener, TextView
            .OnEditorActionListener{

        boolean isIdInput = false, isIdAlreadySet;
        ProgressBar mProgressBar;
        TextView mTextView;
        Button mUpperButton, mLowerButton;
        EditText mEditText;

        private boolean mIsExecuting = false;


        @Override
        public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ){
            super.onCreateView( inflater, container, savedInstanceState );
            final View view = inflater.inflate( R.layout.first_launch_fragment, container, false );
            isIdAlreadySet = !( mHuggerId == null || mHuggerId.isEmpty() );


            mTextView = ( ( TextView ) view.findViewById( R.id.fl_textview ) );
            //            mTextView.setText( String.format(
            //                    "The current ID registered in your Huggi-Shirt is %s.\n Is it " +
            //                    "correct ?", mHuggerId ) );

            mEditText = ( EditText ) view.findViewById( R.id.fl_phone_edittext );
//            mEditText.setImeActionLabel( "Set ID", KeyEvent.KEYCODE_ENTER );

            mEditText.setText( "0" + ( mMyPhoneNumber != null && mMyPhoneNumber.length() > 3 ? //
                    mMyPhoneNumber.substring( 3 ) : "" ) ); // replace +41 by 0
            mEditText.setHint( "0761234567" );
            mEditText.setOnEditorActionListener( this );

            mUpperButton = ( Button ) view.findViewById( R.id.fl_upper_button );
            mUpperButton.setOnClickListener( this );

            mLowerButton = ( Button ) view.findViewById( R.id.fl_lower_button );
            //            mLowerButton.setText( "Nope, change it !" );
            mLowerButton.setOnClickListener( this );

            mProgressBar = ( ProgressBar ) view.findViewById( R.id.fl_progressbar );

            if( !isIdAlreadySet ){
                setEditView();
            }else{
                setConfirmView();
            }

            return view;
        }


        private void setConfirmView(){
            mTextView.setText( String.format( "The current ID registered in your Huggi-Shirt is %s.\n Is it " +
                    "correct ?", mHuggerId ) );
            mUpperButton.setText( "Yep!" );
            mLowerButton.setText( "Nope, change it !" );
            mEditText.setVisibility( View.INVISIBLE );
        }


        private void setEditView(){
            if( !isIdAlreadySet ) mLowerButton.setEnabled( false );
            mTextView.setText( "Please, provide your phone number:" );
            mLowerButton.setText( "Cancel" );
            mUpperButton.setText( "Set ID!");
            mEditText.setVisibility( View.VISIBLE );
            mEditText.requestFocus();
        }


        @Override
        public void onClick( View v ){
            if(v == mLowerButton){
                if( isIdInput ){
                    setConfirmView();
                }else{
                    setEditView();
                }
                isIdInput = !isIdInput;

            }else if(v == mUpperButton){
                if(isIdInput) updateIdCommand();
                else finalStep();
            }

            //            if( isIdInput ){
            //                mLowerButton.setText( "Nope, change it !" );
            //                mUpperButton.setEnabled( true );
            //                mEditText.setVisibility( View.INVISIBLE );
            //            }else{
            //                mLowerButton.setText( "Cancel" );
            //                mUpperButton.setEnabled( false );
            //                mEditText.setVisibility( View.VISIBLE );
            //                mEditText.requestFocus();
            //            }

        }


        @Override
        public boolean onEditorAction( TextView textView, int i, KeyEvent keyEvent ){
            updateIdCommand();
            return true;
        }


        public void updateIdCommand(){
            if(mIsExecuting) return;

            String text = mEditText.getText().toString();
            if( text.length() == 10 ){
                mProgressBar.setVisibility( View.VISIBLE );
                mSPP.executeCommand( CMD_SET_ID, text );  // TODO
                mIsExecuting = true;
            }else{
                Toast.makeText( getActivity(), "Invalid phone number...", Toast.LENGTH_SHORT ).show();
            }
        }


        @Override
        public void onFail(){
            mTextView.setText( "Error setting the ID..." );
            mProgressBar.setVisibility( View.INVISIBLE );
            mIsExecuting = false;
        }
    }

    /* *****************************************************************
     * *****************************************************************
     * ****************************************************************/

}
