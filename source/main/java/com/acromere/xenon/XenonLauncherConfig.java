package com.acromere.xenon;

import com.acromere.product.ProductCard;
import com.acromere.util.OperatingSystem;

class XenonLauncherConfig {

	private static ProductCard card;

	static ProductCard loadProductCard() {
		return updateProductCard( ProductCard.card( Xenon.class ) );
	}

	private static ProductCard updateProductCard( ProductCard fullCard ) {
		// Fill out the rest of the product card
		// Some fields may already be populated, like installation folder
		if( card != null ) {
			fullCard.setInstallFolder( card.getInstallFolder() );
		}
		return card = fullCard;
	}

	/**
	 * Set the custom launcher system property.
	 * See {@link OperatingSystem#getJavaLauncherPath()} for more information.
	 */
	static void setCustomLauncherSystemProperty() {
		if( System.getProperty( OperatingSystem.CUSTOM_LAUNCHER_PATH ) != null ) {
			System.setProperty( OperatingSystem.CUSTOM_LAUNCHER_NAME, loadProductCard().getName() );
		}
	}

}
