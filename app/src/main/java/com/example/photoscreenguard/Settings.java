package com.example.photoscreenguard;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Calendar;
import java.util.Locale;

final class Settings {
    static final String PREF = "photo_guard_settings";
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREF, Context.MODE_PRIVATE); }
    static int getInt(Context c, String k, int def) { return prefs(c).getInt(k,def); }
    static String getString(Context c, String k, String def) { return prefs(c).getString(k,def); }
    static boolean enabled(Context c) { return prefs(c).getBoolean("enabled",false); }

    static boolean validPeriods(String s) {
        if (s == null || s.trim().isEmpty()) return true;
        for (String part : s.split("[,，;；\\n]+")) {
            String[] bounds = part.trim().split("\\s*[-~～]\\s*");
            if (bounds.length != 2 || parseMin(bounds[0]) < 0 || parseMin(bounds[1]) < 0) return false;
        }
        return true;
    }
    static int parseMin(String s) {
        if (!s.matches("\\d{1,2}:\\d{2}")) return -1;
        String[] a = s.split(":");
        int h = Integer.parseInt(a[0]), m = Integer.parseInt(a[1]);
        return h <= 23 && m <= 59 ? h*60+m : -1;
    }
    static boolean inPeriods(String periods, int nowMin) {
        if (periods == null || periods.trim().isEmpty()) return false;
        for (String part : periods.split("[,，;；\\n]+")) {
            String[] a = part.trim().split("\\s*[-~～]\\s*");
            if(a.length!=2) continue;
            int start = parseMin(a[0]), end = parseMin(a[1]);
            if (start < 0 || end < 0 || start == end) continue;
            if ((start < end && nowMin>=start && nowMin<end) ||
                (start > end && (nowMin>=start || nowMin<end))) return true;
        }
        return false;
    }
    static boolean activeNow(Context c, Calendar cal) {
        int day=cal.get(Calendar.DAY_OF_WEEK);
        boolean weekend = day==Calendar.SATURDAY || day==Calendar.SUNDAY;
        String periods=getString(c,weekend?"weekend":"weekday", weekend?"08:00-23:00":"07:00-08:30,12:00-13:30,18:00-23:00");
        int minutes=cal.get(Calendar.HOUR_OF_DAY)*60+cal.get(Calendar.MINUTE);
        return inPeriods(periods,minutes);
    }
    static int toRaw(int percent) { return Math.max(1,Math.min(255,Math.round(percent*255f/100f))); }
}
