package com.acromere.xenon.resource.type;

import com.acromere.xenon.XenonProgramProduct;
import com.acromere.xenon.resource.Codec;
import com.acromere.xenon.resource.PlaceholderCodec;
import com.acromere.xenon.resource.ResourceType;
import com.acromere.xenon.scheme.XenonScheme;

import static com.acromere.xenon.resource.Scheme.SCHEME_SUFFIX;

public class ProgramGuideType extends ResourceType {

	private static final String uriPattern = XenonScheme.ID + SCHEME_SUFFIX + "guide";

	public static final java.net.URI URI = java.net.URI.create( uriPattern );

	public ProgramGuideType( XenonProgramProduct product ) {
		super( product, "guide" );

		PlaceholderCodec codec = new PlaceholderCodec();
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
