package ca.mcgill.mymcgill.activity.base;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;

import java.util.Locale;

import ca.mcgill.mymcgill.App;

/**
 * Author: Julien
 * Date: 2014-03-16 11:42
 */
public class BaseActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //Update locale and config
        Locale locale = new Locale(App.getLanguage().getLanguageString());
        Locale.setDefault(locale);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        //Update locale and config
        Locale locale = new Locale(App.getLanguage().getLanguageString());
        Locale.setDefault(locale);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }
}
