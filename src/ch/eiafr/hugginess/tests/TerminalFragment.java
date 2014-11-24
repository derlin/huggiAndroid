package ch.eiafr.hugginess.tests;

import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import ch.eiafr.hugginess.myspp.BluetoothListener;
import ch.eiafr.hugginess.myspp.BluetoothSPP;
import ch.eiafr.hugginess.myspp.BluetoothState;

import static ch.eiafr.hugginess.myspp.BluetoothListener.OnDataReceivedListener;

/**
 * @author: Lucy Linder
 * @date: 22.11.2014
 */
public class TerminalFragment extends Fragment{
    private BluetoothSPP mSPP;
    private View view;
    private TextView mReceivedText;
    private Button mSendButton;
    private EditText mEditText;

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

        mSPP.setOnDataReceivedListener( new OnDataReceivedListener(){
            public void onDataReceived( String message ){
                mReceivedText.append( message + "\n" );
            }
        } );

        mSendButton = ( Button ) view.findViewById( R.id.btnSend );
        mSendButton.setEnabled( mSPP.getServiceState() == BluetoothState.STATE_CONNECTED );
        mSendButton.setOnClickListener( new View.OnClickListener(){
            public void onClick( View v ){
                if( mEditText.getText().length() != 0 ){
                    mSPP.send( mEditText.getText().toString(), true );
                }
            }
        } );

        mSPP.setBluetoothConnectionListener( new BluetoothListener.ConnectionListenerAdapter(){
            @Override
            public void onDeviceConnected( String name, String address ){
                mSendButton.setEnabled( true );
            }

            @Override
            public void onDeviceDisconnected(){
                mSendButton.setEnabled( false );
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
        Log.d( "lala", "ondestroy" );
        super.onDestroy();
    }
}//end class
