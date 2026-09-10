package com.acromere.xenon.resource.type;

import com.acromere.index.Document;
import com.acromere.product.Product;
import com.acromere.util.IoUtil;
import com.acromere.util.TextUtil;
import com.acromere.xenon.Xenon;
import com.acromere.xenon.XenonProgramProduct;
import com.acromere.xenon.resource.Codec;
import com.acromere.xenon.resource.ContentCodec;
import com.acromere.xenon.resource.Resource;
import com.acromere.xenon.resource.ResourceType;
import com.acromere.xenon.resource.exception.ResourceException;
import com.acromere.xenon.scheme.XenonScheme;
import lombok.CustomLog;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;

import static com.acromere.xenon.resource.Scheme.SCHEME_SUFFIX;

@CustomLog
public class ProgramHelpType extends ResourceType {

	private static final String ID = "help";

	private static final String uriPattern = XenonScheme.ID + SCHEME_SUFFIX + ID;

	public static final java.net.URI URI = java.net.URI.create( uriPattern );

	public ProgramHelpType( XenonProgramProduct product ) {
		super( product, "help" );

		// Sample uri:
		// uri=xenon:/help:/com.acromere.carta/docs/manual/selecting-geometry

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

	public String getResourcePath( Resource resource ) {
		return resource.getUri().toString().substring( uriPattern.length() + SCHEME_SUFFIX.length() );
	}

	@Override
	public boolean resourceOpen( Xenon program, Resource resource ) throws ResourceException {
		try {
			// Get the resource path
			String path = getResourcePath( resource );
			log.atWarn().log( "resource path={0}", path );

			int index = path.indexOf( "/" );
			String productKey = path.substring( 0, index );
			String contentPrefix = path.substring( index );

			URL contentUrl = null;
			String localeSuffix = "_" + Locale.getDefault().toString();
			String contentPath = contentPrefix + localeSuffix + ".html";
			Product product = program.getProductManager().getProduct( productKey );

			// Check for locale-specific resources
			while( contentUrl == null && TextUtil.isNotEmpty( localeSuffix ) ) {
				contentUrl = product.getClass().getResource( contentPath );
				localeSuffix = localeSuffix.substring( 0, localeSuffix.lastIndexOf( "_" ) );
				contentPath = contentPrefix + localeSuffix + ".html";
			}

			// NEXT Should the help tool be filtering the content?
			// ...or, can we get the content from the index?
			//getProgram().getIndexService().lookupFromCache( contentUrl.toURI() );

			// The default resource
			if( contentUrl == null ) contentUrl = product.getClass().getResource( contentPath );

			if( contentUrl != null ) {
				resource.setModel( IoUtil.toString( contentUrl.openStream() ) );
			}
		} catch( IOException exception ) {
			throw new RuntimeException( exception );
		}

		return true;
	}

}
