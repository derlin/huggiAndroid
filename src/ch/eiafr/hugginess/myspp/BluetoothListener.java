package ch.eiafr.hugginess.myspp;

/**
 * @author: Lucy Linder
 * @date: 23.11.2014
 */
public class BluetoothListener{
    public interface OnStateChangedListener{
        public void onServiceStateChanged( int state );
    }

    public interface OnDataReceivedListener{
        public void onDataReceived( String message );
    }

    public interface ConnectionListener{
        public void onDeviceConnected( String name, String address );

        public void onDeviceDisconnected();

        public void onDeviceConnectionFailed();
    }

    public interface AutoConnectionListener{
        public void onAutoConnectionStarted();

        public void onNewConnection( String name, String address );
    }


    public static class ConnectionListenerAdapter implements ConnectionListener{

        @Override
        public void onDeviceConnected( String name, String address ){
        }


        @Override
        public void onDeviceDisconnected(){
        }


        @Override
        public void onDeviceConnectionFailed(){
        }
    }
}//end class
