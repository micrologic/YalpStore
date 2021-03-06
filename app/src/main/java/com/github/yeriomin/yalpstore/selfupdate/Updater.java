package com.github.yeriomin.yalpstore.selfupdate;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.github.yeriomin.yalpstore.BuildConfig;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

abstract public class Updater {

    static private final String CACHED_VERSION_CODE = "CACHED_VERSION_CODE";
    static private final String CACHED_VERSION_CODE_CHECKED_AT = "CACHED_VERSION_CODE_CHECKED_AT";
    static private final long CACHED_VERSION_CODE_VALID_FOR = 60*60;

    protected Context context;

    public Updater(Context context) {
        this.context = context;
    }

    abstract public String getUrlString(int versionCode);

    public int getLatestVersionCode() {
        int latestVersionCode = getCachedVersionCode();
        if (latestVersionCode == 0) {
            latestVersionCode = BuildConfig.VERSION_CODE;
            while (isAvailable(latestVersionCode + 1)) {
                latestVersionCode++;
            }
            cacheVersionCode(latestVersionCode);
        }
        return latestVersionCode;
    }

    private int getCachedVersionCode() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return (System.currentTimeMillis() - preferences.getLong(CACHED_VERSION_CODE_CHECKED_AT, 0)) > CACHED_VERSION_CODE_VALID_FOR
            ? 0
            : preferences.getInt(CACHED_VERSION_CODE, 0)
        ;
    }

    private void cacheVersionCode(int versionCode) {
        SharedPreferences.Editor preferences = PreferenceManager.getDefaultSharedPreferences(context).edit();
        preferences.putInt(CACHED_VERSION_CODE, versionCode);
        preferences.putLong(CACHED_VERSION_CODE_CHECKED_AT, System.currentTimeMillis());
        preferences.commit();
    }

    private URL getUrl(int versionCode) {
        try {
            return new URL(getUrlString(versionCode));
        } catch (MalformedURLException e) {
            // Unlikely
        }
        return null;
    }

    private boolean isAvailable(int versionCode) {
        try {
            URLConnection connection = getUrl(versionCode).openConnection();
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).setInstanceFollowRedirects(false);
                ((HttpURLConnection) connection).setRequestMethod("HEAD");
                return ((HttpURLConnection) connection).getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST;
            }
            InputStream in = connection.getInputStream();
            in.close();
            return true;
        } catch (IOException x) {
            return false;
        }
    }
}
