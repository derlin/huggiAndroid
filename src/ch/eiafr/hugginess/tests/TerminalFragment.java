package ch.eiafr.hugginess.tests;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import ch.eiafr.hugginess.HuggiBTActivity;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.bluetooth.HuggiBluetoothService;

import static ch.eiafr.hugginess.bluetooth.BluetoothState.*;

/**
 * @author: Lucy Linder
 * @date: 22.11.2014
 */
public class TerminalFragment extends Fragment{
    private HuggiBluetoothService mSPP;
    private View view;
    private TextView mReceivedText;
    private Button mSendButton;
    private EditText mEditText;

    private StringBuilder text = new StringBuilder(  );

    Menu menu;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive( Context context, Intent intent ){
            switch( intent.getStringExtra( EXTRA_EVT_TYPE ) ){
                case EVT_DATA_RECEIVED:
                    String newline = intent.getStringExtra( EVT_EXTRA_DATA ) + "\n";
                    text.append( newline );
                    if(mReceivedText != null) mReceivedText.append( newline );
                    //if(mSPP != null) mReceivedText.setText( mSPP.getLastReceivedData() );
                    break;

                case EVT_CONNECTED:
                    if(mSendButton != null) mSendButton.setEnabled( true );
                    mSPP.executeCommand( CMD_SEND_HUGS ); // auto fetch
                    break;

                case EVT_DISCONNECTED:
                    if(mSendButton != null) mSendButton.setEnabled( false );
                    break;
            }
        }
    };

    @Override
    public void onCreate( Bundle savedInstanceState ){
        Log.d( "lala", "on create" );
        LocalBroadcastManager.getInstance( getActivity() ).registerReceiver( mBroadcastReceiver, new
                IntentFilter( BTSERVICE_INTENT_FILTER ) );

        super.onCreate( savedInstanceState );


    }


    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ){
        Log.d( "lala", "on create view" );
        view = inflater.inflate( R.layout.terminal, container, false );

        Log.i( "Check", "onCreateView" );

        //        mTextStatus = ( TextView ) view.findViewById(R.id.textStatus);
        mEditText = ( EditText ) view.findViewById( R.id.etMessage );
        mReceivedText = ( TextView ) view.findViewById( R.id.textRead );
        mReceivedText.setMovementMethod( new ScrollingMovementMethod() );


        registerForContextMenu( mReceivedText );

        mSPP = ( ( HuggiBTActivity ) getActivity() ).getHuggiService();

        mReceivedText.setText( text.toString() );
//        if(mSPP != null)
//        mReceivedText.setText( mSPP.getLastReceivedData() );

        mSendButton = ( Button ) view.findViewById( R.id.btnSend );
        mSendButton.setEnabled( mSPP != null && mSPP.isConnected() );
        mSendButton.setOnClickListener( new View.OnClickListener(){
            public void onClick( View v ){
                if( mEditText.getText().length() != 0 ){
                    mSPP.send( mEditText.getText().toString(), true );
                }
            }
        } );


        return view;
    }


    @Override
    public void onPause(){
        Log.d( "lala", "on pause" );
        super.onPause();
    }


    @Override
    public void onDestroyView(){
        Log.d( "lala", "on destroy view" );
        super.onDestroyView();
    }


    @Override
    public void onDestroy(){
        LocalBroadcastManager.getInstance( getActivity() ).unregisterReceiver( mBroadcastReceiver );
        Log.d( "lala", "ondestroy" );
        super.onDestroy();
    }


    /* *****************************************************************
     * context menu
     * ****************************************************************/

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle("Options");
        menu.add(0, v.getId(), 0, "Clear");
    }
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getTitle() == "Clear") {
            mReceivedText.setText( "" );
            return true;
        }
        return true;
    }
 }//end class
