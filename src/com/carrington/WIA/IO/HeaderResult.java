package com.carrington.WIA.IO;

import java.util.List;

public class HeaderResult {

	public final boolean success;
	public final List<Header> headers;
	public final String errorMsg;

	public HeaderResult(List<Header> headers, String errorMsg, boolean success) {
		this.headers = headers;
		this.success = success;
		this.errorMsg = errorMsg;
	}

}
