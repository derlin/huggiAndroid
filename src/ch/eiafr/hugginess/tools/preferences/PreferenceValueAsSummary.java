package ch.eiafr.hugginess.tools.preferences;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

/**
 * @author: Lucy Linder
 * @date: 30.11.2014
 */

/**
 * This class implements a Preference which automatically displays the preference's current value as its
 * summary.
 *
 * @author Lucy Linder
 *         <p/>
 *         creation date    30.11.2014
 *         context          Projet de semestre Hugginess, EIA-FR, I3 2014-2015
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