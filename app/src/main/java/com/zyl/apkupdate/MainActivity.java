package com.zyl.apkupdate;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.zyl.apkupdate.util.HttpsUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private long downloadId;
    DownloadManager downloadManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.az).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                installAPK();
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                String result = HttpsUtil.get("https://bbs.aoshitang.com/andVersion.htm");
                Log.i(MainActivity.class.getSimpleName(), result);
                if (result == null){
                    return;
                }
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    String url = jsonObject.optString("url");
                    String version = jsonObject.optString("version");
                    Uri uri = Uri.parse(url);
                    DownloadManager.Request request = new DownloadManager.Request(uri);
                    request.setDestinationInExternalFilesDir(getApplicationContext(), Environment.DIRECTORY_DOWNLOADS, "astbbs"+ version + ".apk");
                    //设置Notification的标题和描述
                    request.setTitle("版本更新");
                    request.setDescription(version);
                    //设置Notification的显示，和隐藏。
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    downloadManager = (DownloadManager) getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
                    downloadId = downloadManager.enqueue(request);
                    Log.i(MainActivity.class.getSimpleName(), "" + downloadId);
                    getApplicationContext().registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
//                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//                    startActivity(intent);
                } catch (JSONException e) {
                    Log.i(MainActivity.class.getSimpleName(), e.getMessage(), e);
                }
            }

            private BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    checkStatus();
                }
            };
        }){

        }.start();

    }

    private void checkStatus() {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor c = downloadManager.query(query);
        if (c.moveToFirst()){
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            switch (status){
                case DownloadManager.STATUS_SUCCESSFUL:
                    installAPK();
                    break;
                case DownloadManager.STATUS_FAILED:
                    Toast.makeText(getApplicationContext(), "下载失败", Toast.LENGTH_SHORT).show();
                    break;

            }
        }
    }

    private void installAPK() {
        Uri downloadFileUri = downloadManager.getUriForDownloadedFile(downloadId);
        if (downloadFileUri != null) {
            Intent intent= new Intent(Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
                // 由于没有在Activity环境下启动Activity,设置下面的标签
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                //添加这一句表示对目标应用临时授权该Uri所代表的文件
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            intent.setDataAndType(downloadFileUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
}
