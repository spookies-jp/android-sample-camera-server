package jp.co.spookies.android.utils.websocket;

public class HttpResponse {
	private byte[] content;
	private String contentType;
	private String status;

	public HttpResponse(byte[] content, String contentType, String status) {
		this.content = content;
		this.contentType = contentType;
		this.status = status;
	}

	public String getContentType() {
		return contentType;
	}

	public byte[] getContent() {
		return content;
	}

	public int getContentLength() {
		return getContent().length;
	}

	public String getStatus() {
		return status;
	}
}
