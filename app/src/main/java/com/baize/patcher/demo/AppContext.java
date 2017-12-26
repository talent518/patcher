package com.baize.patcher.demo;

import android.app.Application;
import android.content.Context;

import com.baize.patcher.api.Updater;

/**
 * Created by abao on 2017/6/22.
 */

public class AppContext extends Application {
    public static Context context;

    @Override
    public void onCreate() {
        super.onCreate();

        context = this;

        Updater.init(this);
    }

}
