package com.acromere.xenon.resource;

import com.acromere.event.EventHandler;
import com.acromere.product.Rb;
import com.acromere.settings.Settings;
import com.acromere.skill.Controllable;
import com.acromere.util.*;
import com.acromere.xenon.*;
import com.acromere.xenon.notice.Notice;
import com.acromere.xenon.resource.exception.ResourceException;
import com.acromere.xenon.resource.type.ProgramResourceNewType;
import com.acromere.xenon.resource.type.ProgramResourceType;
import com.acromere.xenon.scheme.FileScheme;
import com.acromere.xenon.scheme.NewScheme;
import com.acromere.xenon.task.Task;
import com.acromere.xenon.throwable.NoToolRegisteredException;
import com.acromere.xenon.throwable.SchemeNotRegisteredException;
import com.acromere.xenon.tool.ResourceTool;
import com.acromere.xenon.workpane.Workpane;
import com.acromere.xenon.workpane.WorkpaneView;
import com.acromere.zerra.event.FxEventHub;
import com.acromere.zerra.stage.DialogUtil;
import javafx.event.ActionEvent;
import javafx.geometry.Side;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import lombok.CustomLog;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@CustomLog
public class ResourceManager implements Controllable<ResourceManager> {

	private static final long DEFAULT_AUTOSAVE_MAX_TRIGGER_LIMIT = 5000;

	private static final long DEFAULT_AUTOSAVE_MIN_TRIGGER_LIMIT = 100;

	private static final String CURRENT_FILE_FOLDER_SETTING_KEY = "current-file-folder";

	private final Xenon program;

	private volatile Resource currentResource;

	private final Set<Resource> openResources;

	private final Map<URI, Resource> identifiedResources;

	private final Map<String, Scheme> schemes;

	private final Map<String, ResourceType> resourceTypes;

	private final Map<Codec.Pattern, Map<String, Set<Codec>>> registeredCodecs;

	private final DelayedAction autosave;

	private final FxEventHub eventBus;

	private final NewActionHandler newActionHandler;

	private final OpenActionHandler openActionHandler;

	private final ReloadActionHandler reloadActionHandler;

	private final SaveActionHandler saveActionHandler;

	private final SaveActionHandler saveAsActionHandler;

	private final SaveAllActionHandler saveAllActionHandler;

	private final RenameActionHandler renameActionHandler;

	private final CloseActionHandler closeActionHandler;

	private final CloseAllActionHandler closeAllActionHandler;

	private final CurrentResourceWatcher currentResourceWatcher;

	private final GeneralResourceWatcher generalResourceWatcher;

	private final Object currentResourceLock = new Object();

	private final Map<URI, URI> aliases;

	private boolean running;

	public ResourceManager( Xenon program ) {
		this.program = program;
		openResources = new CopyOnWriteArraySet<>();
		identifiedResources = new ConcurrentHashMap<>();
		schemes = new ConcurrentHashMap<>();
		resourceTypes = new ConcurrentHashMap<>();
		registeredCodecs = new ConcurrentHashMap<>();
		aliases = new ConcurrentHashMap<>();

		// FIXME This is pretty dangerous for a couple of reasons
		// 1. It saves all resources, not just the current one
		// 2. In the event there was an error loading an resource, it can save the resource in a bad state
		// ?. Maybe this should be changed to save resources that submit themselves for autosave?
		autosave = new DelayedAction( program.getTaskManager().getExecutor(), this::saveAll );
		autosave.setMinTriggerLimit( program.getSettings().get( "autosave-trigger-min", Long.class, DEFAULT_AUTOSAVE_MIN_TRIGGER_LIMIT ) );
		autosave.setMaxTriggerLimit( program.getSettings().get( "autosave-trigger-max", Long.class, DEFAULT_AUTOSAVE_MAX_TRIGGER_LIMIT ) );

		eventBus = new FxEventHub();
		eventBus.parent( program.getFxEventHub() );
		currentResourceWatcher = new CurrentResourceWatcher();
		generalResourceWatcher = new GeneralResourceWatcher();

		newActionHandler = new NewActionHandler( program );
		openActionHandler = new OpenActionHandler( program );
		reloadActionHandler = new ReloadActionHandler( program );
		saveActionHandler = new SaveActionHandler( program, false );
		saveAsActionHandler = new SaveActionHandler( program, true );
		saveAllActionHandler = new SaveAllActionHandler( program );
		renameActionHandler = new RenameActionHandler( program );
		closeActionHandler = new CloseActionHandler( program );
		closeAllActionHandler = new CloseAllActionHandler( program );
	}

	public final Xenon getProgram() {
		return program;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public ResourceManager start() {
		program.getActionLibrary().getAction( "new" ).pushAction( newActionHandler );
		program.getActionLibrary().getAction( "open" ).pushAction( openActionHandler );
		program.getActionLibrary().getAction( "reload" ).pushAction( reloadActionHandler );
		program.getActionLibrary().getAction( "save" ).pushAction( saveActionHandler );
		program.getActionLibrary().getAction( "save-as" ).pushAction( saveAsActionHandler );
		program.getActionLibrary().getAction( "save-all" ).pushAction( saveAllActionHandler );
		program.getActionLibrary().getAction( "rename" ).pushAction( renameActionHandler );
		program.getActionLibrary().getAction( "close" ).pushAction( closeActionHandler );
		program.getActionLibrary().getAction( "close-all" ).pushAction( closeAllActionHandler );
		updateActionState();

		//program.getEventHub().register( ToolEvent.ANY, activeToolWatcher );

		program.getSettings().register( "autosave-trigger-min", e -> autosave.setMinTriggerLimit( Long.parseLong( String.valueOf( e.getNewValue() ) ) ) );
		program.getSettings().register( "autosave-trigger-max", e -> autosave.setMaxTriggerLimit( Long.parseLong( String.valueOf( e.getNewValue() ) ) ) );

		running = true;

		return this;
	}

	@Override
	public ResourceManager stop() {
		running = false;

		//program.getEventHub().unregister( ToolEvent.ANY, activeToolWatcher );

		return this;
	}

	public FxEventHub getEventBus() {
		return eventBus;
	}

	public Path getCurrentFileFolder() {
		// Determine the current folder
		// The current folder string is in URI format
		String currentFolderString = getProgram().getSettings().get( ResourceManager.CURRENT_FILE_FOLDER_SETTING_KEY );
		log.atConfig().log( "Stored current folder: %s", currentFolderString );
		if( currentFolderString == null ) currentFolderString = System.getProperty( "user.dir" );
		URI currentFolderUri = URI.create( currentFolderString );
		Path currentFolder = FileUtil.findValidFolder( currentFolderUri.toString() );
		log.atConfig().log( "Result current folder: %s", currentFolderString );
		setCurrentFileFolder( currentFolder.toUri() );
		return currentFolder;
	}

	public void setCurrentFileFolder( Resource resource ) {
		setCurrentFileFolder( resource.getUri() );
	}

	private void setCurrentFileFolder( URI uri ) {
		if( !FileScheme.ID.equals( uri.getScheme() ) ) return;
		// Current folder value is store in URI format
		getProgram().getSettings().set( ResourceManager.CURRENT_FILE_FOLDER_SETTING_KEY, uri );
	}

	public Resource getCurrentResource() {
		return currentResource;
	}

	public void setCurrentResource( Resource resource ) {
		program.getTaskManager().submit( new SetCurrentResourceTask( resource ) );
	}

	public void setCurrentResourceAndWait( Resource resource ) throws ExecutionException, InterruptedException {
		program.getTaskManager().submit( new SetCurrentResourceTask( resource ) ).get();
	}

	public Set<Resource> getOpenResources() {
		return new HashSet<>( openResources );
	}

	public Set<Resource> getModifiedResources() {
		return getOpenResources().stream().filter( Resource::isModified ).collect( Collectors.toSet() );
	}

	Set<ResourceType> getUserResourceTypes() {
		return resourceTypes.values().stream().filter( ResourceType::isUserType ).collect( Collectors.toSet() );
	}

	/**
	 * Get the externally modified resources.
	 *
	 * @return The set of externally modified resources
	 */
	public Set<Resource> getExternallyModifiedResources() {
		return getOpenResources().stream().filter( Resource::isExternallyModified ).collect( Collectors.toSet() );
	}

	/**
	 * Get a scheme by the scheme name.
	 *
	 * @param name The scheme name
	 * @return The scheme registered to the name
	 */
	public Scheme getScheme( String name ) {
		if( name == null ) name = FileScheme.ID;
		return schemes.get( name );
	}

	private void resolveScheme( Resource resource ) throws ResourceException {
		resolveScheme( resource, resource.getUri().getScheme() );
	}

	private void resolveScheme( Resource resource, String name ) throws ResourceException {
		Scheme scheme = getScheme( name );
		if( scheme == null ) throw new ResourceException( resource, new SchemeNotRegisteredException( name ) );
		resource.setScheme( scheme );
	}

	/**
	 * Add a scheme.
	 *
	 * @param scheme The scheme to add
	 */
	public void addScheme( Scheme scheme ) {
		schemes.put( scheme.getName(), scheme );
	}

	/**
	 * Remove a scheme.
	 *
	 * @param name The name of the scheme to remove
	 */
	public void removeScheme( String name ) {
		schemes.remove( name );
	}

	/**
	 * Get the registered schemes.
	 *
	 * @return The set of registered schemes
	 */
	public Collection<Scheme> getSchemes() {
		return Collections.unmodifiableCollection( schemes.values() );
	}

	/**
	 * Get the registered scheme names.
	 *
	 * @return The set of registered scheme names
	 */
	public Set<String> getSchemeNames() {
		return Collections.unmodifiableSet( schemes.keySet() );
	}

	/**
	 * Get a resource type by the resource type key defined in the resource type.
	 * This is useful for getting resource types from persisted data.
	 *
	 * @param key The resource type key
	 * @return The resource type associated with the key
	 */
	public ResourceType getResourceType( String key ) {
		if( key == null ) return null;
		ResourceType type = resourceTypes.get( key );
		if( type == null ) log.atWarning().log( "Resource type not found: %s", key );
		return type;
	}

	/**
	 * Get the set of supported resource types.
	 *
	 * @return The set of supported resource types
	 */
	public Collection<ResourceType> getResourceTypes() {
		return Collections.unmodifiableCollection( resourceTypes.values() );
	}

	/**
	 * Add a resource type to the set of supported resource types.
	 *
	 * @param type The resource type to add
	 */
	public void addResourceType( ResourceType type ) {
		if( type == null ) return;

		synchronized( resourceTypes ) {
			if( resourceTypes.get( type.getKey() ) != null ) throw new IllegalArgumentException( "ResourceType already exists: " + type.getKey() );

			// Register codecs
			for( Codec codec : type.getCodecs() ) {
				registerCodecs( Codec.Pattern.URI, codec );
				registerCodecs( Codec.Pattern.MEDIATYPE, codec );
				registerCodecs( Codec.Pattern.EXTENSION, codec );
				registerCodecs( Codec.Pattern.FILENAME, codec );
				registerCodecs( Codec.Pattern.SCHEME, codec );
				registerCodecs( Codec.Pattern.FIRSTLINE, codec );
			}

			// Add the resource type to the registered resource types
			resourceTypes.put( type.getKey(), type );

			// Update the actions
			updateActionState();
		}
	}

	/**
	 * Remove a resource type from the set of supported resource types.
	 *
	 * @param type The resource type to remove
	 */
	public void removeResourceType( ResourceType type ) {
		if( type == null ) return;
		synchronized( resourceTypes ) {
			if( !resourceTypes.containsKey( type.getKey() ) ) return;

			// Remove the resource type from the registered resource types
			type = resourceTypes.remove( type.getKey() );

			for( Codec codec : type.getCodecs() ) {
				// Unregister codecs
				unregisterCodecs( Codec.Pattern.URI, codec );
				unregisterCodecs( Codec.Pattern.MEDIATYPE, codec );
				unregisterCodecs( Codec.Pattern.EXTENSION, codec );
				unregisterCodecs( Codec.Pattern.FILENAME, codec );
				unregisterCodecs( Codec.Pattern.SCHEME, codec );
				unregisterCodecs( Codec.Pattern.FIRSTLINE, codec );
			}

			// Update the actions.
			updateActionState();
		}
	}

	public Future<ProgramTool> newResource( String key ) {
		return newResource( key, null );
	}

	public Future<ProgramTool> newResource( String key, Object model ) {
		return newResource( getResourceType( key ), model, null, true, true );
	}

	/**
	 * This method starts the process of creating a new resource by resource type.
	 * The returned future allows the caller to get the tool created for the new
	 * resource. It is possible that a tool was not created for the resource, in
	 * which case the tool is null.
	 *
	 * @param type The new resource type
	 * @return The future to get the new resource tool
	 */
	public Future<ProgramTool> newResource( ResourceType type ) {
		return newResource( type, true, true );
	}

	/**
	 * This method starts the process of creating a new resource by resource type.
	 * The returned future allows the caller to get the tool created for the new
	 * resource. It is possible that a tool was not created for the resource, in
	 * which case the tool is null.
	 *
	 * @param type The new resource type
	 * @return The future to get the new resource tool
	 */
	public Future<ProgramTool> newResource( ResourceType type, boolean openTool, boolean setActive ) {
		return newResource( type, null, null, openTool, setActive );
	}

	/**
	 * This method starts the process of creating a new resource by resource type.
	 * The returned future allows the caller to get the tool created for the new
	 * resource. It is possible that a tool was not created for the resource, in
	 * which case the tool is null.
	 *
	 * @param type The new resource type
	 * @return The future to get the new resource tool
	 */
	private Future<ProgramTool> newResource( ResourceType type, Object model, WorkpaneView view, boolean openTool, boolean setActive ) {
		OpenResourceRequest request = new OpenResourceRequest();
		request.setUri( null );
		request.setType( type );
		request.setModel( model );
		request.setView( view );
		request.setOpenTool( openTool );
		request.setSetActive( setActive );
		return program.getTaskManager().submit( new NewOrOpenResourceTask( request ) );
	}

	public Future<ProgramTool> openResource( URI uri ) {
		return openResource( uri, true, true );
	}

	public Future<ProgramTool> openResource( URI uri, Object model ) {
		return openResource( uri, model, null, null, null, true, true );
	}

	public Future<ProgramTool> openResource( URI uri, Workpane pane ) {
		return openResource( uri, null, pane, null, null, true, true );
	}

	public Future<ProgramTool> openResource( URI uri, Class<? extends ProgramTool> toolClass ) {
		return openResource( uri, null, null, null, toolClass, true, true );
	}

	public Future<ProgramTool> openResource( URI uri, boolean openTool, boolean setActive ) {
		return openResource( uri, null, null, null, null, openTool, setActive );
	}

	public Future<ProgramTool> openResource( URI uri, Workpane pane, boolean openTool, boolean setActive ) {
		return openResource( uri, null, pane, null, null, openTool, setActive );
	}

	public Future<ProgramTool> openResource( URI uri, WorkpaneView view ) {
		return openResource( uri, null, null, view, null, true, true );
	}

	public Future<ProgramTool> openResource( URI uri, WorkpaneView view, Side side ) {
		if( side != null ) view = view.getWorkpane().split( view, side );
		return openResource( uri, null, null, view, null, true, true );
	}

	public Set<Future<ProgramTool>> openDependencyResources( Set<URI> uris, Workpane pane ) {
		return uris.stream().map( uri -> openResource( uri, null, pane, null, null, true, false ) ).collect( Collectors.toSet() );
	}

	private Future<ProgramTool> openResource( URI uri, Object model, Workpane pane, WorkpaneView view, Class<? extends ProgramTool> toolClass, boolean openTool, boolean setActive ) {
		OpenResourceRequest request = new OpenResourceRequest();
		request.setUri( uri );
		request.setPane( pane );
		request.setView( view );
		request.setOpenTool( openTool );
		request.setSetActive( setActive );
		request.setModel( model );
		request.setToolClass( toolClass );
		return program.getTaskManager().submit( new NewOrOpenResourceTask( request ) );
	}

	public Future<ProgramTool> openResource( Resource resource ) {
		return openResource( resource, null, null, null );
	}

	public Future<ProgramTool> openResource( Resource resource, Class<? extends ProgramTool> toolClass ) {
		return openResource( resource, null, null, toolClass );
	}

	public Future<ProgramTool> openResource( Resource resource, WorkpaneView view ) {
		return openResource( resource, view, null, null );
	}

	public Future<ProgramTool> openResource( Resource resource, WorkpaneView view, Side side ) {
		return openResource( resource, view, side, null );
	}

	public Future<ProgramTool> openResource( Resource resource, WorkpaneView view, Side side, Class<? extends ProgramTool> toolClass ) {
		if( side != null ) view = view.getWorkpane().split( view, side );
		OpenResourceRequest request = new OpenResourceRequest();
		request.setResource( resource );
		request.setView( view );
		request.setOpenTool( true );
		request.setSetActive( true );
		request.setToolClass( toolClass );
		return program.getTaskManager().submit( new NewOrOpenResourceTask( request ) );
	}

	public void reloadResource( Resource resource ) {
		if( !resource.isLoaded() ) return;
		reloadResources( resource );
	}

	/**
	 * @implNote This method makes calls to the FX platform.
	 */
	public void saveResource( Resource resource ) {
		doSaveOrRenameResource( resource, null, false, false );
	}

	/**
	 * Request that the source resource be saved as the target resource. This
	 * method submits a task to the task manager and returns immediately.
	 *
	 * @param source The source resource
	 * @param target The target resource
	 * @implNote This method makes calls to the FX platform.
	 */
	public void saveAsResource( Resource source, Resource target ) {
		doSaveOrRenameResource( source, target, true, false );
	}

	/**
	 * Request that the source resource be renamed as the target resource. This
	 * method submits a task to the task manager and returns immediately.
	 *
	 * @param source The source resource
	 * @param target The target resource
	 * @implNote This method makes calls to the FX platform.
	 */
	public void renameResource( Resource source, Resource target ) {
		doSaveOrRenameResource( source, target, false, true );
	}

	/**
	 * Close the resource, prompting the user if necessary.
	 *
	 * @param resource The resource to be closed
	 * @implNote This method makes calls to the FX platform.
	 */
	public void close( Resource resource ) {
		if( resource.isModified() && canSaveResource( resource ) ) {
			Alert alert = new Alert( Alert.AlertType.CONFIRMATION, "", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL );
			alert.setTitle( Rb.text( RbKey.RESOURCE, "close-save-title" ) );
			alert.setHeaderText( Rb.text( RbKey.RESOURCE, "close-save-message" ) );
			alert.setContentText( Rb.text( RbKey.RESOURCE, "close-save-prompt" ) );

			Stage stage = program.getWorkspaceManager().getActiveStage();
			Optional<ButtonType> result = DialogUtil.showAndWait( stage, alert );

			if( result.isPresent() && result.get() == ButtonType.YES ) saveResource( resource );
			if( result.isEmpty() || result.get() == ButtonType.CANCEL ) return;
		}

		closeResources( resource );
	}

	public Resource createResource( Object descriptor ) throws ResourceException {
		return switch( descriptor ) {
			case URI uri -> (createResource( uri ));
			case File file -> (createResource( file.toURI() ));
			case Path path -> (createResource( path.toUri() ));
			default -> (createResource( descriptor.toString() ));
		};
	}

	/**
	 * Create a resource from a string. This resource is considered to be an
	 * existing resource. See {@link Resource#isNew()}
	 *
	 * @param string The resource string
	 * @return A new resource based on the specified string.
	 */
	public Resource createResource( String string ) throws ResourceException {
		if( string == null ) return null;

		URI uri = UriUtil.resolve( string );

		if( uri == null ) {
			String title = Rb.text( "asset", "assets" );
			String message = Rb.text( "program", "asset-unable-to-resolve" );
			program.getNoticeManager().warning( title, message, string );
			return null;
		}

		return createResource( uri );
	}

	/**
	 * Create a resource from a URI. This resource is considered to be an
	 * existing resource. See {@link Resource#isNew()}
	 *
	 * @param uri The URI to create a resource from
	 * @return The resource created from the URI
	 */
	public Resource createResource( URI uri ) throws ResourceException {
		return doCreateResource( null, uri );
	}

	/**
	 * Create a resource from a file. This resource is considered to be an
	 * existing resource. See {@link Resource#isNew()}
	 *
	 * @param file The file to create a resource from
	 * @return The resource created from the file
	 * @deprecated Use {@link #createResource(Path)} instead
	 */
	@Deprecated
	public Resource createResource( File file ) throws ResourceException {
		return doCreateResource( null, file.toURI() );
	}

	/**
	 * Create a resource from a path. This resource is considered to be an
	 * existing resource. See {@link Resource#isNew()}
	 *
	 * @param path The path to create a resource from
	 * @return The resource created from the path
	 */
	public Resource createResource( Path path ) throws ResourceException {
		return doCreateResource( null, path.toUri() );
	}

	/**
	 * Create a resource from a resource type. This resource is considered to be
	 * a new resource. See {@link Resource#isNew()}
	 *
	 * @param type The resource type to create a resource from
	 * @return The resource created from the resource type
	 */
	public Resource createResource( ResourceType type ) throws ResourceException {
		return doCreateResource( type, null );
	}

	public Resource createResource( ResourceType type, String uri ) throws ResourceException {
		return doCreateResource( type, UriUtil.resolve( uri ) );
	}

	/**
	 * Create a resource from a resource type and uri.
	 *
	 * @param type The resource type
	 * @param uri The resource uri
	 * @return The created resource
	 */
	public Resource createResource( ResourceType type, URI uri ) throws ResourceException {
		return doCreateResource( type, uri );
	}

	/**
	 * Create resources from an array of descriptors. Descriptors are preferred in
	 * the following order: URI, Path, String, Object
	 *
	 * @param descriptors The descriptors from which to create resources
	 * @return The list of resources created from the descriptors
	 */
	public Collection<Resource> createResources( Object... descriptors ) throws ResourceException {
		return createResources( List.of( descriptors ) );
	}

	/**
	 * Create resources from a collection of descriptors. Descriptors are
	 * preferred in the following order: URI, Path, String, Object
	 *
	 * @param descriptors The descriptors from which to create resources
	 * @return The list of resources created from the descriptors
	 */
	public Collection<Resource> createResources( Collection<?> descriptors ) throws ResourceException {
		List<Resource> resources = new ArrayList<>( descriptors.size() );

		for( Object descriptor : descriptors ) {
			resources.add( createResource( descriptor ) );
		}

		return resources;
	}

	/**
	 * Request that the specified resources be opened. This method submits a task
	 * to the task manager and returns immediately.
	 *
	 * @param resources The resources to open
	 */
	public void openResources( Resource... resources ) throws ResourceException {
		openResources( List.of( resources ) );
	}

	/**
	 * Request that the specified resources be opened. This method submits a task
	 * to the task manager and returns immediately.
	 *
	 * @param resources The resources to open
	 */
	public void openResources( Collection<Resource> resources ) throws ResourceException {
		program.getTaskManager().submit( new OpenResourceTask( removeAlreadyOpenResources( resources ) ) );
	}

	/**
	 * Request that the specified resources be opened and wait until the task is
	 * complete. This method submits a task to the task manager and waits for the
	 * task to be completed.
	 *
	 * @param resource The resource to open
	 * @throws ExecutionException If there was an exception, opening the resource
	 * @throws InterruptedException If the process of opening the resource was interrupted
	 * @implNote Do not call from the UI thread
	 */
	public void openResourcesAndWait( Resource resource, long time, TimeUnit unit ) throws ExecutionException, InterruptedException, TimeoutException {
		openResourcesAndWait( List.of( resource ), time, unit );
	}

	/**
	 * Request that the specified resources be opened and wait until the task is
	 * complete. This method submits a task to the task manager and waits for the
	 * task to be completed.
	 *
	 * @param resources The resources to open
	 * @throws ExecutionException If there was an exception, opening a resource
	 * @throws InterruptedException If the process of opening a resource was interrupted
	 * @implNote Do not call from a UI thread
	 */
	public void openResourcesAndWait( Collection<Resource> resources, long time, TimeUnit unit ) throws ExecutionException, InterruptedException, TimeoutException {
		program.getTaskManager().submit( new OpenResourceTask( removeAlreadyOpenResources( resources ) ) ).get( time, unit );
	}

	/**
	 * Request that the specified resources be loaded. This method submits a task
	 * to the task manager and returns immediately.
	 *
	 * @param resources The resources to load
	 */
	public Future<Collection<Resource>> loadResources( Resource... resources ) {
		return loadResources( List.of( resources ) );
	}

	/**
	 * Request that the specified resources be loaded. This method submits a task
	 * to the task manager and returns immediately.
	 *
	 * @param resources The resources to load
	 */
	public Future<Collection<Resource>> loadResources( Collection<Resource> resources ) {
		return program.getTaskManager().submit( new LoadResourceTask( resources ) );
	}

	/**
	 * Request that the specified resources be loaded and wait until the task is
	 * complete. This method submits a task to the task manager and waits for the
	 * task to be completed.
	 *
	 * @param resources The resources to load
	 * @throws ExecutionException If there was an exception, loading the resource
	 * @throws InterruptedException If the process of loading the resource was interrupted
	 * @implNote Do not call from a UI thread
	 */
	public void loadResourcesAndWait( Resource... resources ) throws ExecutionException, InterruptedException {
		loadResourcesAndWait( List.of( resources ) );
	}

	/**
	 * Request that the specified resources be loaded and wait until the task is
	 * complete. This method submits a task to the task manager and waits for the
	 * task to be completed.
	 *
	 * @param resources The resources to load
	 * @throws ExecutionException If there was an exception, loading the resources
	 * @throws InterruptedException If the process of loading the resources was interrupted
	 * @implNote Do not call from a UI thread
	 */
	public void loadResourcesAndWait( Collection<Resource> resources ) throws ExecutionException, InterruptedException {
		program.getTaskManager().submit( new LoadResourceTask( resources ) ).get();
	}

	/**
	 * Request that the specified resources be reloaded. This method submits a
	 * task to the task manager and returns immediately.
	 *
	 * @param resource The resource to reload
	 */
	public void reloadResources( Resource resource ) {
		reloadResources( Collections.singletonList( resource ) );
	}

	/**
	 * Request that the specified resources be reloaded. This method submits a task
	 * to the task manager and returns immediately.
	 *
	 * @param resources The resources to reload
	 */
	public void reloadResources( Collection<Resource> resources ) {
		program.getTaskManager().submit( new ReloadResourceTask( resources ) );
	}

	/**
	 * Request that the specified resource be reloaded and wait until the task is
	 * complete. This method submits a task to the task manager and waits for the
	 * task to be completed.
	 *
	 * @param resource The resource to reload
	 * @throws ExecutionException If there was an exception, reloading the resource
	 * @throws InterruptedException If the process of reloading the resource was interrupted
	 * @implNote Do not call from a UI thread
	 */
	public void reloadResourcesAndWait( Resource resource ) throws ExecutionException, InterruptedException {
		reloadResourcesAndWait( Collections.singletonList( resource ) );
	}

	/**
	 * Request that the specified resources be reloaded and wait until the task is
	 * complete. This method submits a task to the task manager and waits for the
	 * task to be completed.
	 *
	 * @param resources The resources to reload
	 * @throws ExecutionException If there was an exception reloading the resources
	 * @throws InterruptedException If the process of reloading the resources was interrupted
	 * @implNote Do not call from a UI thread
	 */
	public void reloadResourcesAndWait( Collection<Resource> resources ) throws ExecutionException, InterruptedException {
		program.getTaskManager().submit( new ReloadResourceTask( resources ) ).get();
	}

	/**
	 * Request that all modified resources be saved. This method submits a task to
	 * the task manager and returns immediately.
	 */
	public void saveAll() {
		saveResources( getModifiedResources() );
		autosave.reset();
	}

	/**
	 * Request that the specified resources be saved. This method submits a task
	 * to the task manager and returns immediately.
	 *
	 * @param resource The resource to save
	 */
	public void saveResources( Resource resource ) {
		saveResources( Collections.singletonList( resource ) );
	}

	/**
	 * Request that the specified resources be saved. This method submits a task
	 * to the task manager and returns immediately.
	 *
	 * @param resources The resources to save
	 */
	public void saveResources( Collection<Resource> resources ) {
		program.getTaskManager().submit( new SaveResourceTask( resources ) );
	}

	/**
	 * Request that the specified resources be saved and wait until the task is
	 * complete. This method submits a task to the task manager and waits for the
	 * task to be completed.
	 *
	 * @param resource The resource to save
	 * @throws ExecutionException If there was an exception, saving the resource
	 * @throws InterruptedException If the process of saving the resource was interrupted
	 * @implNote Do not call from a UI thread
	 */
	public void saveResourcesAndWait( Resource resource ) throws ExecutionException, InterruptedException {
		saveResourcesAndWait( Collections.singletonList( resource ) );
	}

	/**
	 * Request that the specified resources be saved and wait until the task is
	 * complete. This method submits a task to the task manager and waits for the
	 * task to be completed.
	 *
	 * @param resources The resources to save
	 * @throws ExecutionException If there was an exception, saving the resources
	 * @throws InterruptedException If the process of saving the resources was interrupted
	 * @implNote Do not call from a UI thread
	 */
	public void saveResourcesAndWait( Collection<Resource> resources ) throws ExecutionException, InterruptedException {
		program.getTaskManager().submit( new SaveResourceTask( resources ) ).get();
	}

	/**
	 * Request that the specified resources be closed. This method submits a task
	 * to the task manager and returns immediately.
	 *
	 * @param resource The resource to close.
	 */
	public void closeResources( Resource resource ) {
		closeResources( Collections.singletonList( resource ) );
	}

	/**
	 * Request that the specified resources be closed. This method submits a task
	 * to the task manager and returns immediately.
	 *
	 * @param resources The resources to close.
	 */
	public void closeResources( Collection<Resource> resources ) {
		program.getTaskManager().submit( new CloseResourceTask( resources ) );
	}

	/**
	 * Request that the specified resource be closed and wait until the task is
	 * complete. This method submits a task to the task manager and waits for the
	 * task to be completed.
	 *
	 * @param resource The resource to close.
	 * @throws ExecutionException If there was an exception closing the resource
	 * @throws InterruptedException If the process of closing the resource was interrupted
	 * @implNote Do not call from a UI thread
	 */
	public void closeResourceAndWait( Resource resource ) throws ExecutionException, InterruptedException {
		closeResourcesAndWait( Collections.singletonList( resource ) );
	}

	/**
	 * Request that the specified resources be closed and wait until the task is
	 * complete. This method submits a task to the task manager and waits for the
	 * task to be completed.
	 *
	 * @param resources The resources to close.
	 * @throws ExecutionException If there was an exception closing the resources
	 * @throws InterruptedException If the process of closing the resources was interrupted
	 * @implNote Do not call from a UI thread
	 */
	public void closeResourcesAndWait( Collection<Resource> resources ) throws ExecutionException, InterruptedException {
		program.getTaskManager().submit( new CloseResourceTask( resources ) ).get();
	}

	/**
	 * Request that the specified resources be deleted. This method submits a task to
	 * the task manager and returns immediately.
	 *
	 * @param resources The resources to close.
	 */
	public void deleteResources( Collection<Resource> resources ) {
		program.getTaskManager().submit( new DeleteResourceTask( resources ) );
	}

	/**
	 * Request that the specified resources be deleted and wait until the task is
	 * complete. This method submits a task to the task manager and waits for the
	 * task to be completed.
	 *
	 * @param resources The resources to delete.
	 * @throws ExecutionException If there was an exception, deleting the resources
	 * @throws InterruptedException If the process of deleting the resources was interrupted
	 * @implNote Do not call from a UI thread
	 */
	public void deleteResourcesAndWait( Collection<Resource> resources ) throws ExecutionException, InterruptedException {
		program.getTaskManager().submit( new DeleteResourceTask( resources ) ).get();
	}

	/**
	 * Get a collection of the supported codecs.
	 *
	 * @return A collection of all supported codecs
	 */
	public Collection<Codec> getCodecs() {
		return resourceTypes.values().stream().flatMap( t -> t.getCodecs().stream() ).collect( Collectors.toUnmodifiableSet() );
	}

	public Resource getParent( Resource resource ) throws ResourceException {
		if( !UriUtil.hasParent( resource.getUri() ) ) return Resource.NONE;
		Resource parent = resource.getParent();
		if( parent == null ) parent = createResource( UriUtil.getParent( resource.getUri() ) ).add( resource );
		return parent;
	}

	public Resource resolve( Resource resource, String name ) throws ResourceException {
		if( !resource.isFolder() ) return resource;
		if( name == null ) return resource;
		return createResource( resource.getUri().resolve( name.replace( " ", "%20" ) ) );
	}

	private Settings getSettings() {
		return program.getSettingsManager().getSettings( ManagerSettings.RESOURCE );
	}

	/**
	 * Determine the resource type for the given resource. The resource URI is
	 * used to find the asset type in the following order:
	 * <ol>
	 *   <li>Look up the resource type by the full URI</li>
	 *   <li>Look up the resource type by the URI scheme</li>
	 *   <li>Find all the codecs that match the URI</li>
	 *   <li>Sort the codecs by priority, select the highest</li>
	 *   <li>Use the resource type associated with the codec</li>
	 * </ol>
	 *
	 * @param resource The resource for which to resolve the resource type
	 * @return The auto-detected resource type
	 */
	public ResourceType autoDetectResourceType( Resource resource ) {
		ResourceType type = null;

		// Look for resource types assigned to specific codecs
		List<Codec> codecs = new ArrayList<>( autoDetectCodecs( resource ) );
		codecs.sort( new CodecPriorityComparator().reversed() );
		Codec codec = codecs.isEmpty() ? null : codecs.getFirst();
		if( codec != null ) type = codec.getResourceType();

		// Assign values to resource
		if( codec != null ) resource.setCodec( codec );
		if( type != null ) resource.setType( type );

		return type;
	}

	/**
	 * Determine the codec for the given resource by checking the file name, the
	 * first line, and the content type for a match with a supported resource
	 * type. When calling this method, the resource needs to already be open so
	 * that the information needed to determine the correct codec is defined in
	 * the resource.
	 * <p>
	 * Note: This method uses a URLConnection object to get the first line and
	 * content type of the resource. This means that the calling thread will be
	 * blocked during the IO operations used in URLConnection if the first line
	 * or the content type is needed to determine the resource type.
	 *
	 * @param resource The resource for which to find codecs
	 * @return The set of codecs that match the resource
	 */
	public Set<Codec> autoDetectCodecs( Resource resource ) {
		String uri = UriUtil.removeQueryAndFragment( resource.getUri() ).toString();
		String fileName = resource.getFileName();
		// FIXME Only query media type if there are supported codecs to compare with
		String mediaType = resource.getScheme().getMediaType( resource );
		// FIXME Only query first line if there are supported codecs to compare with
		String firstLine = resource.getScheme().getFirstLine( resource );

		Set<Codec> codecs = new HashSet<>();
		for( ResourceType resourceType : getResourceTypes() ) {
			codecs.addAll( resourceType.getSupportedCodecs( Codec.Pattern.URI, uri ) );
			codecs.addAll( resourceType.getSupportedCodecs( Codec.Pattern.SCHEME, uri ) );
			codecs.addAll( resourceType.getSupportedCodecs( Codec.Pattern.MEDIATYPE, mediaType ) );
			codecs.addAll( resourceType.getSupportedCodecs( Codec.Pattern.EXTENSION, fileName ) );
			codecs.addAll( resourceType.getSupportedCodecs( Codec.Pattern.FILENAME, fileName ) );
			codecs.addAll( resourceType.getSupportedCodecs( Codec.Pattern.FIRSTLINE, firstLine ) );
		}
		return codecs;
	}

	private Collection<Resource> removeAlreadyOpenResources( Collection<Resource> resources ) {
		Collection<Resource> filteredResources = new ArrayList<>( resources );
		for( Resource resource : openResources ) {
			filteredResources.remove( resource );
		}
		return filteredResources;
	}

	private boolean isManagedResourceOpen( Resource resource ) {
		boolean isResourceOpen = resource.isOpen();
		boolean isInOpenResources = openResources.contains( resource );

		// This is a double check to ensure things are consistent
		if( isResourceOpen != isInOpenResources ) log.atWarn().log( "Resource open: %s, %s", isResourceOpen, isInOpenResources );

		return isResourceOpen;
	}

	private void updateActionState() {
		newActionHandler.updateEnabled();
		openActionHandler.updateEnabled();
		reloadActionHandler.updateEnabled();
		saveActionHandler.updateEnabled();
		saveAsActionHandler.updateEnabled();
		saveAllActionHandler.updateEnabled();
		renameActionHandler.updateEnabled();
		closeActionHandler.updateEnabled();
		closeAllActionHandler.updateEnabled();
	}

	private void registerCodecs( Codec.Pattern patternType, Codec codec ) {
		Set<String> patterns = codec.getSupported( patternType );
		Map<String, Set<Codec>> codecs = registeredCodecs.computeIfAbsent( patternType, ( k ) -> new ConcurrentHashMap<>() );
		patterns.forEach( pattern -> codecs.computeIfAbsent( pattern, k -> new CopyOnWriteArraySet<>() ).add( codec ) );
	}

	private void unregisterCodecs( Codec.Pattern patternType, Codec codec ) {
		Set<String> patterns = codec.getSupported( patternType );
		Map<String, Set<Codec>> codecs = registeredCodecs.getOrDefault( patternType, new HashMap<>() );
		patterns.forEach( pattern -> codecs.getOrDefault( pattern, new HashSet<>() ).remove( codec ) );
	}

	/**
	 * Determine if the resource can be reloaded. The resource can be reloaded if
	 * the resource is not new and is already loaded.
	 *
	 * @param resource The resource to check
	 * @return True if the resource can be reloaded, false otherwise.
	 */
	private boolean canReloadResource( Resource resource ) {
		if( resource == null || resource.isNew() ) return false;
		return resource.isLoaded();
	}

	/**
	 * Determine if all the resources can be saved.
	 *
	 * @param resources The set of resources to check
	 * @return True if all the resources can be saved
	 */
	private boolean canSaveAllResources( Collection<Resource> resources ) {
		return resources.stream().mapToInt( a -> canSaveResource( a ) ? 0 : 1 ).sum() == 0;
	}

	/**
	 * Determine if any of the resources can be saved.
	 *
	 * @param resources The set of resources to check
	 * @return True if any of the resources can be saved
	 */
	private boolean canSaveAnyResources( Collection<Resource> resources ) {
		return resources.stream().mapToInt( a -> canSaveResource( a ) ? 1 : 0 ).sum() > 0;
	}

	/**
	 * Determine if the resource can be saved. The resource can be saved if the
	 * URI is null or if the URI scheme and codec can both save resources.
	 *
	 * @param resource The resource to check
	 * @return True if the resource can be saved, false otherwise.
	 */
	private boolean canSaveResource( Resource resource ) {
		if( resource == null ) return false;

		if( resource.isNew() ) return true;
		if( !resource.isModified() ) return false;

		// Check supported schemes.
		Scheme scheme = getScheme( resource.getUri().getScheme() );
		if( scheme == null ) return false;

		boolean result = false;
		try {
			Codec codec = resource.getCodec();
			result = scheme.canSave( resource ) && (codec == null || codec.canSave());
		} catch( ResourceException exception ) {
			log.atSevere().withCause( exception ).log( "Error checking if resource can be saved" );
		}

		return result;
	}

	/**
	 * Determine if the resource can be renamed. The resource can be renamed if
	 * the resource is not new and is open.
	 *
	 * @param resource The resource to check
	 * @return True if the resource can be renamed, false otherwise.
	 */
	boolean canRenameResource( Resource resource ) {
		return resource != null && !resource.isNew() && resource.isOpen();
	}

	public void registerResourceAlias( URI alias, URI uri ) {
		aliases.put( alias, uri );
	}

	public void unregisterResourceAlias( URI alias ) {
		aliases.remove( alias );
	}

	private URI resolveResourceAlias( URI uri ) {
		URI resolved = aliases.get( uri );
		if( resolved == null ) return uri;
		return resolved;
	}

	/**
	 * Create a resource from a resource type and/or a URI. The resource is
	 * considered to be a new resource if the URI is null. Otherwise, the resource
	 * is considered an existing resource. See {@link Resource#isNew()}
	 *
	 * @param type The resource type of the resource
	 * @param uri The URI of the resource
	 * @return The resource created from the resource type and URI
	 */
	private synchronized Resource doCreateResource( ResourceType type, URI uri ) throws ResourceException {
		if( uri == null ) uri = URI.create( NewScheme.ID + ":" + IdGenerator.getId() );

		uri = resolveResourceAlias( uri );

		// Many resources use query parameters and fragments in the URI,
		// so we need to clean up the URI before using it
		uri = uriCleanup( uri );

		Resource resource = identifiedResources.get( uri );
		if( resource == null ) {
			resource = new Resource( type, uri );
			resolveScheme( resource );
			identifiedResources.put( uri, resource );
			resource.setIcon( resource.isFolder() ? "folder" : "file" );
			log.atDebug().log( "Resource create: %s", resource );
		} else {
			log.atDebug().log( "Resource exists: %s", resource );
		}

		return resource;
	}

	private boolean doOpenResource( Resource resource ) throws ResourceException {
		if( isManagedResourceOpen( resource ) ) return true;

		// Determine the resource type
		ResourceType type = resource.getType();
		if( type == null ) type = autoDetectResourceType( resource );

		if( type == null ) {
			log.atWarn().log( "Resource type not found: " + resource.getMediaType() );
			String title = Rb.text( RbKey.LABEL, "asset" );
			String message = Rb.text( RbKey.RESOURCE, "asset-type-not-supported", resource.getFileName() );
			Notice notice = new Notice( title, message ).setType( Notice.Type.WARN );
			getProgram().getNoticeManager().addNotice( notice );
			return false;
		}

		// Determine the codec
		Codec codec = resource.getCodec();
		if( codec == null ) {
			codec = resource.getType().getDefaultCodec();
			resource.setCodec( codec );
		}
		log.atFiner().log( "Resource codec: %s", codec );

		// Initialize the resource
		if( !type.callResourceOpen( program, resource ) ) return false;
		log.atFiner().log( "Resource initialized with default values." );

		// Register the general resource listener
		resource.register( ResourceEvent.ANY, generalResourceWatcher );

		// Open the resource
		resource.open( this );

		// Add the resource to the list of open resources
		openResources.add( resource );

		getEventBus().dispatch( new ResourceEvent( this, ResourceEvent.OPENED, resource ) );
		log.atDebug().log( "Resource opened: %s", resource );

		updateActionState();
		return true;
	}

	private boolean doLoadResource( Resource resource ) throws ResourceException {
		if( resource == null ) return false;

		if( !resource.isNew() && !resource.exists() ) {
			log.atWarn().log( "Resource not found: " + resource );
			return false;
		}

		if( !resource.isOpen() ) doOpenResource( resource );
		if( !resource.getScheme().canLoad( resource ) ) return false;

		// Load the resource
		log.atTrace().log( "Loading resource " + resource.getUri() );
		resource.load( this );
		getEventBus().dispatch( new ResourceEvent( this, ResourceEvent.LOADED, resource ) );
		log.atInfo().log( "Loaded: %s", resource );

		updateActionState();
		return true;
	}

	private boolean doReloadResource( Resource resource ) throws ResourceException {
		if( resource == null || !resource.isLoaded() ) return false;

		resource.load( this );
		getEventBus().dispatch( new ResourceEvent( this, ResourceEvent.LOADED, resource ) );
		log.atFiner().log( "Resource reloaded: %s", resource );

		updateActionState();
		return true;
	}

	private boolean doSaveResource( Resource resource ) throws ResourceException {
		if( resource == null || !isManagedResourceOpen( resource ) || !resource.isSafeToSave() ) return false;

		if( !resource.getScheme().canSave( resource ) ) return false;

		resource.save( this );
		identifiedResources.put( resource.getUri(), resource );

		// TODO If the resource is changing URI the settings need to be moved

		// TODO Update the resource type.

		getEventBus().dispatch( new ResourceEvent( this, ResourceEvent.SAVED, resource ) );
		log.atInfo().log( "Saved: %s", resource );

		updateActionState();
		return true;
	}

	private boolean doCloseResource( Resource resource ) throws ResourceException {
		if( resource == null ) return false;
		if( !isManagedResourceOpen( resource ) ) return false;

		// Close the resource
		resource.close( this );

		// Unregister the general resource listener
		resource.unregister( ResourceEvent.ANY, generalResourceWatcher );

		// Remove the resource from the list of open resources
		openResources.remove( resource );
		identifiedResources.remove( resource.getUri() );

		if( openResources.isEmpty() ) doSetCurrentResource( null );

		// TODO Delete the resource settings?
		// Should the settings be removed? Or left for later?
		// Recommended not to delete the resource settings.
		// Maybe have a settings cleanup task and/or user actions

		getEventBus().dispatch( new ResourceEvent( this, ResourceEvent.CLOSED, resource ) );
		log.atDebug().log( "Resource closed: %s", resource );

		updateActionState();
		return true;
	}

	private boolean doDeleteResource( Resource resource ) throws ResourceException {
		if( resource == null ) return false;
		if( resource.isOpen() ) doCloseResource( resource );

		// Delete the resource
		resource.delete();

		getEventBus().dispatch( new ResourceEvent( this, ResourceEvent.DELETED, resource ) );
		log.atDebug().log( "Resource deleted: %s", resource );

		updateActionState();
		return true;
	}

	/**
	 * Save the resource, prompting the user if necessary.
	 *
	 * @param source The resource to be saved
	 * @param target The resource to save as
	 * @param saveAs The save-as flag
	 * @param rename The rename flag
	 * @implNote This method makes calls to the FX platform.
	 */
	private void doSaveOrRenameResource( Resource source, Resource target, boolean saveAs, boolean rename ) {
		try {
			boolean needsTargetResource = source.isNew() || ((saveAs || rename) && target == null);
			if( needsTargetResource ) {
				askForTargetResource( source, saveAs, rename );
			} else {
				saveResources( source );
			}
		} catch( ResourceException exception ) {
			log.atSevere().withCause( exception ).log();
		}
	}

	private String generateFilename() {
		return "resource" + (currentResource == null ? "" : "." + currentResource.getCodec().getDefaultExtension());
	}

	private void askForTargetResource( Resource source, boolean saveAs, boolean rename ) throws ResourceException {
		Codec codec = source.getCodec();
		if( codec == null ) codec = source.getType().getDefaultCodec();

		// Determine the resource path
		Path folder = source.isNew() ? getCurrentFileFolder() : Path.of( getParent( source ).getUri() );
		String filename = source.isNew() ? generateFilename() : source.getFileName();
		Path resourcePath = folder.resolve( filename );

		// Build a URI to open the resource tool
		String uriString = ProgramResourceType.URI + "?mode=" + ResourceTool.Mode.SAVE + "&uri=" + resourcePath.toUri();
		log.atTrace().log( "save resource uri=%s", URI.create( uriString ) );

		final Resource finalResource = source;
		final Codec finalCodec = codec;
		program.getTaskManager().submit( Task.of( () -> {
			try {
				Map<Codec, ResourceFilter> filters = generateResourceFilters( finalResource.getType() );
				ResourceTool tool = (ResourceTool)openResource( URI.create( uriString ) ).get();
				tool.getFilters().addAll( 0, filters.values() );
				tool.setSelectedFilter( filters.get( finalCodec ) );
				tool.setSaveActionConsumer( target -> doAfterResourceTool( tool, filters, source, target, saveAs, rename ) );
			} catch( Exception exception ) {
				log.atWarn().withCause( exception ).log();
			}
		} ) );
	}

	private void doAfterResourceTool( ResourceTool tool, Map<Codec, ResourceFilter> filters, Resource source, Resource target, boolean saveAs, boolean rename ) {
		try {
			Resource folder = target.isFolder() ? target : getParent( target );

			// Store the current folder in the settings
			setCurrentFileFolder( folder );

			// If the user specified a codec, use it to set the codec and resource type
			Map<ResourceFilter, Codec> filterCodecs = MapUtil.mirror( filters );
			Codec selectedCodec = filterCodecs.get( tool.getSelectedFilter() );

			// If the extension is not already supported, use the default extension from the codec
			if( !target.exists() && selectedCodec != null && !selectedCodec.isSupported( Codec.Pattern.EXTENSION, target.getFileName() ) ) {
				target = resolve( folder, target.getFileName() + "." + selectedCodec.getDefaultExtension() );
			}

			// Update the target resource
			if( selectedCodec != null ) target.setCodec( selectedCodec );

			if( source.isNew() || saveAs ) {
				doSaveAsResource( source, target );
			} else if( rename ) {
				doRenameResource( source, target );
			}
		} catch( ResourceException exception ) {
			log.atError( exception ).log();
		}
	}

	private void doSaveAsResource( Resource source, Resource target ) throws ResourceException {
		if( source == null || target == null ) return;

		copySettings( source, target, false );

		// Use the scheme to save the source to the target
		target.getScheme().saveAs( source, target );

		if( source.isNew() ) closeResources( source );
		openResource( target.getUri() );
	}

	private void doRenameResource( Resource source, Resource target ) throws ResourceException {
		if( source == null || target == null ) return;

		copySettings( source, target, true );

		// Use the scheme to rename the source to the target
		target.getScheme().rename( source, target );

		openResource( target.getUri() );
		closeResources( source );
	}

	private void copySettings( Resource source, Resource target, boolean delete ) {
		Settings sourceSettings = getProgram().getSettingsManager().getResourceSettings( source );
		Settings targetSettings = getProgram().getSettingsManager().getResourceSettings( target );
		targetSettings.copyFrom( sourceSettings );
		if( delete ) sourceSettings.delete();
	}

	private Map<Codec, ResourceFilter> generateResourceFilters( ResourceType type ) {
		Map<Codec, ResourceFilter> filters = new HashMap<>();
		type.getCodecs().forEach( c -> filters.put( c, new CodecResourceFilter( c ) ) );
		return filters;
	}

	URI uriCleanup( URI uri ) {
		return UriUtil.removeQueryAndFragment( uri ).normalize();
	}

	private boolean doSetCurrentResource( Resource resource ) {
		synchronized( currentResourceLock ) {
			//log.log( Log.WARN,  "Current resource: " + currentResource + " new resource: " + resource );
			Resource previous = currentResource;

			// "Disconnect" the old current resource
			if( currentResource != null ) {
				currentResource.getEventHub().dispatch( new ResourceEvent( this, ResourceEvent.DEACTIVATED, currentResource ) );
				currentResource.getEventHub().unregister( ResourceEvent.ANY, currentResourceWatcher );
			}

			// Change current resource
			currentResource = resource;

			// "Connect" the new current resource
			if( currentResource != null ) {
				currentResource.getEventHub().register( ResourceEvent.ANY, currentResourceWatcher );
				currentResource.getEventHub().dispatch( new ResourceEvent( this, ResourceEvent.ACTIVATED, currentResource ) );
			}

			// Notify program of current resource change
			getEventBus().dispatch( new ResourceSwitchedEvent( this, ResourceSwitchedEvent.SWITCHED, previous, currentResource ) );
			log.atFiner().log( "Resource select: %s", resource );
		}

		updateActionState();
		return true;
	}

	private class NewOrOpenResourceTask extends Task<ProgramTool> {

		private final OpenResourceRequest request;

		public NewOrOpenResourceTask( OpenResourceRequest request ) {
			this.request = request;
		}

		@Override
		public ProgramTool call() throws ResourceException, ExecutionException, TimeoutException, InterruptedException {
			// Create and configure the resource
			if( request.getResource() == null ) request.setResource( createResource( request.getType(), request.getUri() ) );

			Resource resource = request.getResource();
			Object model = request.getModel();
			Codec codec = request.getCodec();
			if( model != null ) resource.setModel( model );
			if( codec != null ) resource.setCodec( codec );

			// Open the resource
			openResourcesAndWait( resource, 5, TimeUnit.SECONDS );
			//if( !isManagedResourceOpen( resource ) ) return null;

			// Create the tool if needed
			ProgramTool tool = null;
			try {
				// If the resource is "new", get user input from the resource type
				if( resource.isNew() ) {
					if( !resource.getType().callResourceNew( program, resource ) ) return null;
					log.atFiner().log( "Resource initialized with user values." );

					// The resource type may have changed the URI so resolve the scheme again
					resolveScheme( resource );
				}

				if( resource.getType() == null ) log.atError().log( "Resource type is null for: %s", resource );

				if( request.isOpenTool() ) tool = program.getToolManager().openTool( request );
			} catch( NoToolRegisteredException exception ) {
				log.atConfig().log( "No tool registered for: %s", resource );
				String title = Rb.text( "program", "no-tool-for-asset-title" );
				String message = Rb.text( "program", "no-tool-for-asset-message", resource.getUri().toString() );
				program.getNoticeManager().warning( title, message, resource.getName() );
				return null;
			}

			// Start loading the asset after the tool has been created
			if( !resource.isLoaded() ) loadResources( resource );

			return tool;
		}

	}

	private class NewActionHandler extends ProgramAction {

		private NewActionHandler( Xenon program ) {
			super( program );
		}

		@Override
		public boolean isEnabled() {
			return !getUserResourceTypes().isEmpty();
		}

		@Override
		public void handle( ActionEvent event ) {
			Collection<ResourceType> types = getUserResourceTypes();

			if( types.size() == 1 ) {
				newResource( types.iterator().next() );
			} else {
				openResource( ProgramResourceNewType.URI );
			}
		}

	}

	private class OpenActionHandler extends ProgramAction {

		private boolean isHandling;

		private OpenActionHandler( Xenon program ) {
			super( program );
		}

		@Override
		public boolean isEnabled() {
			return !isHandling && !getUserResourceTypes().isEmpty();
		}

		@Override
		public void handle( ActionEvent event ) {
			// Disable the action while the dialog is open
			isHandling = true;
			updateEnabled();

			openResource( ProgramResourceType.OPEN_URI );

			isHandling = false;
			updateActionState();
		}

	}

	private class ReloadActionHandler extends ProgramAction {

		protected ReloadActionHandler( Xenon program ) {
			super( program );
		}

		@Override
		public boolean isEnabled() {
			return canReloadResource( getCurrentResource() );
		}

		@Override
		public void handle( ActionEvent event ) {
			reloadResource( getCurrentResource() );
		}

	}

	private class SaveActionHandler extends ProgramAction {

		private final boolean saveAs;

		private SaveActionHandler( Xenon program, boolean saveAs ) {
			super( program );
			this.saveAs = saveAs;
		}

		@Override
		public boolean isEnabled() {
			return (saveAs && getCurrentResource() != null) || canSaveResource( getCurrentResource() );
		}

		@Override
		public void handle( ActionEvent event ) {
			saveAsResource( getCurrentResource(), null );
		}

	}

	private class SaveAllActionHandler extends ProgramAction {

		private SaveAllActionHandler( Xenon program ) {
			super( program );
		}

		@Override
		public boolean isEnabled() {
			return canSaveAnyResources( getModifiedResources() );
		}

		@Override
		public void handle( ActionEvent event ) {
			try {
				autosave.trigger();
			} catch( Exception exception ) {
				log.atSevere().withCause( exception ).log();
			}
		}

	}

	private class RenameActionHandler extends ProgramAction {

		private RenameActionHandler( Xenon program ) {
			super( program );
		}

		@Override
		public boolean isEnabled() {
			return canRenameResource( getCurrentResource() );
		}

		@Override
		public void handle( ActionEvent event ) {
			renameResource( getCurrentResource(), null );
		}

	}

	private class CloseActionHandler extends ProgramAction {

		private CloseActionHandler( Xenon program ) {
			super( program );
		}

		@Override
		public boolean isEnabled() {
			return openResources.stream().anyMatch( a -> a.getType().isUserType() );
		}

		@Override
		public void handle( ActionEvent event ) {
			try {
				closeResources( getCurrentResource() );
			} catch( Exception exception ) {
				log.atSevere().withCause( exception ).log();
			}
		}

	}

	private class CloseAllActionHandler extends ProgramAction {

		private CloseAllActionHandler( Xenon program ) {
			super( program );
		}

		@Override
		public boolean isEnabled() {
			return canSaveResource( getCurrentResource() );
		}

		@Override
		public void handle( ActionEvent event ) {
			try {
				closeResources( openResources );
			} catch( Exception exception ) {
				log.atSevere().withCause( exception ).log();
			}
		}

	}

	private abstract class AssetTask extends ProgramTask<Collection<Resource>> {

		private final Collection<Resource> resources;

		private AssetTask( Collection<Resource> resources ) {
			super( program );
			this.resources = resources;
		}

		@Override
		public Collection<Resource> call() {
			List<Resource> result = new ArrayList<>();
			Map<Throwable, Resource> throwables = new HashMap<>();
			if( resources == null ) {
				try {
					doOperation( null );
				} catch( Throwable throwable ) {
					throwables.put( throwable, Resource.NONE );
				}
			} else {
				for( Resource resource : resources ) {
					try {
						if( doOperation( resource ) ) result.add( resource );
					} catch( Throwable throwable ) {
						throwables.put( throwable, resource );
					}
				}
			}

			if( !throwables.isEmpty() ) {
				for( Throwable throwable : throwables.keySet() ) {
					String errorName = throwable.getClass().getSimpleName();
					String taskName = getClass().getSimpleName();
					String message = Rb.text( "program", "task-error-message", errorName, taskName );
					if( TestUtil.isTest() ) throwable.printStackTrace( System.err );
					log.atWarning().withCause( throwable ).log( message );
				}
			}

			return result;
		}

		abstract boolean doOperation( Resource resource ) throws ResourceException;

		@Override
		public String toString() {
			if( resources == null || resources.isEmpty() ) return super.toString() + ": none";
			return super.toString() + ": " + resources.iterator().next().toString();
		}

	}

	private class OpenResourceTask extends AssetTask {

		private OpenResourceTask( Collection<Resource> resources ) {
			super( resources );
		}

		@Override
		public boolean doOperation( Resource resource ) throws ResourceException {
			return doOpenResource( resource );
		}

	}

	private class LoadResourceTask extends AssetTask {

		private LoadResourceTask( Collection<Resource> resources ) {
			super( resources );
		}

		@Override
		public boolean doOperation( Resource resource ) throws ResourceException {
			return doLoadResource( resource );
		}

	}

	private class ReloadResourceTask extends AssetTask {

		private ReloadResourceTask( Collection<Resource> resources ) {
			super( resources );
		}

		@Override
		public boolean doOperation( Resource resource ) throws ResourceException {
			return doReloadResource( resource );
		}

	}

	private class SaveResourceTask extends AssetTask {

		private SaveResourceTask( Collection<Resource> resources ) {
			super( resources );
		}

		@Override
		public boolean doOperation( Resource resource ) throws ResourceException {
			return doSaveResource( resource );
		}

	}

	private class CloseResourceTask extends AssetTask {

		private CloseResourceTask( Collection<Resource> resources ) {
			super( resources );
		}

		@Override
		public boolean doOperation( Resource resource ) throws ResourceException {
			return doCloseResource( resource );
		}

	}

	private class DeleteResourceTask extends AssetTask {

		private DeleteResourceTask( Collection<Resource> resources ) {
			super( resources );
		}

		@Override
		public boolean doOperation( Resource resource ) throws ResourceException {
			return doDeleteResource( resource );
		}

	}

	private class SetCurrentResourceTask extends AssetTask {

		private SetCurrentResourceTask( Resource resource ) {
			// A null collection will call the operation with a null value
			super( resource == null ? null : Set.of( resource ) );
		}

		@Override
		public boolean doOperation( Resource resource ) {
			return doSetCurrentResource( resource );
		}

	}

	private class CurrentResourceWatcher implements EventHandler<ResourceEvent> {

		@Override
		public void handle( ResourceEvent event ) {
			//System.err.println( "asset event=" + event );
			if( event.getEventType() == ResourceEvent.MODIFIED ) updateActionState();
			if( event.getEventType() == ResourceEvent.UNMODIFIED ) updateActionState();
		}

	}

	private class GeneralResourceWatcher implements EventHandler<ResourceEvent> {

		@Override
		public void handle( ResourceEvent event ) {
			autosave.request();
		}

	}

}
