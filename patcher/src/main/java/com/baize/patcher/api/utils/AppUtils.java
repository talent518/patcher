package com.baize.patcher.api.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.baize.patcher.api.Updater;

import java.io.File;

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

    /**
     * 调用.so库中的方法,合并apk
     *
     * @param old    旧Apk地址
     * @param newapk 新apk地址(名字)
     * @param patch  增量包地址
     */
    public static native void patcher(String old, String newapk, String patch);
}
