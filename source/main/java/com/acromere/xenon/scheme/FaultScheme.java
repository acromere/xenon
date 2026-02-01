package com.acromere.xenon.scheme;

import com.acromere.xenon.Xenon;
import com.acromere.xenon.resource.Resource;
import com.acromere.xenon.resource.exception.ResourceException;

public class FaultScheme extends ProgramScheme {

	public static final String ID = "fault";

	public FaultScheme( Xenon program ) {
		super( program, ID );
	}

	@Override
	public boolean exists( Resource resource ) {
		return true;
	}

	@Override
	public boolean canLoad( Resource resource ) throws ResourceException {
		return true;
	}

}
