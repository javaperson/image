/**
 * Copyright 2010 Casey Langen. All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * <p/>
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 * <p/>
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list
 * of conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY CASEY LANGEN ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL CASEY LANGEN OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * <p/>
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of Casey Langen.
 */

package com.github.javaperson.image;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Very basic digital clock widget. Contains an embedded Service that responds to a few
 * Intents that are broadcast by the system related to date and time. All real work,
 * including updating the widget, are managed by the Service.
 *
 * @author clangen
 */
public class ImageWidget extends AppWidgetProvider {
    static final String ACTION_UPDATE = "com.github.javaperson.image.UPDATE";

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        context.stopService(new Intent(context, UpdateService.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        context.startService(new Intent(ACTION_UPDATE));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        context.startService(new Intent(ACTION_UPDATE));
    }

    /**
     * Everything interesting happens in here. We setup a BroadcastReciever and listen
     * for the time change events, then update the views accordingly.
     *
     * @author clangen
     */
    public static final class UpdateService extends Service {

        private ExecutorService executorService = Executors.newSingleThreadExecutor();

        private long lastUpdateTimeMillis = 0L;

        private final static IntentFilter sIntentFilter;

        static {
            sIntentFilter = new IntentFilter();
            sIntentFilter.addAction(Intent.ACTION_TIME_TICK);
            sIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCreate() {
            super.onCreate();
            reinit();
            registerReceiver(mTimeTickReceiver, sIntentFilter);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onDestroy() {
            super.onDestroy();
            unregisterReceiver(mTimeTickReceiver);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            if (intent != null) {
                String action = intent.getAction();
                Log.v("onStartCommand", action);
                if (ACTION_UPDATE.equals(action)) {
                    update();
                }
            }
            return super.onStartCommand(intent, flags, startId);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        /**
         * make true current connect service is wifi
         *
         * @param mContext
         * @return
         */
        private static boolean isWifi(Context mContext) {
            ConnectivityManager connectivityManager = (ConnectivityManager) mContext
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetInfo != null
                    && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return true;
            }
            return false;
        }

        /**
         * Updates and redraws the Widget.
         */
        private boolean update() {
            long currentTimeMillis = System.currentTimeMillis();
            long interval = TimeUnit.MINUTES.toMillis(5);

            if (isWifi(this)) {
                interval = TimeUnit.SECONDS.toMillis(30);
            }

            if (currentTimeMillis - lastUpdateTimeMillis >= interval) {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

                if (pm.isScreenOn()) {
                    lastUpdateTimeMillis = currentTimeMillis;
                    final SharedPreferences preferences = getSharedPreferences(ImageWidgetConfiguration.PREFS_NAME, Context.MODE_PRIVATE);
                    final RemoteViews views = new RemoteViews(getPackageName(), R.layout.main);

                    final ComponentName widget = new ComponentName(this, ImageWidget.class);
                    final AppWidgetManager manager = AppWidgetManager.getInstance(this);
                    Log.v("Update", "SCREEN ON");
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            int[] appWidgetIds = manager.getAppWidgetIds(widget);
                            for (final int id : appWidgetIds) {
                                try {
                                    String url = preferences.getString(String.valueOf(id), "");

                                    if (!TextUtils.isEmpty(url)) {
                                        Bitmap bitmap = BitmapFactory.decodeStream((InputStream) new URL(url).getContent());
                                        views.setImageViewBitmap(R.id.img, bitmap);
                                        manager.updateAppWidget(id, views);
                                        Log.v("Update", "Image OK.");
                                    }
                                } catch (IOException e) {
                                    Log.v("Update", "Image cannot be loaded.", e);
                                }
                            }
                        }
                    });
                    return true;
                } else {
                    Log.v("Update", "SCREEN OFF");
                    return false;
                }
            }
            return false;
        }

        private void reinit() {
        }


        /**
         * Automatically registered when the Service is created, and unregistered
         * when the Service is destroyed.
         */
        private final BroadcastReceiver mTimeTickReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (update()) {
                    Log.v("Update", action);
                }
            }
        };
    }
}
