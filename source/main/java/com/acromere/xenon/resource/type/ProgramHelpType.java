package com.acromere.xenon.resource.type;

import com.acromere.xenon.XenonProgramProduct;
import com.acromere.xenon.resource.Codec;
import com.acromere.xenon.resource.HelpContentCodec;
import com.acromere.xenon.resource.ResourceType;
import com.acromere.xenon.scheme.XenonScheme;
import lombok.CustomLog;

import static com.acromere.xenon.resource.Scheme.SCHEME_SUFFIX;

@CustomLog
public class ProgramHelpType extends ResourceType {

	private static final String ID = "help";

	private static final String uriPattern = XenonScheme.ID + SCHEME_SUFFIX + ID;

	public static final java.net.URI URI = java.net.URI.create( uriPattern );

	public ProgramHelpType( XenonProgramProduct product ) {
		super( product, "help" );

		Codec codec = new HelpContentCodec();
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
