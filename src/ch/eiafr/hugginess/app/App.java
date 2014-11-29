package ch.eiafr.hugginess.app;

import android.app.Application;
import android.content.Intent;
import ch.eiafr.hugginess.bluetooth.HuggiBluetoothService;

/**
 * User: lucy
 * Date: 24/11/14
 * Version: 0.1
 */
public class App extends Application {

    public void start() {
        // mContext is defined upper in code, I think it is not necessary to explain what is it
//        Intent i = new Intent( this, BluetoothSPP.class );
        Intent i = new Intent( this, HuggiBluetoothService.class );
        this.startService( i );
    }

    public void stop() {
        this.stopService( new Intent( this, HuggiBluetoothService.class ) );
    }


    @Override
    public void onCreate(){
        super.onCreate();
        start();

    }


    @Override
    public void onTerminate(){
        super.onTerminate();
        stop();
    }
}
