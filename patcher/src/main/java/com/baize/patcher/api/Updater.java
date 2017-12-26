package com.baize.patcher.api;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.baize.patcher.api.utils.ApkInfoTool;
import com.baize.patcher.api.utils.AppUtils;
import com.baize.patcher.api.utils.JsonUtil;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

import javax.net.ssl.SSLException;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.conn.ssl.AbstractVerifier;
import cz.msebera.android.httpclient.conn.ssl.SSLContexts;
import cz.msebera.android.httpclient.conn.ssl.SSLSocketFactory;

public class Updater {
    private static final String TAG = Updater.class.getName();
    private static final String sslHostVerifer = "espacetime.com";
    private static SSLSocketFactory _sslSocketFactory = new SSLSocketFactory(SSLContexts.createDefault(), new AbstractVerifier() {
        @Override
        public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
            Log.v("SSL", "host = " + JsonUtil.encode(host) + ", cns = " + JsonUtil.encode(cns) + ", subjectAlts = " + JsonUtil.encode(subjectAlts));
            if (cns != null && cns.length > 0) {
                for (String cn : cns) {
                    if (cn.substring(cn.indexOf(".") + 1).equals(sslHostVerifer)) {
                        return;
                    }
                }
            }
            if (subjectAlts != null && subjectAlts.length > 0) {
                for (String cn : subjectAlts) {
                    if (cn.substring(cn.indexOf(".") + 1).equals(sslHostVerifer)) {
                        return;
                    }
                }
            }

            throw new SSLException("SSL主机名验证不通过");
        }
    });
    private static Context context = null;
    private static String fileMd5;

    public static void init(Context _context) {
        context = _context;

        File file = new File(context.getPackageCodePath());
        fileMd5 = md5(file);
        Log.v(TAG, "MD5: " + fileMd5 + "\nfile: " + file.getAbsolutePath());

        FileDownloadLog.NEED_LOG = true;
        FileDownloader.init(context);
    }

    public static void checkUpdater(final Activity activity, final String url, final CheckUpdateCallback callback) {
        final AsyncHttpClient client = new AsyncHttpClient();

        client.setTimeout(30000); // 超时30秒
        client.setSSLSocketFactory(_sslSocketFactory);

        client.setUserAgent("Android/" + Build.VERSION.RELEASE + "(SDK/" + Build.VERSION.SDK_INT + ") Model(" + Build.MODEL + ")" + " API/1.0");

        final AsyncHttpResponseHandler handler = new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] _headers, byte[] responseBody) {
                String body = bodyToString(responseBody);
                Log.d(TAG, "onSuccess: \nstatusCode: " + statusCode + "\nheaders: " + headersToString(_headers) + "\nbody: " + body);

                final CheckDao dao = JsonUtil.decode(body, CheckDao.class);

                if (callback != null) {
                    callback.success(dao);
                    return;
                }

                if (!dao.isNeedUpdate()) {
                    // 无需更新
                    return;
                }

                final BaseDownloadTask downloadTask = FileDownloader.getImpl().create(dao.getDownloadUrl());
                final ProgressDialog progressDialog = new ProgressDialog(activity);
                progressDialog.setMax(100);
                progressDialog.setTitle("正在下载 " + dao.getFileName());
                progressDialog.setMessage("0%");
                progressDialog.setCancelable(true);
                progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        downloadTask.pause();
                    }
                });
                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                downloadTask.setPath(path.getAbsolutePath() + File.separator + dao.getFileName());
                Log.v(TAG, "download: " + downloadTask.getTargetFilePath());
                downloadTask.setListener(new FileDownloadListener() {
                    @Override
                    protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                        progress(task, soFarBytes, totalBytes);
                        Log.v(TAG, "download pending: soFarBytes = " + soFarBytes + ", totalBytes = " + totalBytes);
                    }

                    @Override
                    protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                        double n = totalBytes > 0 ? ((double) soFarBytes / (double) totalBytes) * 100.0 : soFarBytes % 100;
                        String msg = String.format("%.1f%%", n);
                        progressDialog.setProgress((int) n);
                        progressDialog.setMessage(msg);
                        Log.v(TAG, "download progress: soFarBytes = " + soFarBytes + ", totalBytes = " + totalBytes + ", msg = " + msg);
                    }

                    @Override
                    protected void completed(BaseDownloadTask task) {
                        progressDialog.setProgress(100);
                        progressDialog.setMessage("100%");
                        Log.v("download: completed", task.getTargetFilePath());
                        progressDialog.cancel();

                        File file = new File(task.getTargetFilePath());

                        String err = null;
                        if (!md5(file).equals(dao.getMd5())) {
                            err = "下载的文件MD5不一致";
                            if (callback != null) {
                                callback.error(err);
                            }
                            Log.d(TAG, err);
                            return;
                        }
                        if (!dao.isPatch()) {
                            AppUtils.installApplication(activity, file);
                            return;
                        }

                        if (!md5(new File(context.getPackageCodePath())).equals(dao.getOldMd5())) {
                            err = "旧版本的文件MD5不一致";
                            if (callback != null) {
                                callback.error(err);
                            }
                            Log.d(TAG, err);
                            return;
                        }

                        String fileName = file.getAbsolutePath();
                        File newFile = new File(fileName.substring(0, fileName.length() - 5) + "apk");
                        if (!newFile.exists()) {
                            AppUtils.patcher(context.getPackageCodePath(), newFile.getAbsolutePath(), file.getAbsolutePath());
                        }
                        if (!md5(newFile).equals(dao.getNewMd5())) {
                            err = "新版本的文件MD5不一致";
                            if (callback != null) {
                                callback.error(err);
                            }
                            Log.d(TAG, err);
                            return;
                        }

                        AppUtils.installApplication(activity, newFile);
                    }

                    @Override
                    protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                        Log.v(TAG, "download paused: soFarBytes = " + soFarBytes + ", totalBytes = " + totalBytes);
                    }

                    @Override
                    protected void error(final BaseDownloadTask task, Throwable e) {
                        Log.d(TAG, "download  error", e);

                        new AlertDialog.Builder(activity).setTitle("下载提示").setMessage("下载失败：" + e.getStackTrace().toString()).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                task.start();
                            }
                        }).setPositiveButton("返回", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                progressDialog.cancel();
                            }
                        }).show();
                    }

                    @Override
                    protected void warn(BaseDownloadTask task) {
                        Log.v(TAG, "download warn");
                    }
                });
                progressDialog.show();
                downloadTask.start();
            }

            @Override
            public void onFailure(int statusCode, Header[] _headers, byte[] responseBody, Throwable error) {
                Log.d(TAG, "onFailure: \nstatusCode: " + statusCode + "\nheaders: " + headersToString(_headers) + "\nbody: " + bodyToString(responseBody), error);

                if (callback != null) {
                    callback.failure(statusCode, _headers, responseBody, error);
                }
            }

            private String headersToString(Header[] _headers) {
                if (_headers == null || _headers.length == 0) {
                    return "[]";
                }

                StringBuffer sb = new StringBuffer();
                sb.append("[\n");
                for (Header header : _headers) {
                    sb.append("    ");
                    sb.append(header.getName());
                    sb.append(": ");
                    sb.append(header.getValue());
                    sb.append("\n");
                }
                sb.append("]");

                return sb.toString();
            }

            private String bodyToString(byte[] body) {
                try {
                    return new String(body, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    return new String(body);
                }
            }

        };

        client.addHeader("Package-Name", context.getPackageName());
        client.addHeader("Version-Name", ApkInfoTool.getVersionName(context));

        client.addHeader("MD5", fileMd5);

        Log.v(TAG, url);
        client.get(url, handler);
    }

    public static String md5(File file) {
        FileInputStream in = null;
        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        final byte[] buffer = new byte[32 * 1024];
        int len;
        try {
            MessageDigest mdTemp = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer)) != -1) {
                mdTemp.update(buffer, 0, len);
            }
            in.close();
            byte[] md = mdTemp.digest();

            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public interface CheckUpdateCallback {
        void success(CheckDao dao);

        void failure(int statusCode, Header[] _headers, byte[] responseBody, Throwable error);

        void error(String err);
    }

    public class CheckDao {
        private boolean isNeedUpdate;
        private String downloadUrl;
        private long fileSize;
        private String fileName;
        private String md5;
        private boolean isPatch;
        private String oldMd5, newMd5;

        public boolean isNeedUpdate() {
            return isNeedUpdate;
        }

        public void setNeedUpdate(boolean needUpdate) {
            isNeedUpdate = needUpdate;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public void setDownloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
        }

        public long getFileSize() {
            return fileSize;
        }

        public void setFileSize(long fileSize) {
            this.fileSize = fileSize;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

        public boolean isPatch() {
            return isPatch;
        }

        public void setPatch(boolean patch) {
            isPatch = patch;
        }

        public String getOldMd5() {
            return oldMd5;
        }

        public void setOldMd5(String oldMd5) {
            this.oldMd5 = oldMd5;
        }

        public String getNewMd5() {
            return newMd5;
        }

        public void setNewMd5(String newMd5) {
            this.newMd5 = newMd5;
        }

        @Override
        public String toString() {
            return JsonUtil.encode(this, true);
        }
    }

}
