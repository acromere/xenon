package com.acromere.xenon.scheme;

import com.acromere.xenon.Xenon;
import com.acromere.xenon.resource.Resource;
import com.acromere.xenon.resource.Codec;
import com.acromere.xenon.resource.exception.ResourceException;

import java.io.IOException;

public abstract class ProgramScheme extends BaseScheme {

	public ProgramScheme( Xenon program, String id ) {
		super( program, id );
	}

	@Override
	public boolean exists( Resource resource ) throws ResourceException {
		return true;
	}

	@Override
	public void load( Resource resource, Codec codec ) throws ResourceException {
		if( codec != null ) {
			try {
				codec.load( resource, null );
			} catch( IOException exception ) {
				throw new ResourceException( resource, "Unable to load " + resource.getUri(), exception );
			}
		}
	}

	@Override
	public void save( Resource resource, Codec codec ) throws ResourceException {
		if( codec != null ) {
			try {
				codec.save( resource, null );
			} catch( IOException exception ) {
				throw new ResourceException( resource, "Unable to save asset", exception );
			}
		}
	}

}
