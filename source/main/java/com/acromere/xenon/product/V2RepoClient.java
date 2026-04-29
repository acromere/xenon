package com.acromere.xenon.product;

import com.acromere.product.RepoCard;
import com.acromere.util.OperatingSystem;
import com.acromere.util.UriUtil;
import com.acromere.xenon.Xenon;
import lombok.CustomLog;

import java.net.URI;

@CustomLog
public class V2RepoClient implements RepoClient {

	private final Xenon program;

	public V2RepoClient( Xenon program ) {
		this.program = program;
	}

	@Override
	public URI getCatalogUri( RepoCard repo ) {
		return UriUtil.addToPath( getRepoApi( repo ), "catalog" );
	}

	/**
	 * Get the product URI. This implementation also ensures that the platform
	 * is included in the URI.
	 *
	 * @param repo
	 * @param product
	 * @param asset
	 * @param format
	 * @return
	 */
	@Override
	public URI getProductUri( RepoCard repo, String product, String asset, String format ) {
		String platform = OperatingSystem.getFamily().toString().toLowerCase();

		URI uri = getRepoApi( repo );
		uri = UriUtil.addToPath( uri, product );
		uri = UriUtil.addToPath( uri, platform );
		uri = UriUtil.addToPath( uri, asset );
		uri = UriUtil.addToPath( uri, format );
		return uri;
	}

	private URI getRepoApi( RepoCard repo ) {
		return URI.create( repo.getUrl() );
	}

}
