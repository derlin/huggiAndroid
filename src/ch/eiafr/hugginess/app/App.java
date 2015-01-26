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

/**
 * This class is the Hugginess Application.
 * Its main role is to start/stop the bluetooth service and to make a
 * context available through a static method (useful when
 * you need a context in a regular class).
 * <p/>
 * creation date    24.11.2014
 * context          Projet de semestre Hugginess, EIA-FR, I3 2014-2015
 *
 * @author Lucy Linder
 */
public class App extends Application{

    static Context sAppContext;


    /** @return the application context * */
    public static Context getAppContext(){
        return sAppContext;
    }

    //-------------------------------------------------------------


    @Override
    public void onCreate(){
        super.onCreate();

        sAppContext = this.getApplicationContext();
        // start the bluetooth service
        Intent i = new Intent( this, HuggiBluetoothService.class );
        this.startService( i );

    }


    @Override
    public void onTerminate(){
        // stop the bluetooth service
        this.stopService( new Intent( this, HuggiBluetoothService.class ) );
        super.onTerminate();
    }
}
