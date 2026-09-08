package com.acromere.xenon.resource;

import com.acromere.product.Product;
import com.acromere.product.Rb;
import com.acromere.xenon.ProgramTool;
import com.acromere.xenon.Xenon;
import com.acromere.xenon.XenonProgramProduct;
import com.acromere.xenon.resource.exception.ResourceException;
import com.acromere.xenon.tool.settings.SettingsPage;
import com.acromere.zerra.javafx.Fx;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * <p>
 * The ResourceType class represents an resource type. An resource must always
 * have an resource type and may be directly specified, or determined by the the
 * URI. Resource types may have one or more associated codecs. {@link Scheme},
 * {@link ResourceType} and {@link Codec} work together to save and load resources.
 * <h2>Determining Resource Type</h2>
 * Resource types can usually be determined by using the resource URI. Some
 * resource types can be determined using just the URI scheme. If the resource
 * type cannot be determined by the URI scheme then it is usually a stateful
 * resource with transient connections.
 * <p>The resource type is determined by
 * comparing the resource name to registered codecs. It is possible to match
 * more than one codec. In this case the user might need to choose which codec
 * to use to determine the resource type. If all the possible codecs belong to
 * the same resource type then the user does not have to choose.
 * <p>
 * If the resource type cannot be determined by name, then the first line of the
 * content can be used to match a codec.
 * <p>
 * If the fist line cannot determine the resource type then the content type may
 * be able to be used. This may not be a reliable method since the content type
 * may be specified in a number of ways. No matter how it is specified it should
 * always be considered a best guess.
 * <p>
 * If the resource type still cannot be determined then one of the two default
 * resource types should be used. If, by reading the content, the resource is
 * determined to be text then the text resource type is used. Otherwise, the
 * binary data type is used.
 * <p>
 * When an resource is saved it might also be necessary to update the resource type.
 * <h2>Determining a Resource Tool</h2>
 * Once the resource type is determined an appropriate tool can be created for
 * it. It is possible to have more than one tool registered for the resource
 * type. In this case a default may be specified or the user will need to
 * choose.
 *
 * @author ecco
 */
@CustomLog
public abstract class ResourceType implements Comparable<ResourceType> {

	protected static final String BASE_MEDIA_TYPE = "application/vnd.acromere.xenon.program";

	private final String key = getClass().getName();

	private final XenonProgramProduct product;

	private final String rbKey;

	private final Set<Codec> codecs;

	private Codec defaultCodec;

	@Setter
	@Getter
	private Map<String, SettingsPage> settingsPages;

	public ResourceType( XenonProgramProduct product, String rbKey ) {
		if( product == null ) throw new NullPointerException( "Product cannot be null" );
		if( rbKey == null ) throw new NullPointerException( "Resource bundle key cannot be null" );
		this.product = product;
		this.rbKey = rbKey;
		this.codecs = new CopyOnWriteArraySet<>();
	}

	public Xenon getProgram() {
		return product.getProgram();
	}

	public Product getProduct() {
		return product;
	}

	public String getKey() {
		return key;
	}

	public String getName() {
		return Rb.text( getProduct(), "resource", rbKey + "-name" );
	}

	public String getDescription() {
		return Rb.text( getProduct(), "resource", rbKey + "-description" );
	}

	public String getIcon() {
		return Rb.textOr( getProduct(), "resource", rbKey + "-icon", "resource" );
	}

	/**
	 * Is this resource type a user defined resource type. Usually it is a user
	 * defined resource type so this should return true. For program defined
	 * resource types this should return false.
	 *
	 * @return false if this resource type is program defined, true otherwise
	 */
	public boolean isUserType() {
		return true;
	}

	public Codec getDefaultCodec() {
		return defaultCodec;
	}

	/**
	 * Add and set the default codec.
	 *
	 * @param codec
	 */
	public void setDefaultCodec( Codec codec ) {
		addCodec( this.defaultCodec = codec );
	}

	/**
	 * Get the set of codecs for this resource type.
	 *
	 * @return The set of codecs for this resource type
	 */
	public Set<Codec> getCodecs() {
		return Collections.unmodifiableSet( codecs );
	}

	public void addCodec( Codec codec ) {
		if( codec == null ) return;
		synchronized( codec ) {
			codecs.add( codec );
			codec.setResourceType( this );
		}
	}

	public void removeCodec( Codec codec ) {
		if( codec == null ) return;
		synchronized( codec ) {
			codecs.remove( codec );
			codec.setResourceType( null );
			if( getDefaultCodec() == codec ) setDefaultCodec( null );
		}
	}

	public Set<Codec.Association> getAssociations() {
		return getCodecs().stream().flatMap( c -> c.getAssociations().stream() ).collect( Collectors.toSet() );
	}

	public List<Class<? extends ProgramTool>> getRegisteredTools() {
		return getProgram().getToolManager().getRegisteredTools( this );
	}

	/**
	 * This method is called when a new resource is requested to be opened. This
	 * method is valuable if the resource requires user interaction when creating new
	 * resources.
	 * <p>
	 * Unlike the {@link #resourceOpen(Xenon, Resource)} method this method is
	 * only called for new resources. If the resource is not new, this method will not
	 * be called, unlike the process for opening or restoring existing resources.
	 * <p>
	 * Note: This method is called using a task thread and is not safe to use
	 * directly on UI components.
	 *
	 * @param program
	 * @param resource
	 * @return True if the resource was opened, false otherwise. A value of false will keep the resource from being opened and an editor from being created.
	 * @throws ResourceException if the resource failed to be opened.
	 */
	public boolean resourceNew( Xenon program, Resource resource ) throws ResourceException {
		return true;
	}

	boolean callResourceNew( Xenon program, Resource resource ) throws ResourceException {
		Object lock = new Object();
		AtomicBoolean result = new AtomicBoolean();
		AtomicReference<ResourceException> resultException = new AtomicReference<>();

		Fx.run( () -> {
			synchronized( lock ) {
				try {
					log.atTrace().log( "Calling resourceNew()..." );
					result.set( resourceNew( program, resource ) );
				} catch( ResourceException exception ) {
					resultException.set( exception );
				} finally {
					lock.notifyAll();
				}
			}
		} );

		synchronized( lock ) {
			try {
				lock.wait( 60000 );
			} catch( InterruptedException exception ) {
				exception.printStackTrace();
			}
		}

		log.atDebug().log( "Done waiting for resourceNew()." );

		if( resultException.get() != null ) throw resultException.get();
		return result.get();
	}

	/**
	 * This method is called as a resource is opened just before it is loaded. This
	 * method can provide the specified resource with an initial state prior to being
	 * loaded or used in a tool.
	 * <p>
	 * Unlike the {@link #resourceNew(Xenon, Resource)} method this method is
	 * always called whenever a resource is opened, new or otherwise. This method
	 * should not be used for user interaction. User interaction should be
	 * implemented in the {@link #resourceNew(Xenon, Resource)} method.
	 * <p>
	 * Note: This method is called using a task thread and is not safe to use
	 * directly on UI components. <br>
	 *
	 * @param program
	 * @param resource
	 * @return True if the resource was initialized, false otherwise. A value of false will keep the resource from being opened and a tool from being created.
	 * @throws ResourceException if the resource failed to be initialized.
	 */
	public boolean resourceOpen( Xenon program, Resource resource ) throws ResourceException {
		return true;
	}

	boolean callResourceOpen( Xenon program, Resource resource ) throws ResourceException {
		return resourceOpen( program, resource );
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public int compareTo( ResourceType type ) {
		return getName().compareTo( type.getName() );
	}

	public Set<Codec> getSupportedCodecs( Codec.Pattern type, String value ) {
		return codecs.stream().filter( c -> c.isSupported( type, value ) ).collect( Collectors.toSet() );
	}

}
