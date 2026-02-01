package com.acromere.xenon.resource;

import com.acromere.xenon.Xenon;
import com.acromere.xenon.scheme.BaseScheme;

public class MockScheme extends BaseScheme {

	public static final String ID = "mock";

	MockScheme( Xenon program ) {
		super( program, ID );
	}

	@Override
	public boolean exists( Resource resource ) {
		return true;
	}

	@Override
	public boolean canLoad( Resource resource ) {
		return true;
	}

	@Override
	public boolean canSave( Resource resource ) {
		return true;
	}

}
