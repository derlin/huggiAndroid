package ch.eiafr.hugginess.tools;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

/**
 * @author: Lucy Linder
 * @date: 30.11.2014
 */
public class PreferenceValueAsSummary extends Preference{

    private final static String TAG = PreferenceValueAsSummary.class.getName();


    public PreferenceValueAsSummary( Context context, AttributeSet attrs ) {
        super(context, attrs);
        init();
    }

    public PreferenceValueAsSummary( Context context ) {
        super(context);
        init();
    }

    private void init() {
        setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference arg0, Object arg1) {
                arg0.setSummary("\nCurrent: " + arg1.toString());
                return true;
            }
        });
    }

    @Override
    public CharSequence getSummary() {
        return "\nCurrent: " + getPersistedString( "" );
    }
}