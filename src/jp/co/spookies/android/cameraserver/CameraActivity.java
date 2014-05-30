package jp.co.spookies.android.cameraserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import jp.co.spookies.android.utils.SimpleCameraCallback;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

public class CameraActivity extends Activity {
	private final int INTERVAL = 300;
	private final int QUALITY = 30;
	ICameraServer mService = null;
	private ServiceConnection connection = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = ICameraServer.Stub.asInterface(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = new Intent(this, CameraServer.class);
		bindService(intent, connection, BIND_AUTO_CREATE);
		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
						| WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		SurfaceView view = new SurfaceView(this);
		setContentView(view);
		this.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);
		view.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		view.getHolder().addCallback(new CameraCallback(this));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unbindService(connection);
	}

	public void sendImage(byte[] image) {
		try {
			mService.broadcast(image);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	class CameraCallback extends SimpleCameraCallback {
		private byte[] mData = null;
		private Thread mThread = null;
		private Object lockObject = new Object();

		public CameraCallback(Context context) {
			super(context);
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			super.surfaceChanged(holder, format, width, height);
			mCamera.setPreviewCallback(new PreviewCallback() {
				public void onPreviewFrame(byte[] data, Camera camera) {
					mData = data;
				}
			});
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			super.surfaceCreated(holder);
			mThread = new CaptureThread();
			mThread.start();
			try {
				mCamera.setPreviewDisplay(holder);
			} catch (IOException e) {
				e.printStackTrace();
				CameraActivity.this.finish();
			}
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			mCamera.stopPreview();
			mCamera.setPreviewCallback(null);
			synchronized (lockObject) {
				mCamera.release();
				mCamera = null;
			}
			try {
				mThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			mThread = null;
			CameraActivity.this.finish();
		}

		class CaptureThread extends Thread {
			public void run() {
				while (mData == null) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				while (mCamera != null) {
					(new CompressThread(mData.clone())).start();
					try {
						Thread.sleep(INTERVAL);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			class CompressThread extends Thread {
				byte[] yuv;

				public CompressThread(byte[] yuv) {
					this.yuv = yuv;
				}

				@Override
				public void run() {
					byte[] data;
					synchronized (lockObject) {
						data = getPreviewImage(yuv);
					}
					sendImage(data);
				}
			}
		}

		/**
		 * プレビュー画像の取得
		 */
		private byte[] getPreviewImage(byte[] yuv) {
			if (yuv == null || mCamera == null) {
				return null;
			}
			Size size = mCamera.getParameters().getPreviewSize();
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			Base64OutputStream base64OutStream = new Base64OutputStream(
					outStream, Base64.DEFAULT);
			YuvImage yuvImage = new YuvImage(yuv, ImageFormat.NV21, size.width,
					size.height, null);
			Rect rect = new Rect(0, 0, size.width, size.height);
			yuvImage.compressToJpeg(rect, QUALITY, base64OutStream);
			return outStream.toByteArray();
		}
	}
}