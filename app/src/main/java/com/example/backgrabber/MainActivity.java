package com.example.backgrabber;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.material.snackbar.Snackbar;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {
    TextView oneText;
    Context context;
    private final  static int UPDATE_INTERVAL = 15000;  // Interval for running Handler here 1 minute
    private final Handler handler = new Handler();
    private final static int jobID = 123;
    private static final String TAG = "MainActivity";
    public static final String APPNAME = "BackGrabber" ;
    public static final String IsNewRun = "newRun";
    public static final String GameName = "GameName";
    public static final String ISGAME = "isGame";
    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;
    SharedPreferences sharedpreferences;
    Button cancelButton, startButton;
    LinearLayout linearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedpreferences = getSharedPreferences(APPNAME, Context.MODE_PRIVATE);
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        context = getApplicationContext();
        List<ResolveInfo> pkgAppsList = context.getPackageManager().queryIntentActivities( mainIntent, 0);
        oneText = findViewById(R.id.oneText);
        cancelButton = findViewById(R.id.cancelButton);
        startButton = findViewById(R.id.startButton);
        linearLayout = findViewById(R.id.masterll);
        //Something

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {


            //If the draw over permission is not available open the settings screen
            //to grant the permission.
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
        } else {

        }

        handler.post(runnable);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelJob();
            }
        });
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scheduleJob();
            }
        });


    }

    private void initializeView() {
        Intent floatingService = new Intent(context, FloatingViewService.class);
        startService(floatingService);
//                startService(new Intent(MainActivity.this, FloatingViewService.class));
//                finish();
    }

    private void stopFloatingService(){
        Intent serviceIntent = new Intent(context, FloatingViewService.class);
        stopService(serviceIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CODE_DRAW_OVER_OTHER_APP_PERMISSION) {
            //Check if the permission is granted or not.
            if (resultCode == 0) {
                initializeView();
            } else { //Permission is not available
                Toast.makeText(this,
                        "Draw over other app permission not available. Closing the application",
                        Toast.LENGTH_SHORT).show();

                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void startService(String gameName, String isGame){
        Intent serviceIntent = new Intent(context, ExampleService.class);
        serviceIntent.putExtra(GameName,gameName);
        serviceIntent.putExtra(ISGAME,isGame);
        startService(serviceIntent);
    }

    public void stopService(){
        Intent serviceIntent = new Intent(context, ExampleService.class);
        stopService(serviceIntent);
    }

    public void scheduleJob(){
        ComponentName componentName = new ComponentName(context, ExampleJobService.class);
        JobInfo info = new JobInfo.Builder(jobID,componentName)
                .setRequiresCharging(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setPersisted(true)
                .setPeriodic(15 * 60 * 1000)
                .build();

        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        int resultCode = scheduler.schedule(info);
        if(resultCode == JobScheduler.RESULT_SUCCESS){
            Log.d(TAG, "scheduleJob: Job scheduled");
        }else {
            Log.d(TAG, "scheduleJob: Job scheduling failed");
        }
    }

    public void cancelJob(){
        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        scheduler.cancel(jobID);
        Log.d(TAG, "scheduleJob: Job scheduling cancelled");
        stopService();
        stopFloatingService();
    }

    private Runnable runnable = new Runnable() { //The code which is repeated
        @Override
        public void run() {
            Log.d(TAG, "run:  gaming: On Thread for Game checking");
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            final SharedPreferences.Editor editor = sharedpreferences.edit();
            if(sharedpreferences.getInt(IsNewRun, 0)!=1){
                Snackbar snackbar = Snackbar
                        .make(linearLayout, "Please allow BackGrabber to usage access", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Allow", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                                startActivity(intent);
                                Toast.makeText(context, "Please allow BackGrabber access from the list.", Toast.LENGTH_SHORT).show();
                                editor.putInt(IsNewRun,1);
                                editor.commit();
                            }
                        });
                snackbar.show();
            }
            needPermissionForBlocking(context);
            printForegroundTask();
            handler.postDelayed(this, UPDATE_INTERVAL);

            scheduleJob();
        }
    };

    public static boolean needPermissionForBlocking(Context context){
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName);
            return  (mode != AppOpsManager.MODE_ALLOWED);
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
    }

    private String printForegroundTask() {
        String currentApp = "NULL";
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager usm = (UsageStatsManager)this.getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,  time - 1000*1000, time);
            if (appList != null && appList.size() > 0) {
                SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
                for (UsageStats usageStats : appList) {
                    mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                }
                if (mySortedMap != null && !mySortedMap.isEmpty()) {
                    currentApp = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                }
            }
        } else {
            ActivityManager am = (ActivityManager)this.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> tasks = am.getRunningAppProcesses();
            currentApp = tasks.get(0).processName;
        }

        Log.d("run: ", "Current App in foreground is: " + currentApp);
        boolean isGame = packageIsGame(context, currentApp);
        if(!currentApp.equals(context.getPackageName())){
            Toast.makeText(context, "Is It a Game? "+ isGame, Toast.LENGTH_SHORT).show();
            PackageManager packageManager = context.getPackageManager();
            try {
                String appName = (String) packageManager.getApplicationLabel(packageManager.getApplicationInfo(currentApp, PackageManager.GET_META_DATA));
                String isItAGame = String.valueOf(isGame);
                startService(appName, isItAGame);
                if(isGame){
                    initializeView();
                }
                else {
                    stopFloatingService();
                }

                oneText.setText("The game playing is : "+ currentApp);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        return currentApp;
    }

    public static boolean packageIsGame(Context context, String packageName) {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(packageName, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return info.category == ApplicationInfo.CATEGORY_GAME;
            } else {
                // We are suppressing deprecation since there are no other options in this API Level
                //noinspection deprecation
                return (info.flags & ApplicationInfo.FLAG_IS_GAME) == ApplicationInfo.FLAG_IS_GAME;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Util", "Package info not found for name: " + packageName, e);
            // Or throw an exception if you want
            return false;
        }
    }
}