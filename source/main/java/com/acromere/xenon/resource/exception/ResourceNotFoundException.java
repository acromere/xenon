package com.acromere.xenon.resource.exception;

import com.acromere.xenon.resource.Resource;

public class ResourceNotFoundException extends ResourceException {

	public ResourceNotFoundException( Resource resource ) {
		this( resource, null );
	}

	public ResourceNotFoundException( Resource resource, Throwable cause ) {
		super( resource, "Resource not found", cause );
	}

}
