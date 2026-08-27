package com.acromere.xenon.resource;

import com.acromere.event.EventType;

public class ResourceSwitchedEvent extends ResourceEvent {

	public static final EventType<ResourceSwitchedEvent> RESOURCE_SWITCHED = new EventType<>( ResourceEvent.ANY, "RESOURCE_SWITCHED" );

	public static final EventType<ResourceSwitchedEvent> ANY = RESOURCE_SWITCHED;

	public static final EventType<ResourceSwitchedEvent> SWITCHED = new EventType<>( RESOURCE_SWITCHED, "SWITCHED" );

	private Resource oldResource;

	private Resource newResource;

	public ResourceSwitchedEvent( Object source, EventType<? extends ResourceSwitchedEvent> type, Resource oldResource, Resource newResource ) {
		super( source, type, newResource );
		this.oldResource = oldResource;
		this.newResource = newResource;
	}

	public Resource getOldResource() {
		return oldResource;
	}

	public Resource getNewResource() {
		return newResource;
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public EventType<? extends ResourceSwitchedEvent> getEventType() {
		return (EventType<ResourceSwitchedEvent>)super.getEventType();
	}

	@Override
	public String toString() {
		return super.toString() + ": " + (oldResource == null ? "null" : oldResource.getUri()) + " -> " + (newResource == null ? "null" : newResource.getUri());
	}

}
