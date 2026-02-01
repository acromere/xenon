package com.acromere.xenon.resource.type;

import com.acromere.xenon.XenonProgramProduct;
import com.acromere.xenon.resource.ResourceType;
import com.acromere.xenon.resource.Codec;
import com.acromere.xenon.resource.PlaceholderCodec;
import com.acromere.xenon.scheme.XenonScheme;

public class ProgramResourceType extends ResourceType {

	private static final String uriPattern = XenonScheme.ID + ":/asset";

	public static final java.net.URI URI = java.net.URI.create( uriPattern );

	public static final String MODE_OPEN = "?mode=open";

	public static final String MODE_SAVE = "?mode=save";
	
	public static final java.net.URI OPEN_URI = java.net.URI.create( URI + MODE_OPEN );

	public static final java.net.URI SAVE_URI = java.net.URI.create( URI + MODE_SAVE );

	public ProgramResourceType( XenonProgramProduct product ) {
		super( product, "asset-open" );

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
