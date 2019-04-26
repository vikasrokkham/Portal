package com.vormittag.util;

public class SanityException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -659529765392882703L;

	public SanityException(String string, Exception e) {
		super(string, e);
	}

	public SanityException(String string) {
		super(string);
	}

}
