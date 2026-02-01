package com.acromere.xenon.scheme;

import com.acromere.xenon.Xenon;

public class HttpsScheme extends HttpScheme {

	public static final String ID = "https";

	public HttpsScheme( Xenon program ) {
		super( program, ID );
	}

}
