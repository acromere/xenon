package com.acromere.xenon.resource;

import com.acromere.product.Rb;
import com.acromere.xenon.RbKey;

public class AnyResourceFilter implements ResourceFilter {

	@Override
	public String getDescription() {
		return Rb.text( RbKey.LABEL, "all-assets" ) + " (*.*)";
	}

	@Override
	public boolean accept( Resource resource ) {
		return true;
	}

	@Override
	public String toString() {
		return getDescription();
	}
}
