package model;

public class LogMessage {
	private String message;
	private String type; // "INFO", "WARN", "ERROR"

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public LogMessage(String message, String type) {
		super();
		this.message = message;
		this.type = type;
	}

	public LogMessage() {
		super();
	}

	
}
