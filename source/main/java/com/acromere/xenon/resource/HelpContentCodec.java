package com.acromere.xenon.resource;

import com.acromere.index.Document;
import com.acromere.product.Rb;
import com.acromere.util.IoUtil;
import com.acromere.util.TextUtil;
import com.acromere.xenon.XenonProgramProduct;
import lombok.CustomLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@CustomLog
public class HelpContentCodec extends Codec {

	private final XenonProgramProduct product;

	public HelpContentCodec( XenonProgramProduct product ) {
		this.product = product;
	}

	@Override
	public String getKey() {
		return "content";
	}

	@Override
	public String getName() {
		return "content";
	}

	@Override
	public boolean canLoad() {
		return true;
	}

	@Override
	public boolean canSave() {
		return false;
	}

	@Override
	public void load( Resource resource, InputStream input ) throws IOException {
		String content;
		if( input != null ) {
			content = IoUtil.toString( input, StandardCharsets.UTF_8 );
		} else {
			URI uri = resource.getUri();
			try {
				Document document = product.getProgram().getIndexService().lookupFromCache( uri );
				if( document == null ) {
					log.atWarn().log( "Document not found: doc=%s", uri );
					String message = Rb.text( "program", "help-document-not-found" );
					content = "<html><body>" + message + "</body></html>";
				} else {
					content = IoUtil.toString( document.reader() );
				}
			} catch( Exception exception ) {
				String message = Rb.text( "program", "error-loading-help-content" );
				content = "<html><body>" + message + "</body></html>";
				log.atError( exception ).log();
			}
		}

		if( TextUtil.isEmpty( content ) ) throw new IOException( "No content loaded!" );
		resource.setModel( content );
	}

	@Override
	public void save( Resource resource, OutputStream output ) {}

}
