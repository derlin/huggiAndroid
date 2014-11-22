package ch.eiafr.hugginess.app;

import android.app.Activity;
import android.os.Bundle;
import ch.eiafr.hugginess.MySPP.R;

public class Main extends Activity{
    /** Called when the activity is first created. */
    @Override
    public void onCreate( Bundle savedInstanceState ){
        super.onCreate( savedInstanceState );
        setContentView( R.layout.main );
    }
}
