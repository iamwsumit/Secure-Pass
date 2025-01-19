package com.sumit.passwordmanager;

import android.content.Context;
import android.content.SharedPreferences;

public class LocalStorage {

    private static final String PREFERENCES_NAME = "PasswordManager";

    private static SharedPreferences getPref(Context context) {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    protected static void writeString(Context context, String key, String value) {
        SharedPreferences.Editor editor = getPref(context).edit();
        editor.putString(key, value);
        editor.apply();
    }

    protected static String getString(Context context, String key) {
        return getPref(context).getString(key, "");
    }
}