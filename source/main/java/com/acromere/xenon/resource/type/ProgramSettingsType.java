package com.acromere.xenon.resource.type;

import com.acromere.xenon.ProgramSettings;
import com.acromere.xenon.Xenon;
import com.acromere.xenon.XenonProgramProduct;
import com.acromere.xenon.resource.Resource;
import com.acromere.xenon.resource.ResourceType;
import com.acromere.xenon.resource.Codec;
import com.acromere.xenon.resource.PlaceholderCodec;
import com.acromere.xenon.scheme.XenonScheme;

public class ProgramSettingsType extends ResourceType {

	private static final String uriPattern = XenonScheme.ID + ":/settings";

	public static final java.net.URI URI = java.net.URI.create( uriPattern );

	public static final java.net.URI ADVANCED = java.net.URI.create( uriPattern + "#advanced" );

	public static final java.net.URI UPDATES = java.net.URI.create( uriPattern + "#modules-updates" );

	public ProgramSettingsType( XenonProgramProduct product ) {
		super( product, "settings" );

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
		resource.setModel( program.getSettingsManager().getSettings( ProgramSettings.PROGRAM ) );
		return true;
	}

}
