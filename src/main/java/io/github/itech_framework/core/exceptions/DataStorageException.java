package io.github.itech_framework.core.exceptions;

public class DataStorageException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6047485549813185039L;

	public DataStorageException(String message, Throwable cause) {
		super(message, cause);
	}

	public DataStorageException(String message) {
		super(message);
	}
}