package com.acromere.xenon.resource;

import com.acromere.event.Event;
import com.acromere.event.EventType;
import lombok.Getter;

@Getter
public class ResourceEvent extends Event {

	public static final EventType<ResourceEvent> RESOURCE = new EventType<>( Event.ANY, "RESOURCE" );

	public static final EventType<ResourceEvent> ANY = RESOURCE;

	public static final EventType<ResourceEvent> OPENED = new EventType<>( RESOURCE, "OPENED" );

	public static final EventType<ResourceEvent> LOADED = new EventType<>( RESOURCE, "LOADED" );

	@Deprecated
	public static final EventType<ResourceEvent> READY = new EventType<>( RESOURCE, "READY" );

	public static final EventType<ResourceEvent> MODIFIED = new EventType<>( RESOURCE, "MODIFIED" );

	public static final EventType<ResourceEvent> UNMODIFIED = new EventType<>( RESOURCE, "UNMODIFIED" );

	// The resource is the resource in the active tool
	public static final EventType<ResourceEvent> ACTIVATED = new EventType<>( RESOURCE, "ACTIVATED" );

	// The resource is not the resource in the active tool
	public static final EventType<ResourceEvent> DEACTIVATED = new EventType<>( RESOURCE, "DEACTIVATED" );

	public static final EventType<ResourceEvent> SAVED = new EventType<>( RESOURCE, "SAVED" );

	public static final EventType<ResourceEvent> CLOSED = new EventType<>( RESOURCE, "CLOSED" );

	public static final EventType<ResourceEvent> DELETED = new EventType<>( RESOURCE, "DELETED" );

	private final Resource resource;

	public ResourceEvent( Object source, EventType<? extends ResourceEvent> type, Resource resource ) {
		super( source, type );
		this.resource = resource;
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public EventType<? extends ResourceEvent> getEventType() {
		return (EventType<ResourceEvent>)super.getEventType();
	}

	@Override
	public String toString() {
		Resource resource = getResource();
		if( resource == null ) return super.toString() + ": null";
		return super.toString() + ": " + resource;
	}

}
