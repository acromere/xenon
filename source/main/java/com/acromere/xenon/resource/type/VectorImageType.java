package com.acromere.xenon.resource.type;

import com.acromere.xenon.XenonProgramProduct;
import com.acromere.xenon.resource.ResourceType;
import com.acromere.xenon.resource.Codec;
import com.acromere.xenon.resource.PlaceholderCodec;

/**
 * Represents a vector image.
 */
public class VectorImageType extends ResourceType {

	private static final String assetTypeKey = "image:vector";

	private static final String svgMediaTypePattern = "image/svg+xml";

	public VectorImageType( XenonProgramProduct product ) {
		super( product, "image" );

		// TODO Replace this placeholder codec with actual codecs
		PlaceholderCodec codec = new PlaceholderCodec();
		codec.addSupported( Codec.Pattern.EXTENSION, "svg" );
		setDefaultCodec( codec );
	}

	@Override
	public String getKey() {
		return assetTypeKey;
	}

}
