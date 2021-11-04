package com.example.deviceui;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

public class LocaleManager {


    public Context updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Resources res = context.getResources();

        Configuration config = new Configuration(res.getConfiguration());

        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }

}
