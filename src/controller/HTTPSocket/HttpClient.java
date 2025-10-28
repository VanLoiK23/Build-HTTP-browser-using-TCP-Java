package controller.HTTPSocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import model.HttpMessage;

public class HttpClient {

	private Socket socket;
//	private PrintWriter out;
//	private BufferedReader in;
//	private InputStream dataIn;

	private Consumer<String> headerHandler;
	private Consumer<String> bodyHandler;

	private Consumer<String> errHandler;

	public void setHeaderHandler(Consumer<String> headerHandler) {
		this.headerHandler = headerHandler;
	}

	public void setBodyHandler(Consumer<String> bodyHandler) {
		this.bodyHandler = bodyHandler;
	}

	public void setErrorHandler(Consumer<String> errHandler) {
		this.errHandler = errHandler;
	}

	public void sendUrlRequest(HttpMessage httpMessage) {
		try {
			socket = new Socket(httpMessage.getHost(), httpMessage.getPort());
			PrintWriter out = new PrintWriter(socket.getOutputStream());
//			InputStream dataIn = socket.getInputStream();
//			BufferedReader in = new BufferedReader(new InputStreamReader(dataIn));

			BufferedReader in = new BufferedReader(
					new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

			// Gửi HTTP Request
			out.println(httpMessage.getMethod() + " " + httpMessage.getPath() + " HTTP/1.1");
			out.println("Host: " + httpMessage.getHost());
			out.println("User-Agent: SimpleJavaBrowser");
			out.println("Connection: close");
			out.println();
			out.flush();

			// Đọc header
			StringBuilder headerBuilder = new StringBuilder();
			String line;
			Map<String, String> headers = new HashMap<>();

			while ((line = in.readLine()) != null && !line.isEmpty()) {
				headerBuilder.append(line).append("\n");

				// Tách header ra (vd: "Content-Length: 123")
				if (line.contains(":")) {
					int idx = line.indexOf(":");
					String key = line.substring(0, idx).trim();
					String value = line.substring(idx + 1).trim();
					headers.put(key, value);
				}
			}

			if (headerHandler != null) {
				headerHandler.accept(headerBuilder.toString());
			}

			System.out.println(headerBuilder.toString());

			// Lấy Content-Length để đọc phần body
			int contentLength = 0;
			if (headers.containsKey("Content-Length")) {
			    contentLength = Integer.parseInt(headers.get("Content-Length"));
			}

			char[] bodyChars = new char[contentLength];
			int totalRead = 0;
			while (totalRead < contentLength) {
			    int bytesRead = in.read(bodyChars, totalRead, contentLength - totalRead);
			    if (bytesRead == -1) break;
			    totalRead += bytesRead;
			}

			String body = new String(bodyChars)
				    .replaceAll("[\\p{C}]", "")       // loại bỏ ký tự điều khiển
				    .replaceAll("\\s+$", "")          // xóa khoảng trắng cuối
				    .trim();                          // xóa khoảng trắng đầu/cuối

			if (bodyHandler != null) {
				bodyHandler.accept(body);
			}
			System.out.println(body);

		} catch (UnknownHostException e) {
			System.err.println("❌ Không tìm thấy máy chủ: " + httpMessage.getHost());
			showError("Không thể kết nối: Sai địa chỉ host hoặc server chưa chạy!");
		} catch (ConnectException e) {
			System.err.println("❌ Server từ chối kết nối (port không mở hoặc server không chạy)");
			showError("Kết nối bị từ chối — hãy kiểm tra lại cổng hoặc khởi động server!");
		} catch (IOException e) {
			System.err.println("❌ Lỗi I/O khi gửi request: " + e.getMessage());
			showError("Đã xảy ra lỗi khi gửi yêu cầu đến server!");
		} finally {
			try {

				if (socket != null)
					socket.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	private void showError(String error) {
		if (errHandler != null) {
			errHandler.accept(error);
		}
	}
}
