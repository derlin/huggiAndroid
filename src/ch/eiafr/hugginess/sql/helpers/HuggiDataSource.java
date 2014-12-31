package ch.eiafr.hugginess.sql.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;
import ch.eiafr.hugginess.sql.entities.Hug;
import ch.eiafr.hugginess.sql.entities.Hugger;

import java.sql.SQLException;
import java.util.*;

import static ch.eiafr.hugginess.sql.helpers.SqlHelper.HG_COL_ID_REF;
import static ch.eiafr.hugginess.sql.helpers.SqlHelper.HUGS_TABLE;

/**
 * User: lucy
 * Date: 12/12/14
 * Version: 0.1
 */
public class HuggiDataSource implements AutoCloseable{

    private SQLiteDatabase db;
    private SqlHelper helper;

    private static final String[] HUGGERS_ALL_COLUMNS = new String[]{ SqlHelper.HR_COL_ID };

    private static final String[] HUGS_ALL_COLUMNS = new String[]{ SqlHelper.HG_COL_ID,        //
            SqlHelper.HG_COL_ID_REF,    //
            SqlHelper.HG_COL_DATE,      //
            SqlHelper.HG_COL_DUR,       //
            SqlHelper.HG_COL_DATA };


    // ----------------------------------------------------


    public HuggiDataSource( Context context ){
        helper = new SqlHelper( context );
    }


    public HuggiDataSource( Context context, boolean autoOpen ) throws SQLException{
        this( context );
        if( autoOpen ) this.open();
    }


    public HuggiDataSource open() throws SQLException{
        db = helper.getWritableDatabase();
        return this;
    }


    public void close(){
        helper.close();
    }


    /* *****************************************************************
     * All
     * ****************************************************************/


    public int clearAllData(){
        int count = 0;
        count += db.delete( SqlHelper.HUGS_TABLE, null, null );
        count += db.delete( SqlHelper.HUGGERS_TABLE, null, null );
        return count;
    }

    /* *****************************************************************
     * Hugs
     * ****************************************************************/


    public boolean deleteHug( String id ){
        return db.delete( SqlHelper.HUGS_TABLE, SqlHelper.HG_COL_ID + " = " + id, null ) > 0;
    }


    public boolean addHug( Hug hug ){
        if( !huggerExists( hug.getHuggerID() ) ){
            Hugger hugger = new Hugger();
            hugger.setId( hug.getHuggerID() );
            addHugger( hugger );
        }
        return db.insert( SqlHelper.HUGS_TABLE, null, hugToContentValues( hug ) ) > 0;
    }


    public List<Hug> getHugs(){
        List<Hug> hugs = new ArrayList<>();

        // order by epoch, then duration
        Cursor cursor = db.query( SqlHelper.HUGS_TABLE, null, null, null, null, null, //
                String.format( "%s DESC, %s DESC", SqlHelper.HG_COL_DATE, SqlHelper.HG_COL_DUR ) );
        cursor.moveToFirst();

        while( !cursor.isAfterLast() ){
            hugs.add( cursorToHug( cursor ) );
            cursor.moveToNext();
        }//end while
        cursor.close();

        return hugs;
    }


    public int getHugsCount(){
        return ( int ) DatabaseUtils.queryNumEntries( db, SqlHelper.HUGS_TABLE );
    }

    //-------------------------------------------------------------


    private ContentValues hugToContentValues( Hug hug ){
        ContentValues values = new ContentValues();
        //values.put( HG_COL_ID, null );
        values.put( SqlHelper.HG_COL_ID_REF, hug.getHuggerID() );
        values.put( SqlHelper.HG_COL_DUR, hug.getDuration() );
        values.put( SqlHelper.HG_COL_DATE, hug.getDate().getTime() );
        values.put( SqlHelper.HG_COL_DATA, hug.getData() );
        return values;
    }


    private static Hug cursorToHug( Cursor cursor ){
        Hug hug = new Hug();
        int i = 0;

        hug.setId( cursor.getInt( i++ ) );
        hug.setHuggerID( cursor.getString( i++ ) );
        hug.setDate( new Date( cursor.getLong( i++ ) ) );
        hug.setDuration( cursor.getInt( i++ ) );
        hug.setData( cursor.getString( i++ ) );
        return hug;
    }

    /* *****************************************************************
     * Huggers
     * ****************************************************************/


    public boolean deleteHugger( String id ){
        db.delete( SqlHelper.HUGS_TABLE, SqlHelper.HG_COL_ID_REF + " = " + id, null );
        return db.delete( SqlHelper.HUGGERS_TABLE, SqlHelper.HR_COL_ID + " = " + id, null ) > 0;
    }


    public boolean addHugger( Hugger hugger ){
        return db.insert( SqlHelper.HUGGERS_TABLE, null, huggerToContentValues( hugger ) ) > 0;
    }


    public Hugger getHugger( String huggerId ){
        Cursor cursor = findHugger( huggerId );
        cursor.moveToFirst();
        Hugger hugger = cursorToHugger( cursor );
        cursor.close();
        return hugger;
    }


    public List<Pair<Hugger, Integer>> getTopXHuggers( int nbr ){
        List<Pair<Hugger, Integer>> list = new ArrayList<>();

        Cursor cursor = db.rawQuery( //
                String.format( "select %s, count(*) from %s group by %s order by 2 DESC limit " + nbr, //
                        HG_COL_ID_REF, HUGS_TABLE, HG_COL_ID_REF ), //
                null );

        cursor.moveToFirst();

        while( !cursor.isAfterLast() ){
            Pair<Hugger, Integer> pair = new Pair<>( getHugger( cursor.getString( 0 ) ), cursor.getInt( 1 ) );
            list.add( pair );
            cursor.moveToNext();
        }

        cursor.close();

        return list;
    }


    public Map<String, Hugger> getHuggersMap(){
        Map<String, Hugger> huggers = new TreeMap<>();

        Cursor cursor = db.query( SqlHelper.HUGGERS_TABLE, HUGGERS_ALL_COLUMNS, null, null, null, null, null );
        cursor.moveToFirst();

        while( !cursor.isAfterLast() ){
            Hugger hugger = cursorToHugger( cursor );
            huggers.put( hugger.getId(), hugger );
            cursor.moveToNext();
        }//end while
        cursor.close();

        return huggers;
    }


    public boolean huggerExists( String id ){
        Cursor cursor = findHugger( id );
        boolean ret = cursor.getCount() > 0;
        cursor.close();
        return ret;
    }


    public int getHuggersCount(){
        return ( int ) DatabaseUtils.queryNumEntries( db, SqlHelper.HUGGERS_TABLE );
    }


    // ----------------------------------------------------


    private Cursor findHugger( String id ){
        return db.query( SqlHelper.HUGGERS_TABLE, HUGGERS_ALL_COLUMNS, SqlHelper.HR_COL_ID + " =?", new String[]{ id
        }, null, null, null );
    }


    private static ContentValues huggerToContentValues( Hugger hugger ){
        ContentValues values = new ContentValues();
        values.put( SqlHelper.HR_COL_ID, hugger.getId() );
        return values;
    }


    public static Hugger cursorToHugger( Cursor cursor ){
        if( cursor.isAfterLast() ) return null;
        Hugger hugger = new Hugger();
        hugger.setId( cursor.getString( cursor.getColumnIndex( SqlHelper.HR_COL_ID ) ) );
        return hugger;
    }


}//end class
