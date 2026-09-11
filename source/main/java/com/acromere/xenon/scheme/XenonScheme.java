package com.acromere.xenon.scheme;

import com.acromere.xenon.Xenon;
import com.acromere.xenon.resource.Resource;
import com.acromere.xenon.resource.exception.ResourceException;

public class XenonScheme extends ProgramScheme {

	public static final String ID = "xenon";

	public XenonScheme( Xenon program ) {
		super( program, ID );
	}

	@Override
	public boolean canLoad( Resource resource ) throws ResourceException {
		return true;
	}

}
