package com.acromere.xenon.resource.type;

import com.acromere.xenon.Xenon;
import com.acromere.xenon.XenonProgramProduct;
import com.acromere.xenon.resource.Resource;
import com.acromere.xenon.resource.ResourceType;
import com.acromere.xenon.resource.Codec;
import com.acromere.xenon.resource.exception.ResourceException;
import com.acromere.xenon.notice.NoticeModel;
import com.acromere.xenon.scheme.XenonScheme;
import lombok.CustomLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@CustomLog
public class ProgramNoticeType extends ResourceType {

	private static final String mediaTypePattern = BASE_MEDIA_TYPE + ".notice";

	private static final String uriPattern = XenonScheme.ID + ":/notice";

	public static final java.net.URI URI = java.net.URI.create( uriPattern );

	public ProgramNoticeType( XenonProgramProduct product ) {
		super( product, "notice" );
		setDefaultCodec( new ProgramNoticeCodec() );
	}

	@Override
	public boolean assetOpen( Xenon program, Resource resource ) throws ResourceException {
		resource.setModel( new NoticeModel() );
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

	private static class ProgramNoticeCodec extends Codec {

		public ProgramNoticeCodec() {
			addSupported( Pattern.URI, uriPattern );
			addSupported( Pattern.MEDIATYPE, mediaTypePattern );
		}

		@Override
		public String getKey() {
			return getClass().getName();
		}

		@Override
		public String getName() {
			return "Program Notice";
		}

		@Override
		public boolean canLoad() {
			return false;
		}

		@Override
		public boolean canSave() {
			return false;
		}

		@Override
		public void load( Resource resource, InputStream input ) throws IOException {
			NoticeModel notices = new NoticeModel();

			log.atTrace().log( "Load program notices..." );
			// TODO How do I want to store the notices? In settings? In a folder as separate files? As a single file?
			// TODO Remove old notices...

			notices.setModified( false );
			resource.setModel( notices );
		}

		@Override
		public void save( Resource resource, OutputStream output ) throws IOException {
			NoticeModel notices = resource.getModel();

			log.atTrace().log( "Save program notices..." );

			resource.setModified( false );
		}

	}

}
