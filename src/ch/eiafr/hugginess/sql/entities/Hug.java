package ch.eiafr.hugginess.sql.entities;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author: Lucy Linder
 * @date: 26.11.2014
 */
public class Hug implements Serializable{
    private int id;
    private String huggerID;
    private int duration;
    private Date date;
    private String data;
    public static final SimpleDateFormat PRETTY_DATE_FORMAT = new SimpleDateFormat("dd.MM.yy HH:mm");


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
        double seconds = duration * 0.001;
        if(seconds < 60) return String.format( "%.2f s", seconds );
        int minutes = ( int ) (seconds / 60);
        int secs = ( int ) (seconds % 60);
        return String.format( "%d.%d min", minutes, secs );
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


    // ----------------------------------------------------


    @Override
    public boolean equals( Object o ){
        if( this == o ) return true;
        if( o == null || this.getClass() != o.getClass() ) return false;

        Hug hug = ( Hug ) o;

        if( duration != hug.duration ) return false;
        if( id != hug.id ) return false;
        if( !date.equals( hug.date ) ) return false;
        if( !huggerID.equals( hug.huggerID ) ) return false;

        return true;
    }


    @Override
    public int hashCode(){
        int result = id;
        result = 31 * result + huggerID.hashCode();
        result = 31 * result + duration;
        result = 31 * result + date.hashCode();
        return result;
    }


    // ----------------------------------------------------


    public static Hug parseHug( String s ){

        String[] split = s.split( "!" );
        if( !split[ 0 ].equals( "@H" ) || split.length < 4 ){
            return null;
        }

        Hug hug = new Hug();
        hug.setHuggerID( split[ 1 ] ); // 0 is @H
        hug.setData( split[ 2 ] );
        hug.setDuration( Integer.parseInt( split[ 3 ] ) );
        hug.setDate( new Date() );

        return hug;
    }
}//end class
