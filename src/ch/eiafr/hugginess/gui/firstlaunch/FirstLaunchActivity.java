package ch.eiafr.hugginess.gui.firstlaunch;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.gui.bt.DeviceListActivity;
import ch.eiafr.hugginess.gui.main.MainActivity;
import ch.eiafr.hugginess.services.bluetooth.HuggiBluetoothService;
import ch.eiafr.hugginess.services.bluetooth.HuggiBroadcastReceiver;

import static ch.eiafr.hugginess.services.bluetooth.BluetoothConstants.*;

/**
 * @author: Lucy Linder
 * @date: 01.12.2014
 */
public class FirstLaunchActivity extends FragmentActivity {


    //-------------------------------------------------------------
    // the bluetooth events are handled from the activity.
    // --> use this method to notify the current fragment when a
    // nak is received
    private interface FirstLaunchFragment {
        void onFail();
    }
    //-------------------------------------------------------------

    private HuggiBluetoothService mSPP;

    private FirstLaunchFragment mCurrentFragment;
    private String mHuggerId;
    private String mShirtAddress;

    private String mMyPhoneNumber;
    private boolean isMyDisconnectRequest = false;
    private boolean mIsAckFinishingActivity = false;
    //-------------------------------------------------------------

    private HuggiBroadcastReceiver mBroadcastReceiver = new HuggiBroadcastReceiver() {
        private int mFailedCount = 0;


        @Override
        public void onBtConnected(){
            step3aGetCurrentShirtConfig();
        }


        @Override
        public void onBtDisonnected(){
            if( !isMyDisconnectRequest ){
                Toast.makeText( FirstLaunchActivity.this, "Disconnected", Toast.LENGTH_SHORT ).show();
                finish(); // TODO
            }else{
                isMyDisconnectRequest = false;
            }
        }


        @Override
        public void onBtConnectionFailed(){
            mFailedCount++;
            if( mFailedCount < 3 ){
                mSPP.connect( mShirtAddress );
                Toast.makeText( FirstLaunchActivity.this, "Failed to connect, retrying", Toast.LENGTH_SHORT
                ).show();
            }else{
                // TODO: dialog
                //                        step1CheckBluetoothEnabled();
                mFailedCount = 0;
                mCurrentFragment.onFail();
            }
        }


        @Override
        public void onBtDataReceived( String line ){
            if( line.startsWith( DATA_PREFIX + CMD_DUMP_ALL ) ){
                String[] split = line.split( DATA_SEP );
                if( split.length == 3 ){ // TODO
                    // split[0] is @A
                    step3bCheckCurrentShirtConfig( split[ 1 ], split[ 2 ] );
                }else{
                    step3bCheckCurrentShirtConfig( "", "" );
                }
            }
        }


        @Override
        public void onBtAckReceived( char cmd, boolean ok ){
            if( ok ){
                if(mIsAckFinishingActivity) finalStep();
            }else{
                mCurrentFragment.onFail();
            }

        }
    };


    // ----------------------------------------------------


    @Override
    public void onCreate( Bundle savedInstanceState ){
        super.onCreate( savedInstanceState );
        mBroadcastReceiver.registerSelf( this );
        setContentView( R.layout.activity_firstlaunch );

        // wait until the bluetooth service has been started
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground( Void... params ){
                while( HuggiBluetoothService.getInstance() == null ){
                    try{
                        Thread.sleep( 200 );
                    }catch( InterruptedException e ){
                        e.printStackTrace();
                    }
                }

                mSPP = HuggiBluetoothService.getInstance();
                return null;
            }

            @Override
            protected void onPostExecute( Void aVoid ){
                step1CheckBluetoothEnabled();
            }

        }.execute();


    }

    // ----------------------------------------------------

    @Override
    protected void onNewIntent( Intent intent ){
        // overriding this method fixes the bugs related to
        // configuration change. The activity is no longer restarted !
        super.onNewIntent( intent );
        setIntent( intent );
    }

    @Override
    public void onDestroy(){
        mBroadcastReceiver.unregisterSelf( this );
        super.onDestroy();
    }


    /* *****************************************************************
     * onActivityResult
     * ****************************************************************/


    @Override
    public void onActivityResult( int requestCode, int resultCode, Intent data ){

        if( requestCode == REQUEST_ENABLE_BT ){
            if( resultCode == Activity.RESULT_OK ){
                step2ChooseShirt();
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


    private void step1CheckBluetoothEnabled(){
        if( !mSPP.isBluetoothEnabled() ){
            Intent btIntent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
            startActivityForResult( btIntent, REQUEST_ENABLE_BT );
        }else{
            step2ChooseShirt();
        }
    }


    /* *****************************************************************
     * Choose device
     * ****************************************************************/


    private void step2ChooseShirt(){
        switchFragments( new ChooseTshirtFragment() );
    }


    /* *****************************************************************
     * confirm/change ID
     * ****************************************************************/


    private void step3aGetCurrentShirtConfig(){
        mSPP.executeCommand( CMD_DUMP_ALL );
    }


    private void step3bCheckCurrentShirtConfig( String id, String data ){
        mHuggerId = id;

        mMyPhoneNumber = getMyPhoneNumber();
        boolean compare = PhoneNumberUtils.compare( mMyPhoneNumber, id );
        if( compare ){
            finalStep();
        }else{
            switchFragments( new ConfirmIDFragment() );
        }


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

        Intent intent = new Intent( this, MainActivity.class );
        //        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags( Intent.FLAG_ACTIVITY_NO_HISTORY );
        startActivity( intent );
        finish();
    }

    //-------------------------------------------------------------

    private String getMyPhoneNumber(){
        TelephonyManager mTelephonyMgr;
        mTelephonyMgr = ( TelephonyManager ) getSystemService( Context.TELEPHONY_SERVICE );
        return mTelephonyMgr.getLine1Number();
    }


    private void switchFragments( Fragment f ){
        // Execute a transaction, replacing any existing fragment
        // with this one inside the frame.
        mCurrentFragment = ( FirstLaunchFragment ) f;
        f.setRetainInstance( true );
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace( R.id.first_launch_fragment_layout, f );
        ft.setTransition( FragmentTransaction.TRANSIT_FRAGMENT_FADE );
        ft.commitAllowingStateLoss();
    }

    /* *****************************************************************
     * *****************************************************************
     * ****************************************************************/

    class ChooseTshirtFragment extends Fragment implements FirstLaunchFragment {
        Button mLowerButton, mUpperButton;
        ProgressBar mProgressBar;
        TextView mTextView;


        @Override
        public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ){
            super.onCreateView( inflater, container, savedInstanceState );
            View view = inflater.inflate( R.layout.activity_firstlaunch_frag_manageshirt, container, false );

            mTextView = ( ( TextView ) view.findViewById( R.id.fl_textview ) );
            mTextView.setText( "You will now choose your Huggi-Shirt" + "" +
                    ".\nPlease, ensure that your Huggi-Shirt is powered and paired before proceeding." );

            mLowerButton = ( Button ) view.findViewById( R.id.fl_lower_button );
            mLowerButton.setText( "Open bluetooth settings" );

            mLowerButton.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick( View view ){
                    Intent i = new Intent( Settings.ACTION_BLUETOOTH_SETTINGS );
                    FirstLaunchActivity.this.startActivity( i );
                }
            } );

            mUpperButton = ( Button ) view.findViewById( R.id.fl_upper_button );
            mUpperButton.setText( "Select Huggi-Shirt" );

            view.findViewById( R.id.fl_lower_button );
            mUpperButton.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick( View view ){
                    Intent intent = new Intent( FirstLaunchActivity.this, DeviceListActivity.class );
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
                            step3aGetCurrentShirtConfig();
                            return;
                        }
                        isMyDisconnectRequest = true;
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
            mProgressBar.setVisibility( View.GONE );
            mUpperButton.setEnabled( true );
        }
    }

    /* *****************************************************************
     * *****************************************************************
     * ****************************************************************/

    class ConfirmIDFragment extends Fragment implements FirstLaunchFragment, View.OnClickListener, TextView
            .OnEditorActionListener {

        boolean isIdInput = false, isIdAlreadySet;
        ProgressBar mProgressBar;
        TextView mTextView;
        Button mUpperButton, mLowerButton;
        EditText mEditText;

        private boolean mIsExecuting = false;


        @Override
        public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ){
            super.onCreateView( inflater, container, savedInstanceState );
            final View view = inflater.inflate( R.layout.activity_firstlaunch_frag_manageshirt, container,
                    false );
            isIdAlreadySet = !( mHuggerId == null || mHuggerId.isEmpty() );


            mTextView = ( ( TextView ) view.findViewById( R.id.fl_textview ) );

            mEditText = ( EditText ) view.findViewById( R.id.fl_phone_edittext );
            mEditText.setText( "0" + ( mMyPhoneNumber != null && mMyPhoneNumber.length() > 3 ? //
                    mMyPhoneNumber.substring( 3 ) : "" ) ); // replace +41 by 0
            mEditText.setHint( "0761234567" );
            mEditText.setOnEditorActionListener( this );

            mUpperButton = ( Button ) view.findViewById( R.id.fl_upper_button );
            mUpperButton.setOnClickListener( this );

            mLowerButton = ( Button ) view.findViewById( R.id.fl_lower_button );
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
            mTextView.setText( String.format( "The current ID registered in your Huggi-Shirt is %s.\n Is it" +
                    " " + "correct ?", mHuggerId ) );
            mUpperButton.setText( "Yep!" );
            mLowerButton.setText( "Nope, change it !" );
            mEditText.setVisibility( View.GONE );
        }


        private void setEditView(){
            if( !isIdAlreadySet ) mLowerButton.setEnabled( false );
            mTextView.setText( "Please, provide your phone number:" );
            mLowerButton.setText( "Cancel" );
            mUpperButton.setText( "Set ID!" );
            mEditText.setVisibility( View.VISIBLE );
            mEditText.requestFocus();
        }


        @Override
        public void onClick( View v ){
            if( v == mLowerButton ){
                if( isIdInput ){
                    setConfirmView();
                }else{
                    setEditView();
                }
                isIdInput = !isIdInput;

            }else if( v == mUpperButton ){
                if( isIdInput ){
                    updateIdCommand();
                }else{
                    finalStep();
                }
            }
        }


        @Override
        public boolean onEditorAction( TextView textView, int i, KeyEvent keyEvent ){
            updateIdCommand();
            return true;
        }


        public void updateIdCommand(){
            if( mIsExecuting ) return;

            String text = mEditText.getText().toString();
            if( text.length() == ID_SIZE ){
                mProgressBar.setVisibility( View.VISIBLE );
                mSPP.executeCommand( CMD_SET_ID, text );  // TODO
                mIsAckFinishingActivity = true;
                mIsExecuting = true;
            }else{
                Toast.makeText( getActivity(), "Invalid phone number...", Toast.LENGTH_SHORT ).show();
            }
        }


        @Override
        public void onFail(){
            mTextView.setText( "Error setting the ID..." );
            mProgressBar.setVisibility( View.GONE );
            mIsExecuting = false;
        }
    }

}
