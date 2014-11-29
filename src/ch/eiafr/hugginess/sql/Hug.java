package ch.eiafr.hugginess.sql;

import java.io.Serializable;
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


    public void setDuration( int duration ){
        this.duration = duration;
    }


    public Date getDate(){
        return date;
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


    public static Hug parseHug( String s ){

        String[] split = s.split( "!" );
        if(!split[0].equals("@H") || split.length < 4 ){
            return null;
        }

        Hug hug = new Hug();
        hug.setHuggerID( split[1] ); // 0 is @H
        hug.setData( split[2] );
        hug.setDuration( Integer.parseInt( split[3]) );
        hug.setDate( new Date() );

        return hug;
    }
}//end class
