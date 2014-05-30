package jp.co.spookies.android.cameraserver;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import jp.co.spookies.android.utils.websocket.ConnectionThread;
import jp.co.spookies.android.utils.websocket.HttpRequest;
import jp.co.spookies.android.utils.websocket.HttpResponse;
import jp.co.spookies.android.utils.websocket.WebSocketService;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;

public class CameraServer extends WebSocketService {
	private static final int PORT = 8080;

	public void onDestroy() {
		super.onDestroy();
		for (CameraConnection connection : wsConnections) {
			try {
				connection.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Editor prefs = PreferenceManager.getDefaultSharedPreferences(this)
					.edit();
			prefs.putBoolean(getString(R.string.preference_name), false);
			prefs.commit();
		}
	}

	@Override
	public int getPortNumber() {
		return PORT;
	}

	IBinder binder = new ICameraServer.Stub() {
		@Override
		public void broadcast(byte[] data) throws RemoteException {
			for (CameraConnection connection : wsConnections) {
				try {
					connection.send(data);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public ConnectionThread createConnection(Socket socket) {
		CameraConnection connection = null;
		try {
			connection = new CameraConnection(socket);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return connection;
	}

	private static Set<CameraConnection> wsConnections = new CopyOnWriteArraySet<CameraConnection>();

	public class CameraConnection extends ConnectionThread {

		public CameraConnection(Socket socket) throws IOException {
			super(socket);
		}

		@Override
		public HttpResponse onHttpRequest(HttpRequest request) {
			AssetManager manager = CameraServer.this.getAssets();
			HttpResponse response = null;
			try {
				String path = request.getAction().substring(1);
				InputStream stream = manager.open(path);
				byte[] file = new byte[stream.available()];
				stream.read(file);
				String mimeType;
				if ("bg_tile.png".equals(path)) {
					mimeType = "image/png";
				} else if ("frame_off.png".equals(path)) {
					mimeType = "image/png";
				} else if ("frame_ready.png".equals(path)) {
					mimeType = "image/png";
				} else if ("frame.png".equals(path)) {
					mimeType = "image/png";
				} else if ("favicon.ico".endsWith(path)) {
					mimeType = "image/png";
				} else {
					mimeType = "text/html";
				}
				response = new HttpResponse(file, mimeType, "200 OK");
			} catch (FileNotFoundException e) {
				String result = "404 Not Found";
				response = new HttpResponse(result.getBytes(), "text/plain",
						result);
			} catch (IOException e) {
				e.printStackTrace();
				String result = "500 Internal Server Error";
				response = new HttpResponse(result.getBytes(), "text/plain",
						result);
			}
			return response;
		}

		@Override
		public void onWSConnect() {
			wsConnections.add(this);
		}

		@Override
		public void onWSDisconnect() {
			wsConnections.remove(this);
		}

		@Override
		public void onWSMessage(byte[] data) {
		}
	}

}
