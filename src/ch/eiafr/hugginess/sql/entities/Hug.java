package ch.eiafr.hugginess.sql.entities;

import android.util.Log;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class is a DAO entity for a Hug.
 * In addition to the standard properties, it also provide some static utilities to work with hugs (@see #parseHug and
 * #formatDuration).
 * <p/>
 * creation date    26.11.2014
 * context          Projet de semestre Hugginess, EIA-FR, I3 2014-2015
 *
 * @author Lucy Linder
 */
public class Hug implements Serializable{

    private static final SimpleDateFormat PRETTY_DATE_FORMAT = new SimpleDateFormat( "dd.MM.yy HH:mm" );

    private int id;
    private String huggerID;
    private int duration;
    private Date date;
    private String data;


    // ---------------------------------------- Getter and setters


    public int getId(){
        return id;
    }


    public void setId( int id ){
        this.id = id;
    }


    public String getHuggerID(){
        return huggerID;
    }


    public void setHuggerID( String huggerID ){
        this.huggerID = huggerID;
    }


    public int getDuration(){
        return duration;
    }


    public String getStringDuration(){
        return formatDuration( duration );
    }


    public void setDuration( int duration ){
        this.duration = duration;
    }


    public Date getDate(){
        return date;
    }


    public String getStringDate(){
        return PRETTY_DATE_FORMAT.format( date );
    }


    public void setDate( Date date ){
        this.date = date;
    }


    public String getData(){
        return data;
    }


    public void setData( String data ){
        this.data = data;
    }


    // ------------------------------------- overrides


    @Override
    public boolean equals( Object o ){
        if( this == o ) return true;
        if( o == null || this.getClass() != o.getClass() ) return false;

        Hug hug = ( Hug ) o;

        return duration == hug.duration && //
                id == hug.id && //
                huggerID == hug.huggerID && //
                date.equals( hug.date );

    }


    @Override
    public int hashCode(){
        int result = id;
        result = 31 * result + huggerID.hashCode();
        result = 31 * result + duration;
        result = 31 * result + date.hashCode();
        return result;
    }


    /* *****************************************************************
     * Static utils
     * ****************************************************************/


    /**
     * Extract a hug from a string in the format:
     * <pre>    @H!hugger_id!data!duration</pre>.
     * The date will be set to {@link Date#Date}.
     * <p/>
     * @param s the string
     * @return the extracted hug, or null if the string does not match the format.
     */
    public static Hug parseHug( String s ){

        String[] split = s.split( "!" );
        if( !split[ 0 ].equals( "@H" ) || split.length < 4 ){
            return null;
        }

        try{
            Hug hug = new Hug();
            hug.setHuggerID( split[ 1 ] ); // 0 is @H
            hug.setData( split[ 2 ] );
            hug.setDuration( Integer.parseInt( split[ 3 ] ) );
            hug.setDate( new Date() );

            return hug;

        }catch( Exception e ){
            Log.d( "Hugginess: Hug.java", "Could not parse hug, invalid format " + e );
            return null;
        }
    }


    /**
     * Helper to work with duration field.
     *
     * @param duration a duration in ms.
     * @return a string properly formatted. The duration is expressed in seconds or minutes, depending on the actual
     * value.
     * <p/>
     * Example:
     * <p></p><pre><blockquote>
     *      formatDuration(1400); // will return "1.40 s"
     *      formatDuration(1400); // will return "1 min 40 s"
     * </blockquote></pre></p>
     */
    public static String formatDuration( int duration ){
        double seconds = duration * 0.001;
        if( seconds < 60 ) return String.format( "%.2f s", seconds );
        int minutes = ( int ) ( seconds / 60 );
        int secs = ( int ) ( seconds % 60 );
        return String.format( "%d min %d s", minutes, secs );
    }

}//end class
