//package com.example.gaze.record.activity;
//
//import android.annotation.SuppressLint;
//import android.content.Intent;
//import android.opengl.GLES20;
//import android.opengl.GLSurfaceView;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.TextView;
//
//import com.example.gaze.record.R;
//import com.example.gaze.record.common.helpers.CameraPermissionHelper;
//import com.example.gaze.record.common.helpers.DisplayRotationHelper;
//import com.example.gaze.record.common.helpers.FullScreenHelper;
//import com.example.gaze.record.common.helpers.SnackbarHelper;
//import com.example.gaze.record.common.helpers.TrackingStateHelper;
//import com.example.gaze.record.common.rendering.BackgroundRenderer;
//import com.example.gaze.record.common.rendering.ObjectRenderer;
//import com.example.gaze.record.renderer.AugmentedFaceRenderer;
//import com.example.gaze.record.utils.Constants;
//import com.example.gaze.record.utils.SharedPreferencesUtils;
//import com.example.gaze.record.widget.ScalerMoveView;
//import com.google.ar.core.ArCoreApk;
//import com.google.ar.core.AugmentedFace;
//import com.google.ar.core.Camera;
//import com.google.ar.core.CameraConfig;
//import com.google.ar.core.CameraConfigFilter;
//import com.google.ar.core.Config;
//import com.google.ar.core.Config.AugmentedFaceMode;
//import com.google.ar.core.Frame;
//import com.google.ar.core.Session;
//import com.google.ar.core.TrackingState;
//import com.google.ar.core.exceptions.CameraNotAvailableException;
//import com.google.ar.core.exceptions.UnavailableApkTooOldException;
//import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
//import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
//import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
//import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
//
//import java.io.IOException;
//import java.text.DecimalFormat;
//import java.util.Collection;
//import java.util.EnumSet;
//import java.util.List;
//import java.util.concurrent.atomic.AtomicBoolean;
//
//import javax.microedition.khronos.egl.EGLConfig;
//import javax.microedition.khronos.opengles.GL10;
//
//public class FaceDistanceActivity extends BaseActivity implements GLSurfaceView.Renderer {
//    private static final String TAG = FaceDistanceActivity.class.getSimpleName();
//
//    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
//    private GLSurfaceView surfaceView;
//
//    private boolean installRequested;
//
//    private Session session;
//    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
//    private DisplayRotationHelper displayRotationHelper;
//    private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
//
//    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
//    private final AugmentedFaceRenderer augmentedFaceRenderer = new AugmentedFaceRenderer();
//    private final ObjectRenderer noseObject = new ObjectRenderer();
//    private final ObjectRenderer rightEarObject = new ObjectRenderer();
//    private final ObjectRenderer leftEarObject = new ObjectRenderer();
//
//    private TextView distanceTv;
//    private ScalerMoveView circleView;
//    private TextView tipTv;
//    private float[] distance;
//    private final DecimalFormat decimalFormat = new DecimalFormat("0.00");
//    private StringBuilder stringBuilder;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_face_distance);
//        surfaceView = findViewById(R.id.surfaceview);
//        Button btnRecord = findViewById(R.id.btn_record);
//        displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);
//
//        surfaceView.setPreserveEGLContextOnPause(true);
//        surfaceView.setEGLContextClientVersion(2);
//        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
//        surfaceView.setRenderer(this);
//        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
//        surfaceView.setWillNotDraw(false);
//
//        distanceTv = findViewById(R.id.distance_tv);
//        tipTv = findViewById(R.id.tip_tv);
//        installRequested = false;
//        btnRecord.setOnClickListener(view -> {
//            Intent intent = new Intent(this, ShowCalibrationActivity.class);
//            startActivity(intent);
//            finish();
//        });
//
//
//        circleView = findViewById(R.id.circle_view);
//        if (!SharedPreferencesUtils.getBoolean(Constants.CA_FRAME_STATE, false)) {
//            circleView.setVisibility(View.INVISIBLE);
//        }
//        float scale = SharedPreferencesUtils.getFloat(Constants.SCALE, -1f);
//        float pivotX = SharedPreferencesUtils.getFloat(Constants.PIVOT_X, -1f);
//        float pivotY = SharedPreferencesUtils.getFloat(Constants.PIVOT_Y, -1f);
//        if (pivotY != -1f && pivotX != -1f) circleView.setPivot(pivotX, pivotY);
//        if (scale != -1f) circleView.setScale(scale);
//
//        distance = new float[2];
//        stringBuilder = new StringBuilder();
//    }
//
//
//    @Override
//    protected void onDestroy() {
//        if (session != null) {
//            // Explicitly close ARCore Session to release native resources.
//            // Review the API reference for important considerations before calling close() in apps with
//            // more complicated lifecycle requirements:
//            // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
//            session.close();
//            session = null;
//        }
//
//        super.onDestroy();
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//
//        if (session == null) {
//            Exception exception = null;
//            String message = null;
//            try {
//                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
//                    case INSTALL_REQUESTED:
//                        installRequested = true;
//                        return;
//                    case INSTALLED:
//                        break;
//                }
//
//                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
//                // permission on Android M and above, now is a good time to ask the user for it.
//                if (!CameraPermissionHelper.hasCameraPermission(this)) {
//                    CameraPermissionHelper.requestCameraPermission(this);
//                    return;
//                }
//
//                // Create the session and configure it to use a front-facing (selfie) camera.
//                session = new Session(/* context= */ this, EnumSet.noneOf(Session.Feature.class));
//                CameraConfigFilter cameraConfigFilter = new CameraConfigFilter(session);
//                cameraConfigFilter.setFacingDirection(CameraConfig.FacingDirection.FRONT);
//                List<CameraConfig> cameraConfigs = session.getSupportedCameraConfigs(cameraConfigFilter);
//                if (!cameraConfigs.isEmpty()) {
//                    // Element 0 contains the camera config that best matches the session feature
//                    // and filter settings.
//                    session.setCameraConfig(cameraConfigs.get(0));
//                } else {
//                    message = "This device does not have a front-facing (selfie) camera";
//                    exception = new UnavailableDeviceNotCompatibleException(message);
//                }
//                configureSession();
//
//            } catch (UnavailableArcoreNotInstalledException
//                    | UnavailableUserDeclinedInstallationException e) {
//                message = "Please install ARCore";
//                exception = e;
//            } catch (UnavailableApkTooOldException e) {
//                message = "Please update ARCore";
//                exception = e;
//            } catch (UnavailableSdkTooOldException e) {
//                message = "Please update this app";
//                exception = e;
//            } catch (UnavailableDeviceNotCompatibleException e) {
//                message = "This device does not support AR";
//                exception = e;
//            } catch (Exception e) {
//                message = "Failed to create AR session";
//                exception = e;
//            }
//
//            if (message != null) {
//                messageSnackbarHelper.showError(this, message);
//                Log.e(TAG, "Exception creating session", exception);
//                return;
//            }
//        }
//
//        // Note that order matters - see the note in onPause(), the reverse applies here.
//        try {
//            session.resume();
//        } catch (CameraNotAvailableException e) {
//            messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
//            session = null;
//            return;
//        }
//
//        surfaceView.onResume();
//        displayRotationHelper.onResume();
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//        if (session != null) {
//            // Note that the order matters - GLSurfaceView is paused first so that it does not try
//            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
//            // still call session.update() and get a SessionPausedException.
//            displayRotationHelper.onPause();
//            surfaceView.onPause();
//            session.pause();
//        }
//    }
//
//    @Override
//    public void onPermissionsDeny() {
//        super.onPermissionsDeny();
//        if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
//            // Permission denied with checking "Do not ask again".
//            CameraPermissionHelper.launchPermissionSettings(this);
//        }
//    }
//
//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
//    }
//
//
//    @Override
//    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
//        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
//
//        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
//        try {
//            // Create the texture and pass it to ARCore session to be filled during update().
//            backgroundRenderer.createOnGlThread(/*context=*/ this);
//            augmentedFaceRenderer.createOnGlThread(this, "models/freckles.png");
//            augmentedFaceRenderer.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);
//            noseObject.createOnGlThread(/*context=*/ this, "models/nose.obj", "models/nose_fur.png");
//            noseObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);
//            noseObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);
//            rightEarObject.createOnGlThread(this, "models/forehead_right.obj", "models/ear_fur.png");
//            rightEarObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);
//            rightEarObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);
//            leftEarObject.createOnGlThread(this, "models/forehead_left.obj", "models/ear_fur.png");
//            leftEarObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);
//            leftEarObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);
//
//        } catch (IOException e) {
//            Log.e(TAG, "Failed to read an asset file", e);
//        }
//    }
//
//    @Override
//    public void onSurfaceChanged(GL10 gl, int width, int height) {
//        displayRotationHelper.onSurfaceChanged(width, height);
//        GLES20.glViewport(0, 0, width, height);
//
//    }
//
//    public AtomicBoolean ClientSet1 = new AtomicBoolean(false);
//    public AtomicBoolean ClientSet2 = new AtomicBoolean(true);
//
//    @SuppressLint("SetTextI18n")
//    @Override
//    public void onDrawFrame(GL10 gl) {
//        // Clear screen to notify driver it should not load any pixels from previous frame.
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
//
//        if (session == null) {
//            return;
//        }
//        // Notify ARCore session that the view size changed so that the perspective matrix and
//        // the video background can be properly adjusted.
//        displayRotationHelper.updateSessionIfNeeded(session);
//
//
//        try {
//            session.setCameraTextureName(backgroundRenderer.getTextureId());
//
//            // Obtain the current frame from ARSession. When the configuration is set to
//            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
//            // camera framerate.
//            Frame frame = session.update();
//            Camera camera = frame.getCamera();
//
//            // Get projection matrix.
//            float[] projectionMatrix = new float[16];
//            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f);
//
//            // Get camera matrix and draw.
//            float[] viewMatrix = new float[16];
//            camera.getViewMatrix(viewMatrix, 0);
//
//            // Compute lighting from average intensity of the image.
//            // The first three components are color scaling factors.
//            // The last one is the average pixel intensity in gamma space.
//            final float[] colorCorrectionRgba = new float[4];
//            //frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);
//
//            // If frame is ready, render camera preview image to the GL surface.
//            backgroundRenderer.draw(frame);
//
//            // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
//            trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());
//
//            // ARCore's face detection works best on upright faces, relative to gravity.
//            // If the device cannot determine a screen side aligned with gravity, face
//            // detection may not work optimally.
//            Collection<AugmentedFace> faces = session.getAllTrackables(AugmentedFace.class);
//
//            for (AugmentedFace face : faces) {
//                if (face.getTrackingState() != TrackingState.TRACKING) {
//                    break;
//                }
//
//                float scaleFactor = 1.0f;
//
//                float[] modelMatrix = new float[16];
//                face.getCenterPose().toMatrix(modelMatrix, 0);
//
//
//                // Face objects use transparency so they must be rendered back to front without depth write.
//                // GLES20.glDepthMask(false);
//
//                // Each face's region poses, mesh vertices, and mesh normals are updated every frame.
//
//                // 1. Render the face mesh first, behind any 3D objects attached to the face regions.
//
//                augmentedFaceRenderer.draw(
//                        projectionMatrix, viewMatrix, modelMatrix, colorCorrectionRgba, face, camera, distance);
////                stringBuilder.append("左眼视距: ");
////                stringBuilder.append(decimalFormat.format(distance[0]));
////                stringBuilder.append("cm\n");
////                stringBuilder.append("右眼视距: ");
////                stringBuilder.append(decimalFormat.format(distance[1]));
////                stringBuilder.append("cm");
////                distanceTv.setText(stringBuilder.toString());
//                stringBuilder.setLength(0);
////                if (Math.abs(distance[0] - distance[1]) > 2) {
////                    tipTv.setTextColor(this.getResources().getColor(R.color.red));
////                    tipTv.setText("请人脸正对屏幕，不要倾斜");
////                }
//
////                if (distance[0] >= 30 && distance[1] >= 30 && distance[0] <= 40 && distance[1] <= 40) {
////                    tipTv.setTextColor(this.getResources().getColor(R.color.green));
////                    tipTv.setText("距离刚刚好！");
////                } else if (distance[0] < 30 || distance[1] < 30) {
////                    tipTv.setTextColor(this.getResources().getColor(R.color.red));
////                    tipTv.setText("距离屏幕太近了");
////                } else if (distance[0] > 40 || distance[1] > 40) {
////                    tipTv.setTextColor(this.getResources().getColor(R.color.red));
////                    tipTv.setText("距离屏幕太远了");
////                }
//            }
//
////            if (faces.size() == 0) {
////                distanceTv.setText("未检测到人脸");
////                tipTv.setTextColor(this.getResources().getColor(R.color.red));
////                tipTv.setText("未发现人脸！");
////            }
//
//        } catch (Throwable t) {
//            // Avoid crashing the application due to unhandled exceptions.
//            Log.e(TAG, "Exception on the OpenGL thread", t);
//        } finally {
//            GLES20.glDepthMask(true);
//        }
//    }
//
//    private void configureSession() {
//        Config config = new Config(session);
//        config.setAugmentedFaceMode(AugmentedFaceMode.MESH3D);
//        session.configure(config);
//    }
//
//}