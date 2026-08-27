package com.acromere.xenon.product;

import java.util.Objects;

public class ProductCoordinates {

	private String artifact;

	private String platform;

	private String resource;

	private String format;

	public ProductCoordinates( String artifact, String platform, String resource, String format ) {
		this.artifact = artifact;
		this.platform = platform;
		this.resource = resource;
		this.format = format;
	}

	public String getArtifact() {
		return artifact;
	}

	public void setArtifact( String artifact ) {
		this.artifact = artifact;
	}

	public String getPlatform() {
		return platform;
	}

	public void setPlatform( String platform ) {
		this.platform = platform;
	}

	public String getResource() {
		return resource;
	}

	public void setResource( String resource ) {
		this.resource = resource;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat( String format ) {
		this.format = format;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder( artifact );
		if( platform != null ) builder.append( "/" ).append( platform );
		builder.append( "/" ).append( resource );
		if( format != null ) builder.append( "." ).append( format );
		return builder.toString();
	}

	@Override
	public boolean equals( Object o ) {
		if( this == o ) return true;
		if( o == null || getClass() != o.getClass() ) return false;
		ProductCoordinates that = (ProductCoordinates)o;
		return artifact.equals( that.artifact ) && Objects.equals( platform, that.platform ) && resource.equals( that.resource ) && Objects.equals( format, that.format );
	}

	@Override
	public int hashCode() {
		return Objects.hash( artifact, platform, resource, format );
	}
}
