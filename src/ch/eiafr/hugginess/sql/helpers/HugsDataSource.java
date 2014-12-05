package ch.eiafr.hugginess.sql.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import ch.eiafr.hugginess.sql.entities.Hug;
import ch.eiafr.hugginess.sql.entities.Hugger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author: Lucy Linder
 * @date: 26.11.2014
 */
public class HugsDataSource{
    private SQLiteDatabase db;
    private SqlHelper helper;
    private static final String[] ALL_COLUMNS = new String[]{ SqlHelper.HG_COL_ID, SqlHelper.HG_COL_ID_REF, SqlHelper.HG_COL_DATE, SqlHelper.HG_COL_DUR,
            SqlHelper.HG_COL_DATA };


    public HugsDataSource( Context context ){
        helper = new SqlHelper( context );
    }


    public HugsDataSource open() throws SQLException{
        db = helper.getWritableDatabase();
        return this;
    }


    public void close(){
        helper.close();
    }


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
}//end class
