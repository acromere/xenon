package com.acromere.xenon.testutil;

import com.acromere.event.EventHandler;
import com.acromere.xenon.product.ProductEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings( "unused" )
public class ProgramEventCollector implements EventHandler<ProductEvent> {

	private final List<ProductEvent> events = new CopyOnWriteArrayList<>();

	@Override
	public void handle( ProductEvent event ) {
		events.add( event );
	}

	@SuppressWarnings( "unused" )
	public List<ProductEvent> getEvents() {
		return events;
	}

}
