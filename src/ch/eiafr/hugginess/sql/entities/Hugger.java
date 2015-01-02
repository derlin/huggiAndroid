package ch.eiafr.hugginess.sql.entities;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import ch.eiafr.hugginess.app.App;

/**
 * This class is a DAO entity for a Hugger.
 * <p/>
 * If the hugger is a local contact (the id matches a phone number registered in this phone),
 * its name and picture can be retrieved by calling {@link #getDetails()}. Note that since querying
 * the phone database can be a heavy process, those information will be fetch only on demand.
 * <p/>
 * creation date    26.11.2014
 * context          Projet de semestre Hugginess, EIA-FR, I3 2014-2015
 *
 * @author Lucy Linder
 */
public class Hugger{

    private String id;
    private LocalContactDetails details;
    private boolean isLocalContactLoaded = false;


    // ----------------------------------------------------


    public String getId(){
        return id;
    }


    public void setId( String id ){
        this.id = id;
    }


    // ----------------------------------------------------


    @Override
    public boolean equals( Object o ){
        if( this == o ) return true;
        if( o == null || getClass() != o.getClass() ) return false;

        Hugger hugger = ( Hugger ) o;
        return id.equals( hugger.id );
    }


    @Override
    public int hashCode(){
        return id.hashCode();
    }

    // ----------------------------------------------------

    public static class LocalContactDetails{

        private long contactId;
        private String name;
        public Uri photoUri;


        public Uri getPhotoUri(){
            return photoUri;
        }


        public long getContactId(){
            return contactId;
        }


        public String getName(){
            return name;
        }
    }

    // ----------------------------------------------------


    /**
     * Check if the hugger is a local contact.
     * Note that calling this method will trigger a query to the Android System, which can be a heavy process.
     *
     * @return true if this hugger is a local contact.
     */
    public boolean isLocalContact(){
        return getDetails() != null;
    }


    /**
     * Get the name and picture of this local contact.
     *
     * @return the local details, or null if this hugger is not a local contact.
     */
    public LocalContactDetails getDetails(){
        if( !isLocalContactLoaded ){
            details = getContactDetails( id );
            isLocalContactLoaded = true;
        }
        return details;
    }

    // ----------------------------------------------------

    /*
     * Query the system to check if this hugger is a local contact. If so, the contact_id, name
     * and picture will be stored in a {@link LocalContactDetails} object.
     */
    private static LocalContactDetails getContactDetails( String number ){

        Context context = App.getAppContext();

        LocalContactDetails details = new LocalContactDetails();

        // define what info we are interested in
        String[] projection = new String[]{ //
                ContactsContract.PhoneLookup.DISPLAY_NAME,//
                ContactsContract.PhoneLookup._ID,  //
                ContactsContract.PhoneLookup.PHOTO_URI };

        // encode the phone number and build the filter URI
        Uri contactUri = Uri.withAppendedPath( ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode( number ) );

        // query
        Cursor cursor = context.getContentResolver().query( contactUri, projection, null, null, null );

        if( cursor.moveToFirst() ){ // the hugger is a local contact
            // get values from the contacts database
            details.contactId = cursor.getLong( cursor.getColumnIndex( ContactsContract.PhoneLookup._ID ) );
            details.name = cursor.getString( cursor.getColumnIndex( ContactsContract.PhoneLookup.DISPLAY_NAME ) );
            String s = cursor.getString( cursor.getColumnIndex( ContactsContract.PhoneLookup.PHOTO_URI ) );
            if( s != null ) details.photoUri = Uri.parse( s );

        }else{  // the hugger is not a local contact
            Log.v( context.getPackageName(), "Hugger [" + number + "] is not a local contact." );
            return null;
        }

        return details;
    }


}//end class
