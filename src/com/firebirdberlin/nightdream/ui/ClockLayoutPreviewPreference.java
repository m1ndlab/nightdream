package com.firebirdberlin.nightdream.ui;

import android.animation.LayoutTransition;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebirdberlin.nightdream.PreferencesActivity;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.AnalogClockConfig;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

public class ClockLayoutPreviewPreference extends Preference {
    private static PreviewMode previewMode = PreviewMode.DAY;
    private ClockLayout clockLayout = null;
    private TextView textViewPurchaseHint = null;
    private View preferenceView = null;
    private LinearLayout preferencesContainer = null;
    private ImageButton resetButton = null;

    private Context context = null;

    public ClockLayoutPreviewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public ClockLayoutPreviewPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    public static void setPreviewMode(PreviewMode previewMode) {
        ClockLayoutPreviewPreference.previewMode = previewMode;
    }

    public void invalidate() {
        notifyChanged();
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        preferenceView = super.onCreateView(parent);

        View summary = preferenceView.findViewById(android.R.id.summary);
        if (summary != null) {
            ViewParent summaryParent = summary.getParent();
            if (summaryParent instanceof ViewGroup) {
                final LayoutInflater layoutInflater =
                    (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                ViewGroup summaryParent2 = (ViewGroup) summaryParent;
                layoutInflater.inflate(R.layout.clock_layout_preference, summaryParent2, true);

                RelativeLayout previewContainer = (RelativeLayout) summaryParent2.findViewById(R.id.previewContainer);
                clockLayout = (ClockLayout) summaryParent2.findViewById(R.id.clockLayout);
                resetButton = (ImageButton) summaryParent2.findViewById(R.id.resetButton);
                textViewPurchaseHint = (TextView) summaryParent2.findViewById(R.id.textViewPurchaseHint);
                preferencesContainer = (LinearLayout) summaryParent2.findViewById(R.id.preferencesContainer);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    LayoutTransition lt = new LayoutTransition();
                    lt.disableTransitionType(LayoutTransition.CHANGING);
                    previewContainer.setLayoutTransition(lt);
                }
            }
        }

        return preferenceView;
    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        updateView();
    }

    protected void updateView() {
        Settings settings = new Settings(getContext());
        int clockLayoutId = settings.getClockLayoutID(true);
        textViewPurchaseHint.setVisibility(showPurchaseHint(settings) ? View.VISIBLE : View.GONE);
        resetButton.setVisibility(showResetButton(settings) ? View.VISIBLE : View.GONE);
        updateClockLayout(clockLayoutId, settings);
        setupPreferencesFragment(clockLayoutId, settings);
        setupResetButton(clockLayoutId, settings);
    }

    private void updateClockLayout(int clockLayoutId, Settings settings) {
        clockLayout.setLayout(clockLayoutId);
        clockLayout.setBackgroundColor(Color.TRANSPARENT);
        clockLayout.setTypeface(settings.typeface);
        int color = previewMode == PreviewMode.DAY ? settings.clockColor : settings.clockColorNight;
        clockLayout.setPrimaryColor(color, settings.glowRadius, color);
        clockLayout.setSecondaryColor(previewMode == PreviewMode.DAY ? settings.secondaryColor : settings.secondaryColorNight);

        clockLayout.setDateFormat(settings.dateFormat);
        clockLayout.setTimeFormat(settings.getTimeFormat(), settings.is24HourFormat());
        clockLayout.setShowDivider(settings.showDivider);
        clockLayout.setMirrorText(settings.clockLayoutMirrorText);
        clockLayout.setScaleFactor(1.f);
        clockLayout.showDate(settings.showDate);

        clockLayout.setTemperature(settings.showTemperature, settings.temperatureUnit);
        clockLayout.setWindSpeed(settings.showWindSpeed, settings.speedUnit);
        clockLayout.showWeather(settings.showWeather);

        WeatherEntry entry = getWeatherEntry(settings);
        clockLayout.update(entry);

        Utility utility = new Utility(getContext());
        Point size = utility.getDisplaySize();
        Configuration config = context.getResources().getConfiguration();
        clockLayout.updateLayout(
            size.x - preferenceView.getPaddingLeft() - preferenceView.getPaddingRight(),
            config
        );

        clockLayout.requestLayout();
        clockLayout.invalidate();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setupPreferencesFragment(final int clockLayoutID, final Settings settings) {
        preferencesContainer.removeAllViews();
        if (clockLayoutID == ClockLayout.LAYOUT_ID_DIGITAL) {
            CustomDigitalClockPreferencesLayout prefs =
                    new CustomDigitalClockPreferencesLayout(context, settings);
            prefs.setIsPurchased(settings.purchasedWeatherData);
            prefs.setOnConfigChangedListener(
                    new CustomDigitalClockPreferencesLayout.OnConfigChangedListener() {
                        @Override
                        public void onConfigChanged() {
                            updateView();
                        }

                        @Override
                        public void onPurchaseRequested() {
                            ((PreferencesActivity) context).showPurchaseDialog();
                        }
                    }
            );
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            preferencesContainer.addView(prefs, lp);
        } else if (clockLayoutID == ClockLayout.LAYOUT_ID_ANALOG2 ||
                clockLayoutID == ClockLayout.LAYOUT_ID_ANALOG3 ||
                clockLayoutID == ClockLayout.LAYOUT_ID_ANALOG4) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                // the view is not drawn correctly. We have issues with invalidation.
                return;
            }
            AnalogClockConfig.Style preset = AnalogClockConfig.toClockStyle(clockLayoutID);
            CustomAnalogClockPreferencesLayout prefs =
                    new CustomAnalogClockPreferencesLayout(context, preset);

            prefs.setIsPurchased(settings.purchasedWeatherData);
            prefs.setOnConfigChangedListener(
                    new CustomAnalogClockPreferencesLayout.OnConfigChangedListener() {
                        @Override
                        public void onConfigChanged() {
                            updateClockLayout(clockLayoutID, settings);
                        }

                        @Override
                        public void onPurchaseRequested() {
                            ((PreferencesActivity) context).showPurchaseDialog();
                        }
                    }
            );
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            preferencesContainer.addView(prefs, lp);
        }
    }

    private void setupResetButton(final int clockLayoutID, final Settings settings) {
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getContext();
                Resources res = context.getResources();
                new AlertDialog.Builder(context)
                        .setTitle(res.getString(R.string.confirm_reset))
                        .setMessage(res.getString(R.string.confirm_reset_question_layout))
                        .setNegativeButton(android.R.string.no, null)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                AnalogClockConfig.Style preset = AnalogClockConfig.toClockStyle(clockLayoutID);
                                AnalogClockConfig config = new AnalogClockConfig(getContext(), preset);
                                config.reset();
                                updateView();
                            }
                        }).show();
            }
        });

    }

    private WeatherEntry getWeatherEntry(Settings settings) {
        WeatherEntry entry = settings.weatherEntry;
        if ( entry.timestamp ==  -1L) {
            entry.setFakeData();
        }
        return entry;
    }

    private boolean showPurchaseHint(Settings settings) {
        return (!settings.purchasedWeatherData && settings.getClockLayoutID(true) > 1);
    }

    private boolean showResetButton(Settings settings) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return false;
        }

        return settings.getClockLayoutID(true) > 1;
    }

    public enum PreviewMode {DAY, NIGHT}
}
