package com.acromere.xenon.product;

import com.acromere.product.Product;
import com.acromere.product.ProductCard;
import com.acromere.product.ProductCardComparator;

public class ProgramProductCardComparator extends ProductCardComparator {

	private final Product product;

	public ProgramProductCardComparator( Product product, Field field ) {
		super( field );
		this.product = product;
	}

	@Override
	public int compare( ProductCard card1, ProductCard card2 ) {
		if( card1.equals( product.getCard() ) ) return -1;
		if( card2.equals( product.getCard() ) ) return 1;
		return super.compare( card1, card2 );
	}

}
