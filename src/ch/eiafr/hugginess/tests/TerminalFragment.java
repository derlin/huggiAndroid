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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.SPPActivity;
import ch.eiafr.hugginess.myspp.BluetoothService;

import static ch.eiafr.hugginess.myspp.BluetoothState.*;

/**
 * @author: Lucy Linder
 * @date: 22.11.2014
 */
public class TerminalFragment extends Fragment{
    private BluetoothService mSPP;
    private View view;
    private TextView mReceivedText;
    private Button mSendButton;
    private EditText mEditText;
    private BroadcastReceiver mBroadcastReceiver;

    Menu menu;


    @Override
    public void onCreate( Bundle savedInstanceState ){
        Log.d( "lala", "on create" );
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


        mSPP = ( ( SPPActivity ) getActivity() ).getSPP();


        mBroadcastReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive( Context context, Intent intent ){
                switch( intent.getStringExtra( EXTRA_EVT_TYPE ) ){
                    case EVT_DATA_RECEIVED:
                        mReceivedText.append( intent.getStringExtra( EVT_EXTRA_DATA ) + "\n" );
                        break;

                    case EVT_CONNECTED:
                        mSendButton.setEnabled( true );
                        break;

                    case EVT_DISCONNECTED:
                        mSendButton.setEnabled( false );
                        break;
                }
            }
        };

        LocalBroadcastManager.getInstance( getActivity() ).registerReceiver( mBroadcastReceiver, new IntentFilter(
                BTSERVICE_INTENT_FILTER ) );

        mSendButton = ( Button ) view.findViewById( R.id.btnSend );
        mSendButton.setEnabled( mSPP.isConnected() );
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
        LocalBroadcastManager.getInstance( getActivity() ).unregisterReceiver( mBroadcastReceiver );
        Log.d( "lala", "on destroy view" );
        super.onDestroyView();
    }


    @Override
    public void onDestroy(){
        Log.d( "lala", "ondestroy" );
        super.onDestroy();
    }
}//end class
