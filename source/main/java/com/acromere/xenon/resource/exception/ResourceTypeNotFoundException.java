package com.acromere.xenon.resource.exception;

import lombok.Getter;

@Getter
public class ResourceTypeNotFoundException extends RuntimeException {

	private final String resourceTypeKey;

	public ResourceTypeNotFoundException( String resourceTypeKey ) {
		this( resourceTypeKey, null );
	}

	public ResourceTypeNotFoundException( String resourceTypeKey, Throwable cause ) {
		super( "Resource type not found: " + resourceTypeKey, cause );
		this.resourceTypeKey = resourceTypeKey;
	}

}
