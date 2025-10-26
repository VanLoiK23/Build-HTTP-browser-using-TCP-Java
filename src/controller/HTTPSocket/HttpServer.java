package controller.HTTPSocket;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.function.Consumer;

import model.LogMessage;

public class HttpServer implements Runnable {

	private static final File WEB_ROOT = new File("src/resource");
	private static final String DEFAULT_FILE = "index.html";
	private static final String NOT_FOUND = "404.html";
	private Socket connect;
	private BufferedReader in = null;
//	private PrintWriter out = null;
//	private BufferedOutputStream dataOut = null;
	private static Consumer<LogMessage> onLogging;

	public HttpServer(Socket socket) {
		try {
			connect = socket;
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
//			out = new PrintWriter(connect.getOutputStream());
//			dataOut = new BufferedOutputStream(connect.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		String fileRequested = null;
		// Đọc dòng request đầu tiên: ví dụ "GET /index.html HTTP/1.1"
		try {
			String inputLine = in.readLine();

			if (inputLine == null || inputLine.isEmpty())
				return;

			setLogging("Request: " + inputLine, "INFO");

			System.out.println("Request: " + inputLine);

			StringTokenizer tokenizer = new StringTokenizer(inputLine);
			String method = tokenizer.nextToken();
			fileRequested = tokenizer.nextToken();

			if (fileRequested.equals("/"))
				fileRequested = "/" + DEFAULT_FILE;
			File file = new File(WEB_ROOT, fileRequested);

			if (method.equals("GET") || method.equals("POST")) {
				if (file.exists()) {
					byte[] data = readFile(file);
					sendResponse("200 OK", "text/html", data, data.length);
					setLogging("Reponse: " + " status 200 OK, Type content: text/html "+data[0], "INFO");
				} else {
					byte[] data = readFile(new File(WEB_ROOT, "/" + NOT_FOUND));
					sendResponse("404 Not Found", "text/html", data, data.length);
					setLogging("Reponse: " + " status 404 Not Found, Type content: text/html ", "WARNING");
				}
			} else if (method.equals("HEAD")) {
				if (file.exists()) {
					byte[] data = readFile(file);
					sendResponse("200 OK", "text/html", new byte[0], data.length);
					setLogging("Reponse: " + " status 200 OK, Type content: text/html "+data[0], "INFO");
				} else {
					sendResponse("404 Not Found", "text/html", new byte[0], 0);
					setLogging("Reponse: " + " status 404 Not Found, Type content: text/html ", "WARNING");
				}
			} else {
				String notSupported = "<h1>501 Not Implemented</h1>";
				sendResponse("501 Not Implemented", "text/html", notSupported.getBytes(),
						notSupported.getBytes().length);
				setLogging("Reponse: " + " status 501 Not Implemented, Type content: text/html ", "WARNING");
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				in.close();
//				out.close();
//				dataOut.close();
				connect.close();
			} catch (Exception e) {
				System.err.println("Error closing stream: " + e.getMessage());
			}
		}

	}

	private void sendResponse(String status, String type, byte[] content, int length) throws IOException {
//		//PrintWritter
//		out.println("HTTP/1.1 " + status);
//		out.println("Server: SimpleJavaHTTP");
//		out.println("Date: " + new Date());
//		out.println("Content-Type: " + type);
//		out.println("Content-Length: " + length);
//		out.println();
//		out.flush();
//		//BufferOutputStream
//		dataOut.write(content, 0, content.length);
//		dataOut.flush();
		
		
		
		 OutputStream out = connect.getOutputStream();
		    String header = ""
		        + "HTTP/1.1 " + status + "\r\n"
		        + "Server: SimpleJavaHTTP\r\n"
		        + "Date: " + new Date() + "\r\n"
		        + "Content-Type: " + type + "\r\n"
		        + "Content-Length: " + length + "\r\n"
		        + "\r\n";
		    out.write(header.getBytes("UTF-8"));
		    out.write(content);
		    out.flush();
	}

	private static byte[] readFile(File file) throws IOException {
		return java.nio.file.Files.readAllBytes(file.toPath());
	}
	
//	private byte[] readFile(File file) throws IOException {
//		FileInputStream fileIn = null;
//		byte[] fileData = new byte[(int)file.length()];
//		
//		try {
//			fileIn= new FileInputStream(file);
//			fileIn.read(fileData);
//		}finally {
//			if(fileIn !=null)
//				fileIn.close();
//		}
//		return fileData;
//	}

	private void setLogging(String msg, String type) {
		if (onLogging != null) {
			LogMessage logMessage = new LogMessage(msg, type);

			onLogging.accept(logMessage);
		}
	}

	public static void setOnLogging(Consumer<LogMessage> consumer) {
		onLogging = consumer;
	}

}
