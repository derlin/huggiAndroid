package ch.eiafr.hugginess.sql.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import ch.eiafr.hugginess.sql.entities.Hugger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author: Lucy Linder
 * @date: 26.11.2014
 */
public class HuggersDataSource implements AutoCloseable {

    private static final String[] ALL_COLUMNS = new String[]{ SqlHelper.HR_COL_ID };
    private SQLiteDatabase db;
    private SqlHelper helper;


    public HuggersDataSource( Context context ){
        helper = new SqlHelper( context );
    }


    public HuggersDataSource( Context context, boolean open ) throws SQLException{
        helper = new SqlHelper( context );
        open();
    }


    public HuggersDataSource open() throws SQLException{
        db = helper.getWritableDatabase();
        return this;
    }


    public void close(){
        helper.close();
    }


    public boolean deleteHugger( String id ){
        db.delete( SqlHelper.HUGS_TABLE, SqlHelper.HG_COL_ID_REF + " = " + id, null );
        return db.delete( SqlHelper.HUGGERS_TABLE, SqlHelper.HR_COL_ID + " = " + id, null ) > 0;
    }


    public boolean addHugger( Hugger hugger ){
        return addHugger( db, hugger );
    }


    public List<Hugger> getHuggers(){
        List<Hugger> huggers = new ArrayList<>();

        Cursor cursor = db.query( SqlHelper.HUGGERS_TABLE, ALL_COLUMNS, null, null, null, null, null );
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

        Cursor cursor = db.query( SqlHelper.HUGGERS_TABLE, ALL_COLUMNS, null, null, null, null, null );
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
        return db.query( SqlHelper.HUGGERS_TABLE, ALL_COLUMNS, SqlHelper.HR_COL_ID + " =?", new String[]{ id }, null, null,
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
