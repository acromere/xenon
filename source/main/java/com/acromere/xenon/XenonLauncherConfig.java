package com.acromere.xenon;

import com.acromere.product.ProductCard;
import com.acromere.util.OperatingSystem;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

class XenonLauncherConfig {

	private static ProductCard card;

	/**
	 * Set the custom launcher system property.
	 * See {@link OperatingSystem#getJavaLauncherPath()} for more information.
	 */
	static void setCustomLauncherSystemProperty() {
		if( System.getProperty( OperatingSystem.CUSTOM_LAUNCHER_PATH ) != null ) {
			System.setProperty( OperatingSystem.CUSTOM_LAUNCHER_NAME, loadProductCard().getName() );
		}
	}

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

}
