package ch.eiafr.hugginess.sql.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import ch.eiafr.hugginess.sql.entities.Hug;
import ch.eiafr.hugginess.sql.entities.Hugger;

import java.sql.SQLException;
import java.util.*;

/**
 * User: lucy
 * Date: 12/12/14
 * Version: 0.1
 */
public class HuggiDataSource implements AutoCloseable{
    private SQLiteDatabase db;
    private SqlHelper helper;
    private static final String[] HUGGERS_ALL_COLUMNS = new String[]{ SqlHelper.HR_COL_ID };
    private static final String[] HUGS_ALL_COLUMNS = new String[]{ SqlHelper.HG_COL_ID, SqlHelper.HG_COL_ID_REF, SqlHelper.HG_COL_DATE, SqlHelper.HG_COL_DUR,
            SqlHelper.HG_COL_DATA };


    public HuggiDataSource( Context context ){
        helper = new SqlHelper( context );
    }
    public HuggiDataSource( Context context, boolean autoOpen ) throws SQLException{
        helper = new SqlHelper( context );
        if(autoOpen) this.open();
    }


    public HuggiDataSource open() throws SQLException{
        db = helper.getWritableDatabase();
        return this;
    }


    public void close(){
        helper.close();
    }

    /* *****************************************************************
     * Hugs
     * ****************************************************************/


    public boolean deleteHug( String id ){
        return db.delete( SqlHelper.HUGS_TABLE, SqlHelper.HG_COL_ID + " = " + id, null ) > 0;
    }


    public boolean addHug( Hug hug ){
        if( !HuggersDataSource.huggerExists( db, hug.getHuggerID() ) ){
            Hugger hugger = new Hugger();
            hugger.setId( hug.getHuggerID() );
            HuggersDataSource.addHugger( db, hugger );
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


    public static Hug cursorToHug( Cursor cursor ){
        Hug hug = new Hug();
        int i = 0;

        hug.setId( cursor.getInt( i++ ) );
        hug.setHuggerID( cursor.getString( i++ ) );
        hug.setDate( new Date(cursor.getLong( i++ ) ));
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
        return addHugger( db, hugger );
    }


    public List<Hugger> getHuggers(){
        List<Hugger> huggers = new ArrayList<>();

        Cursor cursor = db.query( SqlHelper.HUGGERS_TABLE, HUGGERS_ALL_COLUMNS, null, null, null, null, null );
        cursor.moveToFirst();

        while( !cursor.isAfterLast() ){
            huggers.add( cursorToHugger( cursor ) );
            cursor.moveToNext();
        }//end while
        cursor.close();

        return huggers;
    }


    public Hugger getHugger( String huggerId ){
        Cursor cursor = findHugger( db, huggerId );
        cursor.moveToFirst();
        Hugger hugger = cursorToHugger( cursor );
        cursor.close();
        return hugger;
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
        return huggerExists( db, id );
    }


    public int getHuggersCount(){
        return ( int ) DatabaseUtils.queryNumEntries( db, SqlHelper.HUGGERS_TABLE );
    }


    // ----------------------------------------------------


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


    public static Cursor findHugger( SQLiteDatabase db, String id ){
        return db.query( SqlHelper.HUGGERS_TABLE, HUGGERS_ALL_COLUMNS, SqlHelper.HR_COL_ID + " =?", new String[]{ id }, null, null,
                null );
    }

    public static boolean huggerExists( SQLiteDatabase db, String id ){
        Cursor cursor = findHugger( db, id );
        boolean ret = cursor.getCount() > 0;
        cursor.close();
        return ret;
    }


    public static boolean addHugger( SQLiteDatabase db, Hugger hugger ){
        return db.insert( SqlHelper.HUGGERS_TABLE, null, huggerToContentValues( hugger ) ) > 0;
    }

}//end class
