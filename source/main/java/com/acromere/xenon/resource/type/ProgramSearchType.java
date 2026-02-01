package com.acromere.xenon.resource.type;

import com.acromere.xenon.Xenon;
import com.acromere.xenon.XenonProgramProduct;
import com.acromere.xenon.resource.Resource;
import com.acromere.xenon.resource.ResourceType;
import com.acromere.xenon.resource.Codec;
import com.acromere.xenon.resource.PlaceholderCodec;
import com.acromere.xenon.resource.exception.ResourceException;
import com.acromere.xenon.scheme.XenonScheme;

public class ProgramSearchType extends ResourceType {

	private static final String mediaTypePattern = BASE_MEDIA_TYPE + ".index.search";

	private static final String uriPattern = XenonScheme.ID + ":/index-search";

	public static final java.net.URI URI = java.net.URI.create( uriPattern );

	public ProgramSearchType( XenonProgramProduct product ) {
		super( product, "index-search" );
		PlaceholderCodec codec = new PlaceholderCodec();
		codec.addSupported( Codec.Pattern.URI, uriPattern );
		codec.addSupported( Codec.Pattern.MEDIATYPE, mediaTypePattern );
		setDefaultCodec( codec );
	}

	@Override
	public boolean assetOpen( Xenon program, Resource resource ) throws ResourceException {
		return true;
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
