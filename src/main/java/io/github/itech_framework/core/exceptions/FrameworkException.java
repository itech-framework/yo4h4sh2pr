package io.github.itech_framework.core.exceptions;

public class FrameworkException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 7238973558395813814L;

	public FrameworkException(String message) {
        super(message);
    }

    public FrameworkException(String message, Exception e) {
        super(message, e);
    }
}
