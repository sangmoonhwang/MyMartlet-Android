package ca.mcgill.mymcgill.object;

import android.content.Context;

import ca.mcgill.mymcgill.R;

/**
 * Author: Julien
 * Date: 2014-03-22 17:39
 */
public enum Season {
    FALL,
    WINTER,
    SUMMER;

    public static Season findSeason(String season){
        if(season.equalsIgnoreCase(Token.FALL.getString())){
            return FALL;
        }
        else if(season.equalsIgnoreCase(Token.WINTER.getString())){
            return WINTER;
        }
        else if(season.equalsIgnoreCase(Token.SUMMER.getString())){
            return SUMMER;
        }
        return null;
    }

    public String toString(Context context){
        switch(this){
            case FALL:
                return context.getResources().getString(R.string.fall);
            case WINTER:
                return context.getResources().getString(R.string.winter);
            case SUMMER:
                return context.getResources().getString(R.string.summer);
            default:
                return null;
        }
    }

    public String getSeasonNumber(){
        switch(this){
            case FALL:
                return "09";
            case WINTER:
                return "01";
            case SUMMER:
                return "05";
            default:
                return null;
        }
    }
}