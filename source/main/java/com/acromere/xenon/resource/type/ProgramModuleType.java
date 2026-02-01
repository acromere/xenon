package com.acromere.xenon.resource.type;

import com.acromere.xenon.Xenon;
import com.acromere.xenon.XenonProgramProduct;
import com.acromere.xenon.resource.Resource;
import com.acromere.xenon.resource.ResourceType;
import com.acromere.xenon.resource.Codec;
import com.acromere.xenon.resource.PlaceholderCodec;
import com.acromere.xenon.scheme.XenonScheme;

@Deprecated
public class ProgramModuleType extends ResourceType {

	private static final String uriPattern = XenonScheme.ID + ":/modules";

	public static final java.net.URI URI = java.net.URI.create( uriPattern );

	public ProgramModuleType( XenonProgramProduct product ) {
		super( product, "product" );

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

	@Override
	public boolean assetOpen( Xenon program, Resource resource ) {
		resource.setModel( program.getCard() );
		return true;
	}

}
