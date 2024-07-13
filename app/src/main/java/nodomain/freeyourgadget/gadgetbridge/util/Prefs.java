/*  Copyright (C) 2016-2024 Carsten Pfeiffer, JohnnySun, José Rebelo

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.util;

import android.content.SharedPreferences;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Wraps SharedPreferences to avoid ClassCastExceptions and others.
 */
public class Prefs {
    private static final String TAG = "Prefs";
    // DO NOT use slf4j logger here, this would break its configuration via GBApplication
//    private static final Logger LOG = LoggerFactory.getLogger(Prefs.class);

    private final SharedPreferences preferences;

    public Prefs(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public String getString(String key, String defaultValue) {
        String value = preferences.getString(key, defaultValue);
        if (value == null || "".equals(value)) {
            return defaultValue;
        }
        return value;
    }

    public Set<String> getStringSet(String key, Set<String> defaultValue) {
        Set<String> value = preferences.getStringSet(key, defaultValue);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    /**
     * Returns the preference saved under the given key as an integer value.
     * Note that it is irrelevant whether the preference value was actually
     * saved as an integer value or a string value.
     * @param key the preference key
     * @param defaultValue the default value to return if the preference value is unset
     * @return the saved preference value or the given defaultValue
     */
    public int getInt(String key, int defaultValue) {
        try {
            return preferences.getInt(key, defaultValue);
        } catch (Exception ex) {
            try {
                String value = preferences.getString(key, String.valueOf(defaultValue));
                if ("".equals(value)) {
                    return defaultValue;
                }
                return Integer.parseInt(value);
            } catch (Exception ex2) {
                logReadError(key, ex);
                return defaultValue;
            }
        }
    }

    /**
     * Returns the preference saved under the given key as a long value.
     * Note that it is irrelevant whether the preference value was actually
     * saved as a long value or a string value.
     * @param key the preference key
     * @param defaultValue the default value to return if the preference value is unset
     * @return the saved preference value or the given defaultValue
     */
    public long getLong(String key, long defaultValue) {
        try {
            return preferences.getLong(key, defaultValue);
        } catch (Exception ex) {
            try {
                String value = preferences.getString(key, String.valueOf(defaultValue));
                if ("".equals(value)) {
                    return defaultValue;
                }
                return Long.parseLong(value);
            } catch (Exception ex2) {
                logReadError(key, ex);
                return defaultValue;
            }
        }
    }

    /**
     * Returns the preference saved under the given key as a float value.
     * Note that it is irrelevant whether the preference value was actually
     * saved as a float value or a string value.
     * @param key the preference key
     * @param defaultValue the default value to return if the preference value is unset
     * @return the saved preference value or the given defaultValue
     */
    public float getFloat(String key, float defaultValue) {
        try {
            return preferences.getFloat(key, defaultValue);
        } catch (Exception ex) {
            try {
                String value = preferences.getString(key, String.valueOf(defaultValue));
                if ("".equals(value)) {
                    return defaultValue;
                }
                return Float.parseFloat(value);
            } catch (Exception ex2) {
                logReadError(key, ex);
                return defaultValue;
            }
        }
    }

    /**
     * Returns the preference saved under the given key as a boolean value.
     * Note that it is irrelevant whether the preference value was actually
     * saved as a boolean value or a string value.
     * @param key the preference key
     * @param defaultValue the default value to return if the preference value is unset
     * @return the saved preference value or the given defaultValue
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            return preferences.getBoolean(key, defaultValue);
        } catch (Exception ex) {
            try {
                String value = preferences.getString(key, String.valueOf(defaultValue));
                if ("".equals(value)) {
                    return defaultValue;
                }
                return Boolean.parseBoolean(value);
            } catch (Exception ex2) {
                logReadError(key, ex);
                return defaultValue;
            }
        }
    }

    /**
     * Returns the preference saved under the given key as a list of strings.
     * The preference is assumed to be a string, with each value separated by a comma.
     * @param key the preference key
     * @param defaultValue the default value to return if the preference value is unset
     * @param separator the separator to use to split the string
     * @return the saved preference value or the given defaultValue
     */
    public List<String> getList(final String key, final List<String> defaultValue, final String separatorRegex) {
        final String stringValue = preferences.getString(key, null);
        if (stringValue == null) {
            return defaultValue;
        }
        return Arrays.asList(stringValue.split(separatorRegex));
    }

    public List<String> getList(final String key, final List<String> defaultValue) {
        return getList(key, defaultValue, ",");
    }

    @Deprecated  // use getLocalTime
    public Date getTimePreference(final String key, final String defaultValue) {
        final String time = getString(key, defaultValue);

        final DateFormat df = new SimpleDateFormat("HH:mm", Locale.ROOT);
        try {
            return df.parse(time);
        } catch (final Exception e) {
            Log.e(TAG, "Error reading datetime preference value: " + key + "; returning default current time", e); // log the first exception
        }

        return new Date();
    }

    public LocalTime getLocalTime(final String key, final String defaultValue) {
        final String time = getString(key, defaultValue);

        final DateFormat df = new SimpleDateFormat("HH:mm", Locale.ROOT);
        try {
            final Date parse = df.parse(time);
            final Calendar calendar = GregorianCalendar.getInstance();
            calendar.setTime(parse);

            return LocalTime.of(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), 0);
        } catch (final Exception e) {
            Log.e(TAG, "Error reading localtime preference value: " + key + "; returning default current time", e); // log the first exception
        }

        return LocalTime.now();
    }

    private void logReadError(String key, Exception ex) {
        Log.e(TAG, "Error reading preference value: " + key + "; returning default value", ex); // log the first exception
    }

    public boolean contains(final String key) {
        return preferences.contains(key);
    }

    /**
     * Access to the underlying SharedPreferences, typically only used for editing values.
     * @return the underlying SharedPreferences object.
     */
    public SharedPreferences getPreferences() {
        return preferences;
    }

    /**
     * Ugly workaround for Set<String> preferences not consistently applying.
     * @param editor
     * @param preference
     * @param value
     */
    public static void putStringSet(SharedPreferences.Editor editor, String preference, HashSet<String> value) {
        editor.putStringSet(preference, null);
        editor.commit();
        editor.putStringSet(preference, new HashSet<>(value));
    }
}
