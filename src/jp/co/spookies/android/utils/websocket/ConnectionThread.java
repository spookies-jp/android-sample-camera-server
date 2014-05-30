package jp.co.spookies.android.utils.websocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;

public abstract class ConnectionThread extends Thread {
	private static final String CRLF = "\r\n";
	private Socket socket = null;
	private InputStream inputStream = null;
	private PrintStream outputStream = null;
	private boolean continueFlag = true;
	private Object lockObject = new Object();

	public ConnectionThread(Socket socket) throws IOException {
		this.socket = socket;
		inputStream = socket.getInputStream();
		outputStream = new PrintStream(socket.getOutputStream());
	}

	@Override
	public void run() {
		try {
			byte[] temp = new byte[1000];
			int length = inputStream.read(temp);
			byte[] requestBytes = new byte[length];
			System.arraycopy(temp, 0, requestBytes, 0, length);
			HttpRequest request = new HttpRequest(requestBytes);
			if (request.isWSRequest()) {
				// WebSocketコネクションのとき
				handshake(request);
				onWSConnect();
				while (continueFlag) {
					onWSMessage(readMessage());
				}
			} else {
				// 通常のHTTPリクエストのとき
				HttpResponse response = onHttpRequest(request);
				outputStream.print("HTTP/1.0 " + response.getStatus() + CRLF);
				outputStream.print("MIME_version:1.0" + CRLF);
				outputStream.print("Content-Type:" + response.getContentType()
						+ CRLF);
				outputStream.print("Content-Length:"
						+ response.getContentLength() + CRLF);
				outputStream.print(CRLF);
				outputStream.write(response.getContent());
				outputStream.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			outputStream.close();
		}
	}

	public void send(byte[] data) throws IOException {
		if (data != null) {
			synchronized (lockObject) {
				outputStream.write(0x81);
				outputStream.write(0x7e);
				outputStream.write((data.length >> 8) & 0xff);
				outputStream.write(data.length & 0xff);
				outputStream.write(data);
				outputStream.flush();
			}
		}
	}

	public void disconnect() throws IOException {
		synchronized (lockObject) {
			outputStream.write(0x88);
			outputStream.write(0x00);
			outputStream.flush();
			socket.shutdownInput();
			continueFlag = false;
		}
	}

	public abstract HttpResponse onHttpRequest(HttpRequest request);

	public abstract void onWSConnect();

	public abstract void onWSDisconnect();

	public abstract void onWSMessage(byte[] data);

	private void handshake(HttpRequest request) throws IOException {
		outputStream.print("HTTP/1.1 101 Switching Protocols" + CRLF);
		outputStream.print("Upgrade: websocket" + CRLF);
		outputStream.print("Connection: Upgrade" + CRLF);
		outputStream.print("Sec-WebSocket-Accept: "
				+ new String(request.getWSAccept()));
		outputStream.print(CRLF);
		outputStream.flush();
	}

	private byte[] readMessage() {
		int b = 0;
		byte[] buf = new byte[1000];
		int index = 0;
		try {
			while (continueFlag) {
				b = inputStream.read();
				if (b == 0x00) {
					while ((b = inputStream.read()) != 0xFF) {
						buf[index++] = (byte) b;
					}
					break;
				} else if (b == -1) {
					continueFlag = false;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte[] res = new byte[index];
		for (int i = 0; i < index; i++) {
			res[i] = buf[i];
		}
		return res;
	}
}
