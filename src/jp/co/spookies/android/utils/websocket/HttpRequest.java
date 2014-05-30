package jp.co.spookies.android.utils.websocket;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import android.util.Base64;

public class HttpRequest {
	public static final char CR = '\r';
	public static final char LF = '\n';
	public static final String CRLF = "\r\n";
	public static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
	private String request = null;
	private Map<String, String> headers = null;
	private byte[] body = null;

	public HttpRequest(byte[] request) {
		int headerLength = 0;
		for (int i = 3; i < request.length; i++) {
			if (request[i - 3] == CR && request[i - 2] == LF
					&& request[i - 1] == CR && request[i] == LF) {
				headerLength = i + 1;
				break;
			}
		}
		body = new byte[request.length - headerLength];
		for (int i = 0; i < body.length; i++) {
			body[i] = request[headerLength + i];
		}
		List<String> lines = Arrays.asList(StringUtils.split(new String(
				request, 0, headerLength), CRLF));
		this.request = lines.get(0);
		List<String> header = lines.subList(1, lines.size());
		headers = new HashMap<String, String>();
		for (String line : header) {
			int separatorIndex = line.indexOf(':');
			String name = StringUtils.strip(line.substring(0, separatorIndex));
			String value = StringUtils
					.strip(line.substring(separatorIndex + 1));
			headers.put(name, value);
		}
	}

	public boolean isWSRequest() {
		return "websocket".equals(getHeaderValue("Upgrade"));
	}

	public String getRequest() {
		return request;
	}

	public String getHeaderValue(String key) {
		return headers.get(key);
	}

	public byte[] getBody() {
		return body;
	}

	public String getAction() {
		return StringUtils.split(request)[1];
	}

	public String getWSLocation() {
		return "ws://" + getHeaderValue("Host") + getAction();
	}

	public byte[] getWSAccept() {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA1");
			byte[] buf = (getHeaderValue("Sec-WebSocket-Key") + GUID)
					.getBytes();
			return Base64.encode(digest.digest(buf), Base64.DEFAULT);
		} catch (Exception e) {
			return null;
		}
	}
}
