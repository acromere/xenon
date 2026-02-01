package com.acromere.xenon.resource.type;

import com.acromere.xenon.XenonProgramProduct;
import com.acromere.xenon.resource.ResourceType;
import com.acromere.xenon.resource.Codec;
import com.acromere.xenon.resource.ContentCodec;
import com.acromere.xenon.scheme.XenonScheme;

public class ProgramHelpType extends ResourceType {

	private static final String uriPattern = XenonScheme.ID + ":/help";

	public static final java.net.URI URI = java.net.URI.create( uriPattern );

	public ProgramHelpType( XenonProgramProduct product ) {
		super( product, "help" );

		Codec codec = new ContentCodec();
		codec.addSupported( Codec.Pattern.URI, uriPattern );
		setDefaultCodec( codec );
	}

	@Override
	public String getKey() {
		return uriPattern;
	}

	@Override
	public boolean isUserType() {
		return false;
	}

}
