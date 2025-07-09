package com.carrington.WIA.IO;

import java.util.List;

/**
 * A data structure to hold the results of a header-reading operation,
 * indicating success, the list of headers found, and any error messages.
 */
public class HeaderResult {

	private final boolean success;
	private final List<Header> headers;
	private final String errorMsg;

	/**
	 * Constructs a new header result
	 *
	 * @param headers  The list of {@link Header} objects found.
	 * @param errorMsg A string describing any errors encountered, or {@code null}
	 *                 if none.
	 * @param success  A boolean indicating if the header reading was successful.
	 */
	public HeaderResult(List<Header> headers, String errorMsg, boolean success) {
		this.headers = headers;
		this.success = success;
		this.errorMsg = errorMsg;
	}

	/**
	 * Checks if the header reading operation was successful.
	 *
	 * @return true if the operation was successful, otherwise false.
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * Gets the list of headers found during the operation.
	 *
	 * @return A list of {@link Header} objects, which may be null if the operation
	 *         failed.
	 */
	public List<Header> getHeaders() {
		return headers;
	}

	/**
	 * Gets the error message from the header reading operation.
	 *
	 * @return A string containing error details, or {@code null} if the operation
	 *         was successful.
	 */
	public String getErrors() {
		return errorMsg;
	}

}
