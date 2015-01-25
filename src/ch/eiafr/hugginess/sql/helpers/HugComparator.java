/*
 * Copyright (c) 2015. The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author	Lucy Linder
 */

package ch.eiafr.hugginess.sql.helpers;

import ch.eiafr.hugginess.sql.entities.Hug;

import java.util.Comparator;

/**
 * @author: Lucy Linder
 * @date: 25.01.2015
 */
public class HugComparator implements Comparator<Hug>{

    private boolean sortAscending = false;

    public static final int BY_DATE  = 0;
    public static final int BY_DURATION  = 1;
    public static final int BY_HUGGER  = 2;


    private int sortType = BY_DATE;

    @Override
    public int compare( Hug lhs, Hug rhs ){
        switch( sortType ){
            case BY_DATE: return compareByDate( lhs, rhs );
            case BY_DURATION: return compareByDuration( lhs, rhs );
            case BY_HUGGER: return compareByHugger( lhs, rhs );
        }
        throw  new IllegalArgumentException( "int outside bounds" );
    }




    public boolean isSortAscending(){
        return sortAscending;
    }


    public void setSortAscending( boolean sortAscending ){
        this.sortAscending = sortAscending;
    }


    public int getSortType(){
        return sortType;
    }


    public void setSortType( int sortType ){
        this.sortType = sortType;
    }


    private int compareByDate(Hug lhs, Hug rhs){
        return lhs.getDate().compareTo( rhs.getDate() ) * (sortAscending ? 1 : -1);
    }

    private int compareByDuration( Hug lhs, Hug rhs ){
        int ldur = lhs.getDuration();
        int rdur = rhs.getDuration();
        if( ldur == rdur ) return 0;

        if(sortAscending) return ldur < rdur ? -1 : 1;
        else  return ldur > rdur ? -1 : 1;
    }


    private int compareByHugger( Hug lhs, Hug rhs ){
        if(lhs.getHuggerID().equals( rhs.getHuggerID() )){
            return compareByDate( lhs, rhs );
        }

        return lhs.getHuggerID().compareTo( rhs.getHuggerID() )* (sortAscending ? 1 : -1);
    }
}//end class
