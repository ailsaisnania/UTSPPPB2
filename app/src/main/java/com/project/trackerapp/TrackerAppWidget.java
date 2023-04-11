package com.project.trackerapp;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class TrackerAppWidget extends AppWidgetProvider {

    private int currentNumberStepsCount = 0;
    private float distanceTravelled = 0f;
    private int timerCount = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getIntExtra("current_steps", 0) != -1) {
            currentNumberStepsCount = intent.getIntExtra("current_steps", 0);
        }
        if (intent.getIntExtra("timer_counter", 0) != -1) {
            timerCount = intent.getIntExtra("timer_counter", 0);
        }
        if (intent.getFloatExtra("total_distance", 0f) != -1) {
            distanceTravelled = intent.getFloatExtra("total_distance", 0f);
        }
        super.onReceive(context, intent);
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId, int currentNumberStepsCount, int timerCounter, float distanceTravelled) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.tracker_app_widget);

        String currentNumberSteps = String.valueOf(new StringBuilder(currentNumberStepsCount + " Steps"));
        String distance = String.valueOf(new StringBuilder(distanceTravelled + " Meter"));
        String countTime = String.valueOf(new StringBuilder(timerCounter + " Seconds"));

        views.setTextViewText(R.id.tv_number_step, currentNumberSteps);
        views.setTextViewText(R.id.tv_total_distance, distance);
        views.setTextViewText(R.id.tv_average_pace, countTime);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, currentNumberStepsCount, timerCount, distanceTravelled);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}