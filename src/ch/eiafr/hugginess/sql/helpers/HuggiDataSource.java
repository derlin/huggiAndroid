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

import static ch.eiafr.hugginess.sql.helpers.SqlHelper.*;

/**
 * This class is a wrapper for the sql database, acting as a DAO layer.
 * It offers methods to get Hug and Hugger objects (entities) and to
 * retrieve statistics from the database.
 * <p/>
 * Since our database is rather simple (two tables), we chose to use only
 * one helper for everything.
 * <p/>
 * Note that pretty all the methods can throw a runtime exception, so be careful to check for null values.
 * <p/>
 * creation date    12.12.2014
 * context          Projet de semestre Hugginess, EIA-FR, I3 2014-2015
 *
 * @author Lucy Linder
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


    /**
     * Create a datasource helper to easily acces information stored in the sql database.
     *
     * @param context the context
     */
    public HuggiDataSource( Context context ){
        helper = new SqlHelper( context );
    }


    /**
     * Create a datasource helper to easily acces information stored in the sql database.
     * Use this method with a parameter set to true when you want to use it inside a try-with-resource block.
     *
     * @param context  the context
     * @param autoOpen if set to true, the database will be opened automatically.
     * @throws SQLException if the db could not be opened.
     */
    public HuggiDataSource( Context context, boolean autoOpen ) throws SQLException{
        this( context );
        if( autoOpen ) this.open();
    }


    /**
     * Open the database used by the helper. Don't forget to call {@link #close()} as soon as you finish to free resources.
     *
     * @return this object (useful for chaining)
     * @throws SQLException
     */
    public HuggiDataSource open() throws SQLException{
        db = helper.getWritableDatabase();
        return this;
    }


    /**
     * Close the database used by this helper. Once closed, the datasource cannot be opened anymore.
     */
    public void close(){
        helper.close();
    }


    /* *****************************************************************
     * All
     * ****************************************************************/


    /**
     * Clear all the data stored in the database. Cannot be undone.
     *
     * @return rhe total number of rows deleted.
     */
    public int clearAllData(){
        int count = 0;
        count += db.delete( SqlHelper.HUGS_TABLE, null, null );
        count += db.delete( SqlHelper.HUGGERS_TABLE, null, null );
        return count;
    }

    /* *****************************************************************
     * Hugs
     * ****************************************************************/


    /**
     * Delete the given hug from the database.
     *
     * @param id the hug id
     * @return true if a hug was deleted.
     */
    public boolean deleteHug( String id ){
        return db.delete( SqlHelper.HUGS_TABLE, SqlHelper.HG_COL_ID + " = " + id, null ) > 0;
    }


    /**
     * Store a hug in the database, if it does not already exist.
     * If the hugger does not exist, it will be inserted as well.
     *
     * @param hug the hug
     * @return true if the hug was inserted.
     */
    public boolean addHug( Hug hug ){
        if( !huggerExists( hug.getHuggerID() ) ){
            Hugger hugger = new Hugger();
            hugger.setId( hug.getHuggerID() );
            addHugger( hugger );
        }
        return db.insert( SqlHelper.HUGS_TABLE, null, hugToContentValues( hug ) ) > 0;
    }


    /**
     * @return the list of hugs
     */
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


    /**
     * @return the number of hugs in the database.
     */
    public int getHugsCount(){
        return ( int ) DatabaseUtils.queryNumEntries( db, SqlHelper.HUGS_TABLE );
    }


    /**
     * @return the average duration of a hug.
     */
    public int getAvgHugsDuration(){
        Cursor c = db.rawQuery( "select avg(duration) from " + HUGS_TABLE, null );
        c.moveToFirst();
        int avg = c.getInt( 0 );
        c.close();
        return avg;
    }


    /**
     * Get the average number of hugs per day, week and month.
     *
     * @return an array of ints: [0] = h/day, [1] = h/week, [2] = h/month.
     */
    public int[] getAvgStats(){
        int[] stats = new int[ 3 ];

        // the general query: only the date formatter will change
        String queryF = "select avg(cnt) from (" +
                " select count(*) as cnt from " + HUGS_TABLE + //
                " group by strftime('%s', " + HG_COL_DATE + " / 1000, 'unixepoch') ) alias";

        Cursor c;

        // hugs per day
        c = db.rawQuery( String.format( queryF, "%Y-%m-%d" ), null );
        c.moveToFirst();
        stats[ 0 ] = c.getInt( 0 );
        c.close();

        // hugs per week
        c = db.rawQuery( String.format( queryF, "%Y-%W" ), null );
        c.moveToFirst();
        stats[ 1 ] = c.getInt( 0 );
        c.close();

        // hugs per month
        c = db.rawQuery( String.format( queryF, "%Y-%m" ), null );
        c.moveToFirst();
        stats[ 2 ] = c.getInt( 0 );
        c.close();

        return stats;
    }
    //----------------------------------- DAO conversions/mapping


    /*
     * Convert a hug entity to an object insertable in the db.
     */
    private ContentValues hugToContentValues( Hug hug ){
        ContentValues values = new ContentValues();
        values.put( SqlHelper.HG_COL_ID_REF, hug.getHuggerID() );
        values.put( SqlHelper.HG_COL_DUR, hug.getDuration() );
        values.put( SqlHelper.HG_COL_DATE, hug.getDate().getTime() );
        values.put( SqlHelper.HG_COL_DATA, hug.getData() );
        return values;
    }


    /*
     * Convert a cursor to a Hug object.
     */
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


    /**
     * Delete the given hugger from the database.
     *
     * @param id the hugger id
     * @return true if a hugger was deleted.
     */
    public boolean deleteHugger( String id ){
        db.delete( SqlHelper.HUGS_TABLE, SqlHelper.HG_COL_ID_REF + " = " + id, null );
        return db.delete( SqlHelper.HUGGERS_TABLE, SqlHelper.HR_COL_ID + " = " + id, null ) > 0;
    }


    /**
     * Store the given hugger in the database if it does not already exist.
     *
     * @param hugger the hugger.
     * @return true if a hugger was inserted.
     */
    public boolean addHugger( Hugger hugger ){
        return db.insert( SqlHelper.HUGGERS_TABLE, null, huggerToContentValues( hugger ) ) > 0;
    }


    /**
     * Retrieve one hugger from the database.
     *
     * @param huggerId the hugger id.
     * @return the hugger.
     */
    public Hugger getHugger( String huggerId ){
        Cursor cursor = findHugger( huggerId );
        Hugger hugger = cursorToHugger( cursor );
        cursor.close();
        return hugger;
    }


    /**
     * Get the huggers with whom the user has had the most interactions.
     *
     * @param nbr the number of huggers to return
     * @return a list of pairs [hugger, total number of hugs], sorted by descending number of hugs.
     */
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


    /**
     * @return a map of all the huggers stored in the database, indexed by the hugger id.
     */
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


    /**
     * @param id the hugger id
     * @return true if the hugger is present in the database, false otherwise.
     */
    public boolean huggerExists( String id ){
        Cursor cursor = findHugger( id );
        boolean ret = cursor.getCount() > 0;
        cursor.close();
        return ret;
    }


    /**
     *
     * @return the total number of huggers in the database.
     */
    public int getHuggersCount(){
        return ( int ) DatabaseUtils.queryNumEntries( db, SqlHelper.HUGGERS_TABLE );
    }


    // ----------------------------------------------------

    /*
     * Return a cursor positioned at the first row.
     * If the hugger does not exist, {@link Cursor#getCount()} will return 0 and
     * {@link Cursor#isAfterLast()} will be true.
     * If the hugger exist, it can be retrieved using {@link #cursorToHugger}.
     */
    private Cursor findHugger( String id ){
        Cursor c = db.query( SqlHelper.HUGGERS_TABLE, HUGGERS_ALL_COLUMNS, SqlHelper.HR_COL_ID + " =?", new String[]{ id
        }, null, null, null );
        c.moveToFirst();
        return c;
    }

    //----------------------------------- DAO conversions/mapping

    /*
    * Convert a hugger entity to an object insertable in the db.
    */
    private static ContentValues huggerToContentValues( Hugger hugger ){
        ContentValues values = new ContentValues();
        values.put( SqlHelper.HR_COL_ID, hugger.getId() );
        return values;
    }

    /*
     * Convert a cursor to a Hugger object.
     */
    public static Hugger cursorToHugger( Cursor cursor ){
        if( cursor.isAfterLast() ) return null;
        Hugger hugger = new Hugger();
        hugger.setId( cursor.getString( cursor.getColumnIndex( SqlHelper.HR_COL_ID ) ) );
        return hugger;
    }

}//end class
