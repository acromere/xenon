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

	static void setDefaultSslContext() {
		// NEXT If this works, move this logic to the OperatingSystem class
		if( !OperatingSystem.isWindows() ) return;
		try {
			// 1. Load System KeyStore (e.g., Windows ROOT)
			KeyStore systemKeyStore = KeyStore.getInstance( "Windows-ROOT" );
			systemKeyStore.load( null, null );

			// 2. Load JVM Default KeyStore
			KeyStore jvmKeyStore = KeyStore.getInstance( KeyStore.getDefaultType() );
			jvmKeyStore.load( null, null ); // Load default truststore

			// 3. Merge KeyStores
			TrustManagerFactory systemTmf = TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() );
			systemTmf.init( systemKeyStore ); // Use system trust store
			TrustManagerFactory jvmTmf = TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() );
			jvmTmf.init( jvmKeyStore );

			List<TrustManager> trustManagers = new ArrayList<>();
			trustManagers.addAll( List.of( systemTmf.getTrustManagers() ) );
			trustManagers.addAll( List.of( jvmTmf.getTrustManagers() ) );

			// 4. Create and Initialize SSLContext
			SSLContext sslContext = SSLContext.getInstance( "TLS" );
			sslContext.init( null, trustManagers.toArray( new TrustManager[ 0 ] ), null );
			SSLContext.setDefault( sslContext );
		} catch( Exception e ) {
			//
		}
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
