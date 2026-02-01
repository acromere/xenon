package com.acromere.xenon.product;

import com.acromere.event.EventType;
import com.acromere.product.ProductCard;
import com.acromere.xenon.ProgramEvent;

public class ProductEvent extends ProgramEvent {

	public static final EventType<ProgramEvent> PRODUCT = new EventType<>( ProgramEvent.ANY, "PRODUCT" );

	public static final EventType<ProgramEvent> ANY = PRODUCT;

	public static final EventType<ProgramEvent> STAGED = new EventType<>( PRODUCT, "STAGED" );

	private ProductCard card;

	public ProductEvent( Object source, EventType<? extends ProgramEvent> type, ProductCard card ) {
		super( source, type );
		this.card = card;
	}

	public ProductCard getMetadata() {
		return card;
	}

	public String toString() {
		return super.toString() + ": " + getMetadata().toString();
	}

}
