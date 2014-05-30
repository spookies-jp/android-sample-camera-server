package jp.co.spookies.android.cameraserver;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;

public class Controller extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.controller);
	}

	@Override
	public void onResume() {
		super.onResume();
		updateButtonStatus(getServerStatus());
	}

	public void onServiceStartButtonClicked(View view) {
		setServerStatus(true);
		updateButtonStatus(true);
		startService(new Intent(this, CameraServer.class));
	}

	public void onServiceStopButtonClicked(View view) {
		setServerStatus(false);
		updateButtonStatus(false);
		stopService(new Intent(this, CameraServer.class));
	}

	public void onCameraStartButtonClicked(View view) {
		startActivity(new Intent(this, CameraActivity.class));
	}

	public void updateButtonStatus(boolean serverStarted) {
		findViewById(R.id.start_button).setEnabled(!serverStarted);
		findViewById(R.id.stop_button).setEnabled(serverStarted);
		findViewById(R.id.camera_button).setEnabled(serverStarted);
	}

	private boolean getServerStatus() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		return prefs.getBoolean(getString(R.string.preference_name), false);
	}

	private void setServerStatus(boolean serverStarted) {
		Editor prefs = PreferenceManager.getDefaultSharedPreferences(this)
				.edit();
		prefs.putBoolean(getString(R.string.preference_name), serverStarted);
		prefs.commit();
	}
}
