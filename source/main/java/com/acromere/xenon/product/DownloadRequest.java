package com.acromere.xenon.product;

import com.acromere.event.Event;
import com.acromere.event.EventHandler;
import com.acromere.event.EventHub;
import com.acromere.event.EventType;
import com.acromere.product.ProductCard;
import com.acromere.xenon.task.TaskEvent;
import lombok.CustomLog;

@CustomLog
public class DownloadRequest implements EventHandler<TaskEvent> {

	private final ProductCard card;

	private final EventHub bus;

	public DownloadRequest( ProductCard card ) {
		this.card = card;
		this.bus = new EventHub();
	}

	public ProductCard getCard() {
		return card;
	}

	public <T extends Event> EventHub register( EventType<? super T> type, EventHandler<? super T> handler ) {
		return bus.register( type, handler );
	}

	public <T extends Event> EventHub unregister( EventType<? super T> type, EventHandler<? super T> handler ) {
		return bus.unregister( type, handler );
	}

	@Override
	public void handle( TaskEvent event ) {
		bus.dispatch( event );
	}

}
