package com.acromere.xenon.resource.type;

import com.acromere.xenon.XenonProgramProduct;
import com.acromere.xenon.resource.ResourceType;
import com.acromere.xenon.resource.Codec;
import com.acromere.xenon.resource.PlaceholderCodec;
import com.acromere.xenon.scheme.XenonScheme;

@Deprecated
public class ProgramThemesType extends ResourceType {

	private static final String uriPattern = XenonScheme.ID + ":/themes";

	public static final java.net.URI URI = java.net.URI.create( uriPattern );

	public ProgramThemesType( XenonProgramProduct product ) {
		super( product, "themes" );

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
