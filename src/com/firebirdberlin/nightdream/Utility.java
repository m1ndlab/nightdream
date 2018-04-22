package com.firebirdberlin.nightdream;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings.System;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.content.Context.POWER_SERVICE;

public class Utility {
    private static final String SCREENSAVER_ENABLED = "screensaver_enabled";
    private static final String SCREENSAVER_COMPONENTS = "screensaver_components";
    private static String TAG ="NightDreamActivity";
    int system_brightness_mode = System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
    private Context mContext;

    // constructor
    public Utility(Context context){
        this.mContext = context;
        getSystemBrightnessMode();
    }

    static public boolean is24HourFormat(Context context) {
        return android.text.format.DateFormat.is24HourFormat(context);
    }

    static public String formatTime(String format, Calendar calendar) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(calendar.getTime());
    }

    static public String[] getWeekdayStrings() {
        return getWeekdayStringsForLocale(Locale.getDefault());
    }

    static public String[] getWeekdayStringsForLocale(Locale locale) {
        DateFormatSymbols symbols = new DateFormatSymbols(locale);
        String[] dayNames = symbols.getShortWeekdays();
        for (int i = 1; i < dayNames.length; i++) {
            dayNames[i] = dayNames[i].substring(0, 1);
        }
        return dayNames;
    }

    static public int getFirstDayOfWeek() {
        return Calendar.getInstance().getFirstDayOfWeek();
    }

    static public Sensor getLightSensor(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        return sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    public static boolean isDebuggable(Context context){
        return (context != null &&
                0 != (context.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE));
    }

    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static int getScreenOffTimeout(Context context){
        return System.getInt(context.getContentResolver(), System.SCREEN_OFF_TIMEOUT, -1);
    }

    public static boolean isDaydreamEnabled(final Context c) {
        if(Build.VERSION.SDK_INT < 17) return false;
        return 1 == android.provider.Settings.Secure.getInt(c.getContentResolver(), SCREENSAVER_ENABLED, -1);
    }

    public static boolean isDaydreamEnabledOnDock(final Context c) {
        return 1 == android.provider.Settings.Secure.getInt(c.getContentResolver(), "screensaver_activate_on_dock", -1);
    }

    public static boolean isDaydreamEnabledOnSleep(final Context c) {
        return 1 == android.provider.Settings.Secure.getInt(c.getContentResolver(), "screensaver_activate_on_sleep", -1);
    }

    public static String getSelectedDaydreamClassName(final Context c) {
        String names = android.provider.Settings.Secure.getString(c.getContentResolver(), SCREENSAVER_COMPONENTS);
        return names == null ? null : componentsFromString(names)[0].getClassName();
    }

    private static ComponentName[] componentsFromString(String names) {
        String[] namesArray = names.split(",");
        ComponentName[] componentNames = new ComponentName[namesArray.length];
        for (int i = 0; i < namesArray.length; i++) {
            componentNames[i] = ComponentName.unflattenFromString(namesArray[i]);
        }
        return componentNames;
    }

    public static boolean isConfiguredAsDaydream(final Context c) {
        if(Build.VERSION.SDK_INT < 17) return false;
        if (1 == android.provider.Settings.Secure.getInt(c.getContentResolver(), SCREENSAVER_ENABLED, -1)) {
            String classname = getSelectedDaydreamClassName(c);
            Log.i(TAG, "Daydream is active " + classname);
            return "com.firebirdberlin.nightdream.NightDreamService".equals(classname);
        }
        return false;
    }

    static public void toggleComponentState(Context context, Class component, boolean on){
        ComponentName receiver = new ComponentName(context, component);
        PackageManager pm = context.getPackageManager();
        int new_state = (on) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        pm.setComponentEnabledSetting(receiver, new_state, PackageManager.DONT_KILL_APP);
    }

    public static long getFirstInstallTime(Context context) {
        if(Build.VERSION.SDK_INT < 9) return 0L;
        try {
            return context.getPackageManager()
                          .getPackageInfo(context.getPackageName(), 0)
                          .firstInstallTime;
        } catch (NameNotFoundException e) {
            return 0L;
        }
    }

    public static boolean hasNetworkConnection(Context context) {
        ConnectivityManager cm =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return ( activeNetwork != null && activeNetwork.isConnectedOrConnecting());
    }

    public static boolean hasFastNetworkConnection(Context context) {
        ConnectivityManager cm =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return ( hasNetworkConnection(context) &&
                ( activeNetwork.getType() == ConnectivityManager.TYPE_WIFI ||
                  activeNetwork.getType() == ConnectivityManager.TYPE_ETHERNET));
    }

    public static void turnScreenOn(Context c) {
        try {
            @SuppressWarnings("deprecation")
            PowerManager.WakeLock wl =
                    ((PowerManager) c.getSystemService(POWER_SERVICE))
                            .newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                                            PowerManager.ACQUIRE_CAUSES_WAKEUP,
                                    "WAKE_LOCK_TAG");
            wl.acquire();
            wl.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int pixelsToDp(Context context, float px) {
        DisplayMetrics displaymetrics = context.getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, displaymetrics);
    }

    public static int dpToPx(Context context, float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());

    }

    public static int spToPx(Context context, float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
                context.getResources().getDisplayMetrics());
    }

    public static int getNearestEvenIntValue(float value) {
        int r = (int) Math.ceil(value);
        if (r % 2 != 0) {
            r = (int) Math.floor(value);
        }
        return r;
    }

    public static int getHeightOfView(View contentview) {
        contentview.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        return contentview.getMeasuredHeight();
    }

    public static boolean isCharging(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return (status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL);
    }

    public static boolean languageIs(String... languages) {
        for (String language : languages) {
            if (language.equals(Locale.getDefault().getLanguage())) {
                return true;
            }
        }
        return false;
    }

    public static void hideSystemUI(Context context) {
        hideSystemUI(((Activity) context).getWindow());
    }

    public static void hideSystemUI(Window window) {
        if (window == null) return;
        if (Build.VERSION.SDK_INT >= 19) {

            View decorView = window.getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    public Point getDisplaySize() {
        Point size = new Point();
        if (Build.VERSION.SDK_INT < 13) {
            size = getDisplaySizeV12();
        } else if (Build.VERSION.SDK_INT < 17) {
            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getMetrics(metrics);
            size.x = metrics.widthPixels;
            size.y = metrics.heightPixels;
        } else {
            WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            display.getSize(size);
        }

        return size;
    }

    @TargetApi(12)
    @SuppressWarnings("deprecation")
    public Point getDisplaySizeV12() {
        Point size = new Point();
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        size.x = display.getWidth();
        size.y = display.getHeight();
        return size;
    }

    public boolean isDebuggable() {
        return (0 != (mContext.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE));
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = mContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = mContext.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public void getSystemBrightnessMode() {
        system_brightness_mode = System.getInt(mContext.getContentResolver(),
                System.SCREEN_BRIGHTNESS_MODE,
                System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    }

    public void setManualBrightnessMode() {
        System.putInt(mContext.getContentResolver(), System.SCREEN_BRIGHTNESS_MODE,
                System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    public void setAutoBrightnessMode() {
        System.putInt(mContext.getContentResolver(), System.SCREEN_BRIGHTNESS_MODE,
                System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    }

    public void restoreSystemBrightnessMode() {
        System.putInt(mContext.getContentResolver(), System.SCREEN_BRIGHTNESS_MODE,
                system_brightness_mode);
    }

    private static final SimpleDateFormat LOG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    public static void logToFile(Context context, String logFileName, String text) {

        // allow logging only in debug mode
        if (!isDebuggable(context)) {
            return;
        }

        String filePath = context.getFilesDir().getPath().toString() + "/" + logFileName;
        Log.d(TAG, "log file locatin: " + filePath);
        File logFile = new File(filePath);
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                Log.i(TAG, "error creating log file ", e);
            }
        }

        if (logFile.exists()) {

            Date time = Calendar.getInstance().getTime();
            String formattedTime = LOG_DATE_FORMAT.format(time);

            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true));
                writer.append(formattedTime + " " + text);
                writer.newLine();
                writer.flush();
                writer.close();
            } catch (IOException e) {
                Log.i(TAG, "error writing to log file ", e);
            }
        }
    }

    public static boolean isScreenOn(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT <= 19){
            return deprecatedIsScreenOn(pm);
        }
        return pm.isInteractive();
    }

    public static void logResponseHeaders(Map<String, List<String>> responseHeaders) {
        for (String key : responseHeaders.keySet()) {
            List<String> v = responseHeaders.get(key);
            Log.i(TAG, "header: " + key + "=" + (v != null && !v.isEmpty() ? v.get(0) : "null"));
        }
    }

    @SuppressWarnings("deprecation")
    protected static boolean deprecatedIsScreenOn(PowerManager pm) {
        return pm.isScreenOn();
    }
}
