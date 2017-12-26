package com.baize.patcher.demo;

import android.app.Activity;
import android.os.Bundle;

import com.baize.patcher.api.Updater;

/**
 * 增量升级Demo主逻辑文件(客户端)
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Updater.checkUpdater(this, "http://resource.espacetime.com/iimupdater/check.php", null);
    }

}
