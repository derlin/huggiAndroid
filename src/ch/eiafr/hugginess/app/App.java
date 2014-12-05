package ch.eiafr.hugginess.app;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import ch.eiafr.hugginess.services.bluetooth.HuggiBluetoothService;

/**
 * User: lucy
 * Date: 24/11/14
 * Version: 0.1
 */
public class App extends Application {

    static Context appContext;


    public static Context getAppContext(){
        return appContext;
    }

    //-------------------------------------------------------------

    @Override
    public void onCreate(){
        super.onCreate();

        appContext = this.getApplicationContext();
        Intent i = new Intent( this, HuggiBluetoothService.class );
        this.startService( i );

    }


    @Override
    public void onTerminate(){
        this.stopService( new Intent( this, HuggiBluetoothService.class ) );
        super.onTerminate();
    }
}
