package com.example.backgrabber;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;
import android.widget.Toast;

public class ExampleJobService extends JobService {
    private static final String TAG = "ExampleJobService";
    private boolean jobCancel = false;

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "onStartJob: Job Started");
        doBackGroudwork(params);
        return true;
    }

    private void doBackGroudwork(final JobParameters parameters){
        new Thread(new Runnable() {
            @Override
            public void run() {
                for(int i = 0; i<10; i++){
                    Log.d(TAG, "run: "+ i);
                    if(jobCancel){
                        return;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                Log.d(TAG, "run: Job Finished");
                jobFinished(parameters, false);
            }
        }).start();
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "onStopJob: Jobs Cancelled before completion");
        jobCancel = true;
        return true;
    }
}
