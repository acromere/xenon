package com.acromere.xenon.resource.type;

import com.acromere.xenon.XenonProgramProduct;
import com.acromere.xenon.resource.ResourceType;
import com.acromere.xenon.resource.Codec;
import com.acromere.xenon.resource.PlaceholderCodec;
import com.acromere.xenon.scheme.FaultScheme;
import com.acromere.xenon.scheme.XenonScheme;

public class ProgramFaultType extends ResourceType {

	private static final String uriPattern = XenonScheme.ID + ":/fault";

	public static final java.net.URI URI = java.net.URI.create( uriPattern );

	public ProgramFaultType( XenonProgramProduct product ) {
		super( product, "fault" );

		PlaceholderCodec codec = new PlaceholderCodec();
		codec.addSupported( Codec.Pattern.SCHEME, FaultScheme.ID );
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
