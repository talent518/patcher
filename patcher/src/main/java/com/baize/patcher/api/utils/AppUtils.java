package com.baize.patcher.api.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.baize.patcher.api.Updater;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 应用程序的工具类
 *
 * @author Administrator
 */
public class AppUtils {
    static {
        // 调用.so文件,引入打包库
        System.loadLibrary("Patcher");
    }

    /**
     * 安装一个应用程序
     *
     * @param context
     * @param apkfile
     */
    public static void installApplication(Context context, File apkfile) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);

        Uri uri = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            uri = FileProvider.getUriForFile(context, "com.baize.patcher.fileprovider", apkfile);
        } else {
            uri = Uri.fromFile(apkfile);
        }
        Log.i(Updater.class.getName(), "Install: " + uri.getPath());
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

    public static int requestPermissions(Activity activity, String[] perms, int requestCode) {
        List<String> reqPerms = new ArrayList<String>();

        for (String perm : perms) {
            if (ContextCompat.checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED) {
                reqPerms.add(perm);
            }
        }

        if (reqPerms.size() > 0) {
            Log.i(Updater.class.getName(), reqPerms.toString());
            perms = new String[reqPerms.size()];
            ActivityCompat.requestPermissions(activity, reqPerms.toArray(perms), requestCode);
        }

        return reqPerms.size();
    }

    /**
     * 调用.so库中的方法,合并apk
     *
     * @param old    旧Apk地址
     * @param newapk 新apk地址(名字)
     * @param patch  增量包地址
     */
    public static native void patcher(String old, String newapk, String patch);
}
