package ch.eiafr.hugginess.gui.prefs;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import ch.eiafr.hugginess.gui.prefs.frag.PrefsFragment;

import java.util.List;

/**
 * @author: Lucy Linder
 * @date: 30.11.2014
 */
public class PrefsActivity extends PreferenceActivity{


    //-------------------------------------------------------------


    @Override
    protected void onNewIntent( Intent intent ){
        // overriding this method fixes the bugs related to
        // configuration change. The activity is no longer restarted !
        super.onNewIntent( intent );
        setIntent( intent );
    }


    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    // ----------------------------------------------------


    @Override
    protected void onCreate( Bundle savedInstanceState ){
        super.onCreate( savedInstanceState );

        // allow to go back to main activity on home pressed
        getActionBar().setDisplayHomeAsUpEnabled( true );

        // directly display the fragment as the main content (skip headers)
        getFragmentManager().beginTransaction().replace( android.R.id.content, new PrefsFragment() ).commit();
    }


    @Override
    public void onBuildHeaders( List<PreferenceActivity.Header> target ){
        //loadHeadersFromResource( R.xml.activity_prefs_header, target );
    }


    @Override
    public boolean onOptionsItemSelected( MenuItem item ){
        switch( item.getItemId() ){
            case android.R.id.home:{
                onBackPressed();
                return true;
            }
            default:
                return super.onOptionsItemSelected( item );
        }
    }


    //-------------------------------------------------------------
    @Override
    protected boolean isValidFragment( String fragmentName ){
        return "ch.eiafr.hugginess.gui.prefs.frag.PrefsFragment".equals( fragmentName );
    }


}