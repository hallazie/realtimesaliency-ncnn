package com.tencent.tnn.demo.StreamSaliencyPredictor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ToggleButton;

import com.tencent.tnn.demo.SaliencyPredictor;
import com.tencent.tnn.demo.FileUtils;
import com.tencent.tnn.demo.R;
import com.tencent.tnn.demo.common.component.CameraSetting;
import com.tencent.tnn.demo.common.component.DrawRectView;
import com.tencent.tnn.demo.common.fragment.BaseFragment;
import com.tencent.tnn.demo.common.sufaceHolder.DemoSurfaceHolder;

import java.io.IOException;


public class StreamSaliencyPredictFragment extends BaseFragment {

    private final static String TAG = StreamSaliencyPredictFragment.class.getSimpleName();

    /**********************************     Define    **********************************/

    private SurfaceView mPreview;

    private DrawRectView mDrawView;
    private int mCameraWidth;
    private int mCameraHeight;

    Camera mOpenedCamera;
    int mOpenedCameraId = 0;
    DemoSurfaceHolder mDemoSurfaceHolder = null;

    int mCameraFacing = -1;
    int mRotate = -1;
    SurfaceHolder mSurfaceHolder;

    private SaliencyPredictor mSaliencyPredictor = new SaliencyPredictor();
    private boolean mIsDetectingFace = false;

    private ToggleButton mGPUSwitch;
    private boolean mUseGPU = false;

    /**********************************     Get Preview Advised    **********************************/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        System.loadLibrary("tnn_wrapper");
        //start SurfaceHolder
        mDemoSurfaceHolder = new DemoSurfaceHolder(this);
    }

    private String initModel()
    {

        String targetDir =  getActivity().getFilesDir().getAbsolutePath();

        //copy detect model to sdcard
        String[] modelPathsDetector = {
                "fastsal.tnnmodel",
                "fastsal.tnnproto",
        };

        for(int i = 0; i < modelPathsDetector.length; i++) {
            String modelFilePath = modelPathsDetector[i];
            String interModelFilePath = targetDir + "/" + modelFilePath ;
            FileUtils.copyAsset(getActivity().getAssets(), "fastsal/"+modelFilePath, interModelFilePath);
        }
        return targetDir;
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.back_rl) {
            clickBack();
        }
    }

    private void clickBack() {
        if (getActivity() != null) {
            (getActivity()).finish();
        }
    }

    @Override
    public void setFragmentView() {
        Log.d(TAG, "setFragmentView");
        setView(R.layout.fragment_streamfacedetector);
        setTitleGone();
        $$(R.id.back_rl);
        init();
    }


    private void init() {
        mPreview = $(R.id.live_detection_preview);

        Button btnSwitchCamera = $(R.id.switch_camera);
        btnSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeCamera();
                if (mCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    openCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
                }
                else {
                    openCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
                }
                startPreview(mSurfaceHolder);
            }
        });

        mDrawView = (DrawRectView) $(R.id.drawView);

    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
        if (null != mDemoSurfaceHolder) {
            SurfaceHolder holder = mPreview.getHolder();
            holder.setKeepScreenOn(true);
            mDemoSurfaceHolder.setSurfaceHolder(holder);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        getFocus();
        preview();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    private void preview() {
        Log.i(TAG, "preview");

    }

    private void getFocus() {
        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                    clickBack();
                    return true;
                }
                return false;
            }
        });
    }

    /**********************************     Camera    **********************************/


    public void openCamera() {
        openCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
    }

    private void openCamera(int cameraFacing) {
        mIsDetectingFace = true;
        mCameraFacing = cameraFacing;
        try {
            int numberOfCameras = Camera.getNumberOfCameras();
            if (numberOfCameras < 1) {
                Log.e(TAG, "no camera device found");
            } else if (1 == numberOfCameras) {
                mOpenedCamera = Camera.open(0);
                mOpenedCameraId = 0;
            } else {
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                for (int i = 0; i < numberOfCameras; i++) {
                    Camera.getCameraInfo(i, cameraInfo);
                    if (cameraInfo.facing == cameraFacing) {
                        mOpenedCamera = Camera.open(i);
                        mOpenedCameraId = i;
                        break;
                    }
                }
            }
            if (mOpenedCamera == null) {
//                popTip("can't find camera","");
                Log.e(TAG, "can't find camera");
            }
            else {

                int r = CameraSetting.initCamera(getActivity().getApplicationContext(),mOpenedCamera,mOpenedCameraId);
                if (r == 0) {
                    //设置摄像头朝向
                    CameraSetting.setCameraFacing(cameraFacing);

                    Camera.Parameters parameters = mOpenedCamera.getParameters();
                    mRotate = CameraSetting.getRotate(getActivity().getApplicationContext(), mOpenedCameraId, mCameraFacing);
                    mCameraWidth = parameters.getPreviewSize().width;
                    mCameraHeight = parameters.getPreviewSize().height;
                    String modelPath = initModel();
                    int ret = mSaliencyPredictor.init(modelPath, mCameraWidth, mCameraHeight);
                    if (ret == 0) {
                        mIsDetectingFace = true;
                    } else {
                        mIsDetectingFace = false;
                        Log.e(TAG, "Face detector init failed " + ret);
                    }
                } else {
                    Log.e(TAG, "Failed to init camera");
                }
            }
        }
        catch (Exception e) {
            Log.e(TAG, "open camera failed:" + e.getLocalizedMessage());
        }
    }

    public void startPreview(SurfaceHolder surfaceHolder) {
        try {
            if (null != mOpenedCamera) {
                Log.i(TAG, "start preview, is previewing");
                mOpenedCamera.setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        if (mIsDetectingFace) {
                            Camera.Parameters mCameraParameters = camera.getParameters();
                            Bitmap outputMap = null;
                            int status = mSaliencyPredictor.predictFromStream(data, mCameraParameters.getPreviewSize().width, mCameraParameters.getPreviewSize().height, outputMap);

//                            mDrawView.addFaceRect(faceInfoList, mCameraParameters.getPreviewSize().height, mCameraParameters.getPreviewSize().width);

                        }
                        else {
                            Log.i(TAG,"No face");
                        }
                    }
                });
                mOpenedCamera.setPreviewDisplay(surfaceHolder);
                mOpenedCamera.startPreview();
                mSurfaceHolder = surfaceHolder;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public void closeCamera() {
        Log.i(TAG, "closeCamera");
        mIsDetectingFace = false;
        if (mOpenedCamera != null) {
            try {
                mOpenedCamera.stopPreview();
                mOpenedCamera.setPreviewCallback(null);
                Log.i(TAG, "stop preview, not previewing");
            } catch (Exception e) {
                e.printStackTrace();
                Log.i(TAG, "Error setting camera preview: " + e.toString());
            }
            try {
                mOpenedCamera.release();
                mOpenedCamera = null;
            } catch (Exception e) {
                e.printStackTrace();
                Log.i(TAG, "Error setting camera preview: " + e.toString());
            } finally {
                mOpenedCamera = null;
            }
        }
        mSaliencyPredictor.deinit();
    }

}
