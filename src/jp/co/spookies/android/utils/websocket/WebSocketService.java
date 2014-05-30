package jp.co.spookies.android.utils.websocket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import jp.co.spookies.android.cameraserver.Controller;
import jp.co.spookies.android.cameraserver.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

public abstract class WebSocketService extends Service {
	private NotificationManager mNM;
	private Server server = null;

	@Override
	public void onCreate() {
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		server = new Server();
		try {
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDestroy() {
		mNM.cancel(R.string.app_name);
		try {
			server.stop();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Notification notification = new Notification(
				R.drawable.notification_icon, getText(R.string.start_service),
				System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, Controller.class), 0);
		notification.setLatestEventInfo(this, getText(R.string.app_name),
				getIpAddress(), contentIntent);
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		mNM.notify(R.string.app_name, notification);
		return START_STICKY;
	}

	public abstract int getPortNumber();

	public abstract ConnectionThread createConnection(Socket socket);

	class Server implements Runnable {
		ServerSocket serverSocket = null;

		@Override
		public void run() {
			try {
				do {
					Socket socket = serverSocket.accept();
					ConnectionThread connection = createConnection(socket);
					connection.start();
				} while (serverSocket != null && !serverSocket.isClosed());
			} catch (IOException e) {
				e.printStackTrace();
				WebSocketService.this.stopSelf();
			}
		}

		public void start() throws IOException {
			serverSocket = new ServerSocket(getPortNumber());
			new Thread(this).start();
		}

		public void stop() throws IOException {
			serverSocket.close();
			serverSocket = null;
		}
	}

	private String getIpAddress() {
		WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		int ip = manager.getConnectionInfo().getIpAddress();
		return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "."
				+ ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
	}
}
