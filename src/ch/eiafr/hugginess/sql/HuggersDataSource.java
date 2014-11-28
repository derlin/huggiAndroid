package ch.eiafr.hugginess.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static ch.eiafr.hugginess.sql.SqlHelper.*;

/**
 * @author: Lucy Linder
 * @date: 26.11.2014
 */
public class HuggersDataSource{

    private static final String[] ALL_COLUMNS = new String[]{ HR_COL_ID };
    private SQLiteDatabase db;
    private SqlHelper helper;


    public HuggersDataSource( Context context ){
        helper = new SqlHelper( context );
    }


    public void open() throws SQLException{
        db = helper.getWritableDatabase();
    }


    public void close(){
        helper.close();
    }


    public boolean deleteHugger( String id ){
        db.delete( HUGS_TABLE, HG_COL_ID_REF + " = " + id, null );
        return db.delete( HUGGERS_TABLE, HR_COL_ID + " = " + id, null ) > 0;
    }


    public boolean addHugger( Hugger hugger ){
        return addHugger( db, hugger );
    }




    public List<Hugger> getHuggers(){
        List<Hugger> huggers = new ArrayList<>();

        Cursor cursor = db.query( HUGGERS_TABLE, ALL_COLUMNS, null, null, null, null, null );
        cursor.moveToFirst();

        while( !cursor.isAfterLast() ){
            huggers.add( cursorToHugger( cursor ) );
            cursor.moveToNext();
        }//end while
        cursor.close();

        return huggers;
    }

    public boolean huggerExists( String id ){
        return huggerExists( db, id );
    }

    public int getHuggersCount(){
        return ( int ) DatabaseUtils.queryNumEntries(db, HUGGERS_TABLE);
    }



    // ----------------------------------------------------

    private static ContentValues huggerToContentValues( Hugger hugger ){
        ContentValues values = new ContentValues();
        values.put( HR_COL_ID, hugger.getId() );
        return values;
    }


    private static Hugger cursorToHugger( Cursor cursor ){
        Hugger hugger = new Hugger();
        hugger.setId( cursor.getString( 0 ) );
        return hugger;
    }

    static Cursor findHugger( SQLiteDatabase db, String id ){
        return db.query( HUGGERS_TABLE, ALL_COLUMNS, HR_COL_ID + " = " + id, null, null, null, null );
    }

    static boolean huggerExists(SQLiteDatabase db, String id){
        Cursor cursor = findHugger( db, id );
        boolean ret = cursor.getCount() > 0;
        cursor.close();
        return ret;
    }

    static boolean addHugger( SQLiteDatabase db, Hugger hugger ){
        return db.insert( HUGGERS_TABLE, null, huggerToContentValues( hugger ) ) > 0;
    }

}//end class
