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
import com.acromere.xenon.tool.AssetTool;
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

	public Resource createAsset( Object descriptor ) throws ResourceException {
		if( descriptor instanceof URI ) {
			return (createAsset( (URI)descriptor ));
		} else if( descriptor instanceof File ) {
			return (createAsset( ((File)descriptor).toURI() ));
		} else if( descriptor instanceof Path ) {
			return (createAsset( ((Path)descriptor).toUri() ));
		} else {
			return (createAsset( descriptor.toString() ));
		}
	}

	/**
	 * Create an asset from a string. This asset is considered to be an old asset. See {@link Resource#isNew()}
	 *
	 * @param string The asset string
	 * @return A new asset based on the specified string.
	 */
	public Resource createAsset( String string ) throws ResourceException {
		if( string == null ) return null;

		URI uri = UriUtil.resolve( string );

		if( uri == null ) {
			String title = Rb.text( "asset", "assets" );
			String message = Rb.text( "program", "asset-unable-to-resolve" );
			program.getNoticeManager().warning( title, message, string );
			return null;
		}

		return createAsset( uri );
	}

	/**
	 * Create an asset from a URI. This asset is considered to be an old asset. See {@link Resource#isNew()}
	 *
	 * @param uri The URI to create an asset from
	 * @return The asset created from the URI
	 */
	public Resource createAsset( URI uri ) throws ResourceException {
		return doCreateAsset( null, uri );
	}

	/**
	 * Create an asset from a file. This asset is considered to be an old asset. See {@link Resource#isNew()}
	 *
	 * @param file The file to create an asset from
	 * @return The asset created from the file
	 */
	@Deprecated
	public Resource createAsset( File file ) throws ResourceException {
		return doCreateAsset( null, file.toURI() );
	}

	/**
	 * Create an asset from a path. This asset is considered to be an old asset. See {@link Resource#isNew()}
	 *
	 * @param path The path to create an asset from
	 * @return The asset created from the path
	 */
	public Resource createAsset( Path path ) throws ResourceException {
		return doCreateAsset( null, path.toUri() );
	}

	/**
	 * Create an asset from an asset type. This asset is considered to be a new asset. See {@link Resource#isNew()}
	 *
	 * @param type The asset type to create an asset from
	 * @return The asset created from the asset type
	 */
	public Resource createAsset( ResourceType type ) throws ResourceException {
		return doCreateAsset( type, null );
	}

	public Resource createAsset( ResourceType type, String uri ) throws ResourceException {
		return doCreateAsset( type, UriUtil.resolve( uri ) );
	}

	/**
	 * Create an asset from an asset type and uri.
	 *
	 * @param type The asset type
	 * @param uri The asset uri
	 * @return The created asset
	 */
	public Resource createAsset( ResourceType type, URI uri ) throws ResourceException {
		return doCreateAsset( type, uri );
	}

	/**
	 * Create assets from an array of descriptors. Descriptors are preferred in the following order: URI, File, String, Object
	 *
	 * @param descriptors The descriptors from which to create assets
	 * @return The list of assets created from the descriptors
	 */
	public Collection<Resource> createAssets( Object... descriptors ) throws ResourceException {
		return createAssets( List.of( descriptors ) );
	}

	/**
	 * Create assets from a collection of descriptors. Descriptors are preferred in the following order: URI, File, String, Object
	 *
	 * @param descriptors The descriptors from which to create assets
	 * @return The list of assets created from the descriptors
	 */
	public Collection<Resource> createAssets( Collection<?> descriptors ) throws ResourceException {
		List<Resource> resources = new ArrayList<>( descriptors.size() );

		for( Object descriptor : descriptors ) {
			resources.add( createAsset( descriptor ) );
		}

		return resources;
	}

	/**
	 * Request that the specified assets be opened. This method submits a task to the task manager and returns immediately.
	 *
	 * @param resources The assets to open
	 */
	public void openAssets( Resource... resources ) throws ResourceException {
		openAssets( List.of( resources ) );
	}

	/**
	 * Request that the specified assets be opened. This method submits a task to the task manager and returns immediately.
	 *
	 * @param resources The assets to open
	 */
	public void openAssets( Collection<Resource> resources ) throws ResourceException {
		program.getTaskManager().submit( new OpenAssetTask( removeAlreadyOpenAssets( resources ) ) );
	}

	/**
	 * Request that the specified assets be opened and wait until the task is complete. This method submits a task to the task manager and waits for the task
	 * to be completed.
	 *
	 * @param resource The asset to open
	 * @throws ExecutionException If there was an exception opening the asset
	 * @throws InterruptedException If the process of opening the asset was interrupted
	 * @implNote Do not call from a UI thread
	 */
	public void openAssetsAndWait( Resource resource, long time, TimeUnit unit ) throws ExecutionException, InterruptedException, TimeoutException {
		openAssetsAndWait( List.of( resource ), time, unit );
	}

	/**
	 * Request that the specified assets be opened and wait until the task is complete. This method submits a task to the task manager and waits for the task
	 * to be completed.
	 *
	 * @param resources The assets to open
	 * @throws ExecutionException If there was an exception opening an asset
	 * @throws InterruptedException If the process of opening an asset was interrupted
	 * @implNote Do not call from a UI thread
	 */
	public void openAssetsAndWait( Collection<Resource> resources, long time, TimeUnit unit ) throws ExecutionException, InterruptedException, TimeoutException {
		program.getTaskManager().submit( new OpenAssetTask( removeAlreadyOpenAssets( resources ) ) ).get( time, unit );
	}

	/**
	 * Request that the specified assets be loaded. This method submits a task to the task manager and returns immediately.
	 *
	 * @param resources The assets to load
	 */
	public Future<Collection<Resource>> loadAssets( Resource... resources ) {
		return loadAssets( List.of( resources ) );
	}

	/**
	 * Request that the specified assets be loaded. This method submits a task to the task manager and returns immediately.
	 *
	 * @param resources The assets to load
	 */
	public Future<Collection<Resource>> loadAssets( Collection<Resource> resources ) {
		return program.getTaskManager().submit( new LoadAssetTask( resources ) );
	}

	/**
	 * Request that the specified assets be loaded and wait until the task is complete. This method submits a task to the task manager and waits for the task
	 * to be completed.
	 *
	 * @param resources The assets to load
	 * @throws ExecutionException If there was an exception loading the asset
	 * @throws InterruptedException If the process of loading the asset was interrupted
	 * @implNote Do not call from a UI thread
	 */
	public void loadAssetsAndWait( Resource... resources ) throws ExecutionException, InterruptedException {
		loadAssetsAndWait( List.of( resources ) );
	}

	/**
	 * Request that the specified assets be loaded and wait until the task is complete. This method submits a task to the task manager and waits for the task
	 * to be completed.
	 *
	 * @param resources The assets to load
	 * @throws ExecutionException If there was an exception loading the assets
	 * @throws InterruptedException If the process of loading the assets was interrupted
	 * @implNote Do not call from a UI thread
	 */
	public void loadAssetsAndWait( Collection<Resource> resources ) throws ExecutionException, InterruptedException {
		program.getTaskManager().submit( new LoadAssetTask( resources ) ).get();
	}

	/**
	 * Request that the specified assets be reloaded. This method submits a task
	 * to the task manager and returns immediately.
	 *
	 * @param resource The asset to reload
	 */
	public void reloadResources( Resource resource ) {
		reloadResources( Collections.singletonList( resource ) );
	}

	/**
	 * Request that the specified assets be reloaded. This method submits a task
	 * to the task manager and returns immediately.
	 *
	 * @param resources The assets to reload
	 */
	public void reloadResources( Collection<Resource> resources ) {
		program.getTaskManager().submit( new ReloadAssetTask( resources ) );
	}

	/**
	 * Request that the specified assets be reloaded and wait until the task is
	 * complete. This method submits a task to the task manager and waits for the
	 * task to be completed.
	 *
	 * @param resource The asset to reload
	 * @throws ExecutionException If there was an exception reloading the asset
	 * @throws InterruptedException If the process of reloading the asset was interrupted
	 * @implNote Do not call from a UI thread
	 */
	public void reloadAssetsAndWait( Resource resource ) throws ExecutionException, InterruptedException {
		reloadAssetsAndWait( Collections.singletonList( resource ) );
	}

	/**
	 * Request that the specified assets be reloaded and wait until the task is
	 * complete. This method submits a task to the task manager and waits for the
	 * task to be completed.
	 *
	 * @param resources The assets to reload
	 * @throws ExecutionException If there was an exception reloading the assets
	 * @throws InterruptedException If the process of reloading the assets was interrupted
	 * @implNote Do not call from a UI thread
	 */
	public void reloadAssetsAndWait( Collection<Resource> resources ) throws ExecutionException, InterruptedException {
		program.getTaskManager().submit( new ReloadAssetTask( resources ) ).get();
	}

	/**
	 * Request that all modified assets be saved. This method submits a task to
	 * the task manager and returns immediately.
	 */
	public void saveAll() {
		saveAssets( getModifiedResources() );
		autosave.reset();
	}

	/**
	 * Request that the specified assets be saved. This method submits a task to
	 * the task manager and returns immediately.
	 *
	 * @param resource The asset to save
	 */
	public void saveAssets( Resource resource ) {
		saveAssets( Collections.singletonList( resource ) );
	}

	/**
	 * Request that the specified assets be saved. This method submits a task to the task manager and returns immediately.
	 *
	 * @param resources The assets to save
	 */
	public void saveAssets( Collection<Resource> resources ) {
		program.getTaskManager().submit( new SaveAssetTask( resources ) );
	}

	/**
	 * Request that the specified assets be saved and wait until the task is complete. This method submits a task to the task manager and waits for the task to
	 * be completed.
	 *
	 * @param resource The asset to save
	 * @throws ExecutionException If there was an exception, saving the asset
	 * @throws InterruptedException If the process of saving the asset was interrupted
	 * @implNote Do not call from a UI thread
	 */
	public void saveAssetsAndWait( Resource resource ) throws ExecutionException, InterruptedException {
		saveAssetsAndWait( Collections.singletonList( resource ) );
	}

	/**
	 * Request that the specified assets be saved and wait until the task is complete. This method submits a task to the task manager and waits for the task to
	 * be completed.
	 *
	 * @param resources The assets to save
	 * @throws ExecutionException If there was an exception saving the assets
	 * @throws InterruptedException If the process of saving the assets was interrupted
	 * @implNote Do not call from a UI thread
	 */
	public void saveAssetsAndWait( Collection<Resource> resources ) throws ExecutionException, InterruptedException {
		program.getTaskManager().submit( new SaveAssetTask( resources ) ).get();
	}

	/**
	 * Request that the specified assets be closed. This method submits a task to the task manager and returns immediately.
	 *
	 * @param resource The asset to close.
	 */
	public void closeResources( Resource resource ) {
		closeResources( Collections.singletonList( resource ) );
	}

	/**
	 * Request that the specified assets be closed. This method submits a task to the task manager and returns immediately.
	 *
	 * @param resources The assets to close.
	 */
	public void closeResources( Collection<Resource> resources ) {
		program.getTaskManager().submit( new CloseAssetTask( resources ) );
	}

	/**
	 * Request that the specified assets be closed and wait until the task is complete. This method submits a task to the task manager and waits for the task
	 * to be completed.
	 *
	 * @param resource The assets to close.
	 * @throws ExecutionException If there was an exception closing the asset
	 * @throws InterruptedException If the process of closing the asset was interrupted
	 * @implNote Do not call from a UI thread
	 */
	public void closeAssetsAndWait( Resource resource ) throws ExecutionException, InterruptedException {
		closeAssetsAndWait( Collections.singletonList( resource ) );
	}

	/**
	 * Request that the specified assets be closed and wait until the task is complete. This method submits a task to the task manager and waits for the task
	 * to be completed.
	 *
	 * @param resources The assets to close.
	 * @throws ExecutionException If there was an exception closing the assets
	 * @throws InterruptedException If the process of closing the assets was interrupted
	 * @implNote Do not call from a UI thread
	 */
	public void closeAssetsAndWait( Collection<Resource> resources ) throws ExecutionException, InterruptedException {
		program.getTaskManager().submit( new CloseAssetTask( resources ) ).get();
	}

	/**
	 * Request that the specified assets be deleted. This method submits a task to
	 * the task manager and returns immediately.
	 *
	 * @param resources The assets to close.
	 */
	public void deleteAssets( Collection<Resource> resources ) {
		program.getTaskManager().submit( new DeleteAssetTask( resources ) );
	}

	/**
	 * Request that the specified assets be deleted and wait until the task is
	 * complete. This method submits a task to the task manager and waits for the
	 * task to be completed.
	 *
	 * @param resources The assets to delete.
	 * @throws ExecutionException If there was an exception deleting the assets
	 * @throws InterruptedException If the process of deleting the assets was interrupted
	 * @implNote Do not call from a UI thread
	 */
	public void deleteAssetsAndWait( Collection<Resource> resources ) throws ExecutionException, InterruptedException {
		program.getTaskManager().submit( new DeleteAssetTask( resources ) ).get();
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
		if( parent == null ) parent = createAsset( UriUtil.getParent( resource.getUri() ) ).add( resource );
		return parent;
	}

	public Resource resolve( Resource resource, String name ) throws ResourceException {
		if( !resource.isFolder() ) return resource;
		if( name == null ) return resource;
		return createAsset( resource.getUri().resolve( name.replace( " ", "%20" ) ) );
	}

	private Settings getSettings() {
		return program.getSettingsManager().getSettings( ManagerSettings.ASSET );
	}

	/**
	 * Determine the asset type for the given asset. The asset URI is used to find
	 * the asset type in the following order:
	 * <ol>
	 *   <li>Lookup the asset type by the full URI</li>
	 *   <li>Lookup the asset type by the URI scheme</li>
	 *   <li>Find all the codecs that match the URI</li>
	 *   <li>Sort the codecs by priority, select the highest</li>
	 *   <li>Use the asset type associated to the codec</li>
	 * </ol>
	 *
	 * @param resource The asset for which to resolve the asset type
	 * @return The auto detected asset type
	 */
	public ResourceType autoDetectAssetType( Resource resource ) {
		ResourceType type = null;

		// Look for asset types assigned to specific codecs
		List<Codec> codecs = new ArrayList<>( autoDetectCodecs( resource ) );
		codecs.sort( new CodecPriorityComparator().reversed() );
		Codec codec = codecs.isEmpty() ? null : codecs.getFirst();
		if( codec != null ) type = codec.getResourceType();

		// Assign values to asset
		if( codec != null ) resource.setCodec( codec );
		if( type != null ) resource.setType( type );

		return type;
	}

	/**
	 * Determine the codec for the given asset by checking the file name, the
	 * first line, and the content type for a match with a supported asset
	 * type. When calling this method the asset needs to already be open so
	 * that the information needed to determine the correct codec is defined in
	 * the asset.
	 * <p>
	 * Note: This method uses a URLConnection object to get the first line and
	 * content type of the asset. This means that the calling thread will be
	 * blocked during the IO operations used in URLConnection if the first line
	 * or the content type is needed to determine the asset type.
	 *
	 * @param resource The asset for which to find codecs
	 * @return The set of codecs that match the asset
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

	private Collection<Resource> removeAlreadyOpenAssets( Collection<Resource> resources ) {
		Collection<Resource> filteredResources = new ArrayList<>( resources );
		for( Resource resource : openResources ) {
			filteredResources.remove( resource );
		}
		return filteredResources;
	}

	private boolean isManagedAssetOpen( Resource resource ) {
		boolean isAssetOpen = resource.isOpen();
		boolean isInOpenAssets = openResources.contains( resource );

		// This is a double check to ensure things are consistent
		if( isAssetOpen != isInOpenAssets ) log.atWarn().log( "Asset open: %s, %s", isAssetOpen, isInOpenAssets );

		return isAssetOpen;
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
	 * Determine if the asset can be reloaded. The asset can be reloaded if the
	 * asset is not new and is already loaded.
	 *
	 * @param resource The asset to check
	 * @return True if the asset can be reloaded, false otherwise.
	 */
	private boolean canReloadAsset( Resource resource ) {
		if( resource == null || resource.isNew() ) return false;
		return resource.isLoaded();
	}

	/**
	 * Determine if all the assets can be saved.
	 *
	 * @param resources The set of assets to check
	 * @return True if all the assets can be saved
	 */
	private boolean canSaveAllAssets( Collection<Resource> resources ) {
		return resources.stream().mapToInt( a -> canSaveResource( a ) ? 0 : 1 ).sum() == 0;
	}

	/**
	 * Determine if any of the assets can be saved.
	 *
	 * @param resources The set of assets to check
	 * @return True if any of the assets can be saved
	 */
	private boolean canSaveAnyAssets( Collection<Resource> resources ) {
		return resources.stream().mapToInt( a -> canSaveResource( a ) ? 1 : 0 ).sum() > 0;
	}

	/**
	 * Determine if the asset can be saved. The asset can be saved if the URI is
	 * null or if the URI scheme and codec can both save assets.
	 *
	 * @param resource The asset to check
	 * @return True if the asset can be saved, false otherwise.
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
			log.atSevere().withCause( exception ).log( "Error checking if asset can be saved" );
		}

		return result;
	}

	/**
	 * Determine if the asset can be renamed. The asset can be renamed if the
	 * asset is not new and is open.
	 *
	 * @param resource The asset to check
	 * @return True if the asset can be renamed, false otherwise.
	 */
	boolean canRenameAsset( Resource resource ) {
		return resource != null && !resource.isNew() && resource.isOpen();
	}

	public void registerAssetAlias( URI alias, URI uri ) {
		aliases.put( alias, uri );
	}

	public void unregisterAssetAlias( URI alias ) {
		aliases.remove( alias );
	}

	private URI resolveAssetAlias( URI uri ) {
		URI resolved = aliases.get( uri );
		if( resolved == null ) return uri;
		return resolved;
	}

	/**
	 * Create an asset from an asset type and/or a URI. The asset is considered to be a new asset if the URI is null. Otherwise, the asset is
	 * considered an old asset. See {@link Resource#isNew()}
	 *
	 * @param type The asset type of the asset
	 * @param uri The URI of the asset
	 * @return The asset created from the asset type and URI
	 */
	private synchronized Resource doCreateAsset( ResourceType type, URI uri ) throws ResourceException {
		if( uri == null ) uri = URI.create( NewScheme.ID + ":" + IdGenerator.getId() );

		uri = resolveAssetAlias( uri );

		// Many assets use query parameters and fragments in the URI,
		// so we need to clean up the URI before using it
		uri = uriCleanup( uri );

		Resource resource = identifiedResources.get( uri );
		if( resource == null ) {
			resource = new Resource( type, uri );
			resolveScheme( resource );
			identifiedResources.put( uri, resource );
			resource.setIcon( resource.isFolder() ? "folder" : "file" );
			log.atDebug().log( "Asset create: %s", resource );
		} else {
			log.atDebug().log( "Asset exists: %s", resource );
		}

		return resource;
	}

	private boolean doOpenAsset( Resource resource ) throws ResourceException {
		if( isManagedAssetOpen( resource ) ) return true;

		// Determine the asset type
		ResourceType type = resource.getType();
		if( type == null ) type = autoDetectAssetType( resource );

		if( type == null ) {
			log.atWarn().log( "Asset type not found: " + resource.getMediaType() );
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
		log.atFiner().log( "Asset codec: %s", codec );

		// Initialize the asset
		if( !type.callAssetOpen( program, resource ) ) return false;
		log.atFiner().log( "Asset initialized with default values." );

		// Register the general asset listener
		resource.register( ResourceEvent.ANY, generalResourceWatcher );

		// Open the asset
		resource.open( this );

		// Add the asset to the list of open assets
		openResources.add( resource );

		getEventBus().dispatch( new ResourceEvent( this, ResourceEvent.OPENED, resource ) );
		log.atDebug().log( "Asset opened: %s", resource );

		updateActionState();
		return true;
	}

	private boolean doLoadAsset( Resource resource ) throws ResourceException {
		if( resource == null ) return false;

		if( !resource.isNew() && !resource.exists() ) {
			log.atWarn().log( "Asset not found: " + resource );
			return false;
		}

		if( !resource.isOpen() ) doOpenAsset( resource );
		if( !resource.getScheme().canLoad( resource ) ) return false;

		// Load the asset
		log.atTrace().log( "Loading asset " + resource.getUri() );
		resource.load( this );
		getEventBus().dispatch( new ResourceEvent( this, ResourceEvent.LOADED, resource ) );
		log.atInfo().log( "Loaded: %s", resource );

		updateActionState();
		return true;
	}

	private boolean doReloadAsset( Resource resource ) throws ResourceException {
		if( resource == null || !resource.isLoaded() ) return false;

		resource.load( this );
		getEventBus().dispatch( new ResourceEvent( this, ResourceEvent.LOADED, resource ) );
		log.atFiner().log( "Asset reloaded: %s", resource );

		updateActionState();
		return true;
	}

	private boolean doSaveAsset( Resource resource ) throws ResourceException {
		if( resource == null || !isManagedAssetOpen( resource ) || !resource.isSafeToSave() ) return false;

		if( !resource.getScheme().canSave( resource ) ) return false;

		resource.save( this );
		identifiedResources.put( resource.getUri(), resource );

		// TODO If the asset is changing URI the settings need to be moved

		// TODO Update the asset type.

		getEventBus().dispatch( new ResourceEvent( this, ResourceEvent.SAVED, resource ) );
		log.atInfo().log( "Saved: %s", resource );

		updateActionState();
		return true;
	}

	private boolean doCloseAsset( Resource resource ) throws ResourceException {
		if( resource == null ) return false;
		if( !isManagedAssetOpen( resource ) ) return false;

		// Close the asset
		resource.close( this );

		// Unregister the general asset listener
		resource.unregister( ResourceEvent.ANY, generalResourceWatcher );

		// Remove the asset from the list of open assets
		openResources.remove( resource );
		identifiedResources.remove( resource.getUri() );

		if( openResources.isEmpty() ) doSetCurrentAsset( null );

		// TODO Delete the asset settings?
		// Should the settings be removed? Or left for later?
		// Recommended not to delete the asset settings.
		// Maybe have a settings cleanup task and/or user actions

		getEventBus().dispatch( new ResourceEvent( this, ResourceEvent.CLOSED, resource ) );
		log.atDebug().log( "Asset closed: %s", resource );

		updateActionState();
		return true;
	}

	private boolean doDeleteAsset( Resource resource ) throws ResourceException {
		if( resource == null ) return false;
		if( resource.isOpen() ) doCloseAsset( resource );

		// Delete the asset
		resource.delete();

		getEventBus().dispatch( new ResourceEvent( this, ResourceEvent.DELETED, resource ) );
		log.atDebug().log( "Asset deleted: %s", resource );

		updateActionState();
		return true;
	}

	/**
	 * Save the asset, prompting the user if necessary.
	 *
	 * @param source The asset to be saved
	 * @param target The asset to save as
	 * @param saveAs The save as flag
	 * @param rename The rename flag
	 * @implNote This method makes calls to the FX platform.
	 */
	private void doSaveOrRenameResource( Resource source, Resource target, boolean saveAs, boolean rename ) {
		try {
			boolean needsTargetAsset = source.isNew() || ((saveAs || rename) && target == null);
			if( needsTargetAsset ) {
				askForTargetAsset( source, saveAs, rename );
			} else {
				saveAssets( source );
			}
		} catch( ResourceException exception ) {
			log.atSevere().withCause( exception ).log();
		}
	}

	private String generateFilename() {
		return "asset" + (currentResource == null ? "" : "." + currentResource.getCodec().getDefaultExtension());
	}

	private void askForTargetAsset( Resource source, boolean saveAs, boolean rename ) throws ResourceException {
		Codec codec = source.getCodec();
		if( codec == null ) codec = source.getType().getDefaultCodec();

		// Determine the asset path
		Path folder = source.isNew() ? getCurrentFileFolder() : Path.of( getParent( source ).getUri() );
		String filename = source.isNew() ? generateFilename() : source.getFileName();
		Path assetPath = folder.resolve( filename );

		// Build a URI to open the asset tool
		String uriString = ProgramResourceType.URI + "?mode=" + AssetTool.Mode.SAVE + "&uri=" + assetPath.toUri();
		log.atTrace().log( "save asset uri=%s", URI.create( uriString ) );

		final Resource finalResource = source;
		final Codec finalCodec = codec;
		program.getTaskManager().submit( Task.of( () -> {
			try {
				Map<Codec, ResourceFilter> filters = generateAssetFilters( finalResource.getType() );
				AssetTool tool = (AssetTool)openResource( URI.create( uriString ) ).get();
				tool.getFilters().addAll( 0, filters.values() );
				tool.setSelectedFilter( filters.get( finalCodec ) );
				tool.setSaveActionConsumer( target -> doAfterAssetTool( tool, filters, source, target, saveAs, rename ) );
			} catch( Exception exception ) {
				log.atWarn().withCause( exception ).log();
			}
		} ) );
	}

	private void doAfterAssetTool( AssetTool tool, Map<Codec, ResourceFilter> filters, Resource source, Resource target, boolean saveAs, boolean rename ) {
		try {
			Resource folder = target.isFolder() ? target : getParent( target );

			// Store the current folder in the settings
			setCurrentFileFolder( folder );

			// If the user specified a codec use it to set the codec and asset type
			Map<ResourceFilter, Codec> filterCodecs = MapUtil.mirror( filters );
			Codec selectedCodec = filterCodecs.get( tool.getSelectedFilter() );

			// If the extension is not already supported use the default extension from the codec
			if( !target.exists() && selectedCodec != null && !selectedCodec.isSupported( Codec.Pattern.EXTENSION, target.getFileName() ) ) {
				target = resolve( folder, target.getFileName() + "." + selectedCodec.getDefaultExtension() );
			}

			// Update the target asset
			if( selectedCodec != null ) target.setCodec( selectedCodec );

			if( source.isNew() || saveAs ) {
				doSaveAsAsset( source, target );
			} else if( rename ) {
				doRenameAsset( source, target );
			}
		} catch( ResourceException exception ) {
			log.atError( exception ).log();
		}
	}

	private void doSaveAsAsset( Resource source, Resource target ) throws ResourceException {
		if( source == null || target == null ) return;

		copySettings( source, target, false );

		// Use the scheme to save the source to the target
		target.getScheme().saveAs( source, target );

		if( source.isNew() ) closeResources( source );
		openResource( target.getUri() );
	}

	private void doRenameAsset( Resource source, Resource target ) throws ResourceException {
		if( source == null || target == null ) return;

		copySettings( source, target, true );

		// Use the scheme to rename the source to the target
		target.getScheme().rename( source, target );

		openResource( target.getUri() );
		closeResources( source );
	}

	private void copySettings( Resource source, Resource target, boolean delete ) {
		Settings sourceSettings = getProgram().getSettingsManager().getAssetSettings( source );
		Settings targetSettings = getProgram().getSettingsManager().getAssetSettings( target );
		targetSettings.copyFrom( sourceSettings );
		if( delete ) sourceSettings.delete();
	}

	private Map<Codec, ResourceFilter> generateAssetFilters( ResourceType type ) {
		Map<Codec, ResourceFilter> filters = new HashMap<>();
		type.getCodecs().forEach( c -> filters.put( c, new CodecResourceFilter( c ) ) );
		return filters;
	}

	URI uriCleanup( URI uri ) {
		return UriUtil.removeQueryAndFragment( uri ).normalize();
	}

	private boolean doSetCurrentAsset( Resource resource ) {
		synchronized( currentResourceLock ) {
			//log.log( Log.WARN,  "Current asset: " + currentAsset + " new asset: " + asset );
			Resource previous = currentResource;

			// "Disconnect" the old current asset
			if( currentResource != null ) {
				currentResource.getEventHub().dispatch( new ResourceEvent( this, ResourceEvent.DEACTIVATED, currentResource ) );
				currentResource.getEventHub().unregister( ResourceEvent.ANY, currentResourceWatcher );
			}

			// Change current asset
			currentResource = resource;

			// "Connect" the new current asset
			if( currentResource != null ) {
				currentResource.getEventHub().register( ResourceEvent.ANY, currentResourceWatcher );
				currentResource.getEventHub().dispatch( new ResourceEvent( this, ResourceEvent.ACTIVATED, currentResource ) );
			}

			// Notify program of current asset change
			getEventBus().dispatch( new ResourceSwitchedEvent( this, ResourceSwitchedEvent.SWITCHED, previous, currentResource ) );
			log.atFiner().log( "Asset select: %s", resource );
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
			// Create and configure the asset
			if( request.getResource() == null ) request.setResource( createAsset( request.getType(), request.getUri() ) );

			Resource resource = request.getResource();
			Object model = request.getModel();
			Codec codec = request.getCodec();
			if( model != null ) resource.setModel( model );
			if( codec != null ) resource.setCodec( codec );

			// Open the asset
			openAssetsAndWait( resource, 5, TimeUnit.SECONDS );
			//if( !isManagedAssetOpen( asset ) ) return null;

			// Create the tool if needed
			ProgramTool tool = null;
			try {
				// If the asset is "new", get user input from the asset type
				if( resource.isNew() ) {
					if( !resource.getType().callAssetNew( program, resource ) ) return null;
					log.atFiner().log( "Asset initialized with user values." );

					// The asset type may have changed the URI so resolve the scheme again
					resolveScheme( resource );
				}

				if( resource.getType() == null ) log.atError().log( "Asset type is null for: %s", resource );

				if( request.isOpenTool() ) tool = program.getToolManager().openTool( request );
			} catch( NoToolRegisteredException exception ) {
				log.atConfig().log( "No tool registered for: %s", resource );
				String title = Rb.text( "program", "no-tool-for-asset-title" );
				String message = Rb.text( "program", "no-tool-for-asset-message", resource.getUri().toString() );
				program.getNoticeManager().warning( title, message, resource.getName() );
				return null;
			}

			// Start loading the asset after the tool has been created
			if( !resource.isLoaded() ) loadAssets( resource );

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
			return canReloadAsset( getCurrentResource() );
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
			return canSaveAnyAssets( getModifiedResources() );
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
			return canRenameAsset( getCurrentResource() );
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

	private class OpenAssetTask extends AssetTask {

		private OpenAssetTask( Collection<Resource> resources ) {
			super( resources );
		}

		@Override
		public boolean doOperation( Resource resource ) throws ResourceException {
			return doOpenAsset( resource );
		}

	}

	private class LoadAssetTask extends AssetTask {

		private LoadAssetTask( Collection<Resource> resources ) {
			super( resources );
		}

		@Override
		public boolean doOperation( Resource resource ) throws ResourceException {
			return doLoadAsset( resource );
		}

	}

	private class ReloadAssetTask extends AssetTask {

		private ReloadAssetTask( Collection<Resource> resources ) {
			super( resources );
		}

		@Override
		public boolean doOperation( Resource resource ) throws ResourceException {
			return doReloadAsset( resource );
		}

	}

	private class SaveAssetTask extends AssetTask {

		private SaveAssetTask( Collection<Resource> resources ) {
			super( resources );
		}

		@Override
		public boolean doOperation( Resource resource ) throws ResourceException {
			return doSaveAsset( resource );
		}

	}

	private class CloseAssetTask extends AssetTask {

		private CloseAssetTask( Collection<Resource> resources ) {
			super( resources );
		}

		@Override
		public boolean doOperation( Resource resource ) throws ResourceException {
			return doCloseAsset( resource );
		}

	}

	private class DeleteAssetTask extends AssetTask {

		private DeleteAssetTask( Collection<Resource> resources ) {
			super( resources );
		}

		@Override
		public boolean doOperation( Resource resource ) throws ResourceException {
			return doDeleteAsset( resource );
		}

	}

	private class SetCurrentResourceTask extends AssetTask {

		private SetCurrentResourceTask( Resource resource ) {
			// A null collection will call the operation with a null value
			super( resource == null ? null : Set.of( resource ) );
		}

		@Override
		public boolean doOperation( Resource resource ) {
			return doSetCurrentAsset( resource );
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
