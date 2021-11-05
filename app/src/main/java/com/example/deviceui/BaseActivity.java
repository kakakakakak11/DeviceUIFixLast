package com.example.deviceui;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

abstract class BaseActivity extends AppCompatActivity {
    protected static final String LANGUAGE_ENGLISH = "en";
    protected static final String LANGUAGE_UKRAINIAN = "uk";
    private static final String LANGUAGE_RUSSIAN = "ru";
    protected static final String languageKey = "languageKey";
    SharedPreferences mSettings;
    public String languageValue;

    LocaleManager localeManager = new LocaleManager();

    @Override
    protected void attachBaseContext(Context newBase) {
        mSettings = newBase.getSharedPreferences("DataBase", Context.MODE_PRIVATE);
        if (mSettings.contains(languageKey)) {
            languageValue = mSettings.getString(languageKey, LANGUAGE_UKRAINIAN);
            Log.d("Locale", "key: " + languageValue);
        }

        Log.d("Locale", "keyattachBase: " + languageValue);
        Log.d("Locale", "attachBaseContext");
        if (languageValue == null) {
            super.attachBaseContext(newBase);
        } else {
            Context contextOne;
            contextOne = localeManager.updateResources(newBase, languageValue);
            super.attachBaseContext(contextOne);
        }
    }
}
