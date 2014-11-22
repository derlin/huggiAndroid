package ch.eiafr.hugginess.tests;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.myspp.BluetoothSPP;
import ch.eiafr.hugginess.myspp.BluetoothState;

import static ch.eiafr.hugginess.myspp.BluetoothSPP.BluetoothConnectionListener;
import static ch.eiafr.hugginess.myspp.BluetoothSPP.OnDataReceivedListener;

/**
 * @author: Lucy Linder
 * @date: 22.11.2014
 */
public class TerminalFragment extends Fragment{
    BluetoothSPP bt;
    View view;
    TextView textStatus, textRead;
    EditText etMessage;

    Menu menu;


    @Override
    public void onCreate( Bundle savedInstanceState ){
        Log.d( "prout", "on create" );
        super.onCreate( savedInstanceState );
    }


    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ){
        Log.d( "prout", "on create view" );
        view = inflater.inflate( R.layout.terminal, container, false );

        Log.i( "Check", "onCreateView" );

        textRead = (TextView ) view.findViewById(R.id.textRead);
        textStatus = ( TextView ) view.findViewById(R.id.textStatus);
        etMessage = ( EditText ) view.findViewById(R.id.etMessage);

        bt = ((TabTestActivity)getActivity()).bt; //new BluetoothSPP(getActivity()); TODO

        if(bt.getServiceState() == BluetoothState.STATE_CONNECTED) {
            setup();
            textStatus.setText("Status : Connected to " + bt.getConnectedDeviceName());
        }

        bt.setOnDataReceivedListener( new OnDataReceivedListener(){
            public void onDataReceived( String message ){
                textRead.append( message + "\n" );
            }
        } );

        bt.setBluetoothConnectionListener(new BluetoothConnectionListener() {
            public void onDeviceDisconnected() {
                textStatus.setText("Status : Not connected");
            }

            public void onDeviceConnectionFailed() {
                textStatus.setText("Status : Connection failed");
            }

            public void onDeviceConnected(String name, String address) {
                textStatus.setText( "Status : Connected to " + name );
                setup();
            }
        });

        return view;
    }



    public void setup() {
        Button btnSend = ( Button ) view.findViewById(R.id.btnSend);
        btnSend.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                if(etMessage.getText().length() != 0) {
                    bt.send(etMessage.getText().toString(), true);
                    etMessage.setText("");
                }
            }
        });
    }


    @Override
    public void onPause(){
        Log.d( "prout", "on pause" );
        super.onPause();
    }


    @Override
    public void onDestroyView(){
        Log.d( "prout", "on destroy view" );
        super.onDestroyView();
    }


    @Override
    public void onDestroy(){
        Log.d( "prout", "ondestroy" );
        super.onDestroy();
    }
}//end class
