package ch.eiafr.hugginess.tools.preferences;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

/**
 * @author: Lucy Linder
 * @date: 30.11.2014
 */
public class PreferenceValueAsSummary extends Preference{


    public PreferenceValueAsSummary( Context context, AttributeSet attrs ){
        super( context, attrs );
        init();
    }


    public PreferenceValueAsSummary( Context context ){
        super( context );
        init();
    }


    private void init(){
        setOnPreferenceChangeListener( new OnPreferenceChangeListener(){

            @Override
            public boolean onPreferenceChange( Preference oldValue, Object newValue ){
                oldValue.setSummary( newValue.toString() );
                return true;
            }
        } );
    }


    @Override
    public CharSequence getSummary(){
        return getPersistedString( "" );
    }
}