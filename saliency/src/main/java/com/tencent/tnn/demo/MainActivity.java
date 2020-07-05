package com.tencent.tnn.demo;


import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.tencent.tnn.demo.StreamFaceDetector.StreamFaceDetectFragment;
import com.tencent.tnn.demo.StreamSaliencyPredictor.StreamSaliencyPredictFragment;
import com.tencent.tnn.demo.common.activity.DemoBaseActivity;


public class MainActivity extends DemoBaseActivity {

    private static final String TAG = "Main";
    private static final int FACE_PERMISSION_QUEST_CAMERA = 1024;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.base_activity_layout);

        if (Build.VERSION.SDK_INT >= 23) {
            Log.d(TAG, "begin askForPermission the sdk version is" + Build.VERSION.SDK_INT);
            askForPermission();
        } else {
            Log.d(TAG, "no need to askForPermission the sdk version is" + Build.VERSION.SDK_INT);
            updateUI();
        }
    }

    public void updateUI() {
//        Fragment fragment = new StreamFaceDetectFragment();
        Log.d(TAG, "using stream SALIENCY !!!!!!!!!!!!");
        Fragment fragment = new StreamSaliencyPredictFragment();
        getFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).commit();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "Activity onResume");
        super.onResume();

    }

    @Override
    protected void onPause() {
        Log.d(TAG, "Activity onPause");
        super.onPause();

    }

    @Override
    protected void onStop() {
        Log.d(TAG, "Activity onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "Activity onDestroy");
        super.onDestroy();
    }

}
