package com.acromere.xenon;

import com.acromere.log.LazyEval;
import com.acromere.log.LogLevel;
import com.acromere.product.Rb;
import com.acromere.settings.Settings;
import com.acromere.xenon.notice.Notice;
import com.acromere.xenon.resource.OpenAssetRequest;
import com.acromere.xenon.resource.Resource;
import com.acromere.xenon.resource.ResourceType;
import com.acromere.xenon.resource.exception.AssetTypeNotFoundException;
import com.acromere.xenon.resource.exception.ResourceException;
import com.acromere.xenon.resource.exception.ResourceNotFoundException;
import com.acromere.xenon.scheme.XenonScheme;
import com.acromere.xenon.workpane.*;
import com.acromere.xenon.workspace.Workarea;
import com.acromere.xenon.workspace.Workspace;
import com.acromere.zerra.javafx.Fx;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import lombok.CustomLog;
import lombok.Getter;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@CustomLog
class UiReader {

	private static final LogLevel logLevel = LogLevel.DEBUG;

	@Getter
	private final Xenon program;

	private final UiWorkspaceFactory spaceFactory;

	private final UiWorkareaFactory areaFactory;

	private final Map<String, Workspace> spaces = new HashMap<>();

	private final Map<String, Workarea> areas = new HashMap<>();

	private final Map<String, WorkpaneEdge> edges = new HashMap<>();

	private final Map<String, WorkpaneView> views = new HashMap<>();

	private final Map<String, Tool> tools = new HashMap<>();

	private final Set<Resource> resources = new HashSet<>();

	private Workspace activeSpace;

	private final Set<Workspace> maximizedSpaces = new HashSet<>();

	private final Map<Workspace, Workarea> spaceActiveAreas = new HashMap<>();

	private final Map<Workarea, WorkpaneView> areaActiveViews = new HashMap<>();

	private final Map<Workarea, WorkpaneView> areaDefaultViews = new HashMap<>();

	private final Map<Workarea, WorkpaneView> areaMaximizedViews = new HashMap<>();

	private final Map<WorkpaneView, Tool> viewActiveTools = new HashMap<>();

	private final List<Exception> errors = new ArrayList<>();

	private final Lock spaceRestoreLock = new ReentrantLock();

	private final Condition spacesRestoredCondition = spaceRestoreLock.newCondition();

	private boolean spacesRestored;

	private Future<Collection<Resource>> assetLoadFuture;

	public UiReader( Xenon program ) {
		this.program = program;
		this.spaceFactory = new UiWorkspaceFactory( program );
		this.areaFactory = new UiWorkareaFactory( program );
	}

	public void loadWorkspaces() {
		Fx.run( this::doWorkspaceLoad );
	}

	public void awaitLoadWorkspaces( long duration, TimeUnit unit ) throws InterruptedException, TimeoutException {
		doAwaitForSpaceLoad( duration, unit );
	}

	public void loadAssets() {
		doStartAssetLoading();
	}

	private void doWorkspaceLoad() {
		Fx.affirmOnFxThread();
		spaceRestoreLock.lock();

		try {
			if( getWorkspaceCount() == 0 ) {
				Workspace space = spaceFactory.createDefaultWorkspace( areaFactory );
				spaces.put( space.getUid(), space );
				log.at( logLevel ).log( "Created default workspace" );
			} else {
				restoreWorkspaces();
				log.at( logLevel ).log( "Restored known workspaces: count=%s", spaces.size() );
			}

			// Check the restored state
			Workspace space = getProgram().getWorkspaceManager().getActiveWorkspace();
			if( getProgram().getWorkspaceManager().getWorkspaces().isEmpty() ) log.atError().log( "No workspaces restored" );
			if( space == null ) errors.add( new UiException( "Missing active workspace" ) );
			if( space != null && space.getWorkareas().isEmpty() ) errors.add( new UiException( "No workareas restored" ) );
			if( space != null && space.getActiveWorkarea() == null ) errors.add( new UiException( "Missing active workarea" ) );
			for( Workarea area : areas.values() ) {
				if( area.getUid() == null ) errors.add( new UiException( "Missing id for workarea: " + area ) );
				if( area.getActiveView() == null ) errors.add( new UiException( "Missing active view for workarea: " + area ) );
				if( area.getDefaultView() == null ) errors.add( new UiException( "Missing default view for workarea: " + area ) );
			}

			// If there are exceptions restoring the UI, notify the user
			if( !errors.isEmpty() ) notifyUserOfErrors( errors );
		} finally {
			spacesRestored = true;
			spacesRestoredCondition.signalAll();
			spaceRestoreLock.unlock();
		}
	}

	private int getWorkspaceCount() {
		return getProgram().getSettingsManager().getSettings( ProgramSettings.SPACE ).getNodes().size();
	}

	private void restoreWorkspaces() {
		getUiSettings( ProgramSettings.SPACE ).forEach( this::loadSpaceForLinking );
		getUiSettings( ProgramSettings.AREA ).forEach( this::loadAreaForLinking );
		getUiSettings( ProgramSettings.VIEW ).forEach( this::loadViewForLinking );
		getUiSettings( ProgramSettings.EDGE ).forEach( this::loadEdgeForLinking );
		getUiSettings( ProgramSettings.TOOL ).forEach( this::loadToolForLinking );

		// Reassemble the UI
		linkSpaces();
		linkAreasToSpaces();
		linkEdgesAndViewsToAreas();
		linkToolsToViews();

		// Now that everything is linked, time to restore the flags. This should be
		// done before the listeners are added to avoid unintended modifications.
		restoreFlags();

		// Last, but not least, bind the settings. This should be done last to
		// avoid unintended modifications while the UI is being restored.
		bindSettings();
	}

	private void restoreFlags() {
		/* TOOLS */
		// For each view there is an active tool
		for( WorkpaneView view : viewActiveTools.keySet() ) {
			Tool tool = viewActiveTools.get( view );
			if( tool instanceof ProgramTool programTool ) {
				programTool.setActiveWhenReady();
			} else {
				view.setActiveTool( tool );
			}
		}

		/* WORKAREAS */
		// For each area there is an active view
		for( Workarea area : areaActiveViews.keySet() ) {
			WorkpaneView view = areaActiveViews.get( area );
			area.setActiveView( view );
		}
		// For each area there is a default view
		for( Workarea area : areaDefaultViews.keySet() ) {
			WorkpaneView view = areaDefaultViews.get( area );
			area.setDefaultView( view );
		}
		// For each area there might be a maximized view
		for( Workarea area : areaMaximizedViews.keySet() ) {
			WorkpaneView view = areaMaximizedViews.get( area );
			area.setMaximizedView( view );
		}

		/* WORKSPACES */
		// For each space there is an active area
		for( Workspace space : spaceActiveAreas.keySet() ) {
			Workarea area = spaceActiveAreas.get( space );
			space.setActiveWorkarea( area );
		}
		// For each space there might be a maximized area
		for( Workspace space : maximizedSpaces ) {
			space.setMaximized( true );
		}
		// Set the active space
		if( activeSpace != null ) {
			program.getWorkspaceManager().setActiveWorkspace( activeSpace );
		}
	}

	private void bindSettings() {
		// Register the workarea listeners
		for( Workarea area : areas.values() ) {
			Settings settings = program.getSettingsManager().getSettings( ProgramSettings.AREA, area.getUid() );
			areaFactory.bindSettings( area, settings );
		}

		// Register the workspace listeners
		for( Workspace space : spaces.values() ) {
			Settings settings = program.getSettingsManager().getSettings( ProgramSettings.SPACE, space.getUid() );
			spaceFactory.bindSettings( space, settings );
		}
	}

	private void doStartAssetLoading() {
		try {
			assetLoadFuture = getProgram().getResourceManager().loadAssets( resources );
		} catch( Exception exception ) {
			log.atWarn( exception ).log();
		}
	}

	Workspace loadSpaceForLinking( Settings settings ) {
		try {
			Workspace workspace = loadSpace( settings );
			spaces.put( workspace.getUid(), workspace );
			return workspace;
		} catch( Exception exception ) {
			log.atError( exception ).log( "Error restoring workspace" );
			return null;
		}
	}

	Workspace loadSpace( Settings settings ) {
		Workspace workspace = spaceFactory.createWorkspace();
		workspace.setUid( settings.getName() );
		spaceFactory.restoreWorkspaceFromSettings( workspace, settings );
		if( isActive( settings ) ) activeSpace = workspace;
		if( isMaximized( settings ) ) maximizedSpaces.add( workspace );
		return workspace;
	}

	Workarea loadAreaForLinking( Settings settings ) {
		try {
			String id = settings.getName();
			Workspace space = spaces.get( settings.get( Ui.PARENT_SPACE_ID ) );

			// If the workspace is not found, then the workarea is orphaned...delete the settings
			if( space == null ) {
				settings.delete();
				throw new UiException( "Removed orphaned area id=" + id );
			}

			Workarea area = loadArea( settings );
			if( isActive( settings ) ) spaceActiveAreas.put( space, area );

			areas.put( id, area );
			return area;
		} catch( Exception exception ) {
			errors.add( exception );
			return null;
		}
	}

	Workarea loadArea( Settings settings ) {
		Workarea area = new Workarea();
		areaFactory.restoreWorkareaFromSettings( area, settings );
		return area;
	}

	WorkpaneView loadViewForLinking( Settings settings ) {
		try {
			String id = settings.getName();
			Workarea area = areas.get( settings.get( Ui.PARENT_AREA_ID ) );

			// If the workpane is not found, then the view is orphaned...delete the settings
			if( area == null ) {
				settings.delete();
				throw new UiException( "Removed orphaned view id=" + id );
			}

			WorkpaneView view = loadView( settings );
			views.put( id, view );
			return view;
		} catch( Exception exception ) {
			errors.add( exception );
			return null;
		}
	}

	WorkpaneView loadView( Settings settings ) {
		WorkpaneView view = new WorkpaneView();
		view.setUid( settings.getName() );
		if( settings.exists( Ui.PLACEMENT ) ) view.setPlacement( settings.get( Ui.PLACEMENT, Workpane.Placement.class ) );
		return view;
	}

	WorkpaneEdge loadEdgeForLinking( Settings settings ) {
		try {
			String id = settings.getName();
			Workarea area = areas.get( settings.get( Ui.PARENT_AREA_ID ) );

			// If the workpane is not found, then the edge is orphaned...delete the settings
			if( area == null ) {
				settings.delete();
				throw new UiException( "Removed orphaned edge id=" + id );
			}

			WorkpaneEdge edge = loadEdge( settings );
			edges.put( id, edge );
			return edge;
		} catch( Exception exception ) {
			errors.add( exception );
			return null;
		}
	}

	WorkpaneEdge loadEdge( Settings settings ) {
		WorkpaneEdge edge = new WorkpaneEdge();
		edge.setUid( settings.getName() );
		if( settings.exists( Ui.ORIENTATION ) ) edge.setOrientation( Orientation.valueOf( settings.get( Ui.ORIENTATION ).toUpperCase() ) );
		if( settings.exists( Ui.POSITION ) ) edge.setPosition( settings.get( Ui.POSITION, Double.class ) );
		return edge;
	}

	Tool loadToolForLinking( Settings settings ) {
		try {
			String id = settings.getName();
			URI uri = settings.get( Resource.SETTINGS_URI_KEY, URI.class );
			WorkpaneView view = views.get( settings.get( Ui.PARENT_VIEW_ID ) );

			// If the view is not found, then the tool is orphaned...delete the settings
			if( view == null || uri == null ) {
				settings.delete();
				throw new UiException( "Removed orphaned tool id=" + id );
			}

			Tool tool = loadTool( settings );
			if( isActive( settings ) ) viewActiveTools.put( view, tool );
			resources.add( tool.getResource() );
			tools.put( id, tool );
			return tool;
		} catch( Exception exception ) {
			errors.add( exception );
			return null;
		}
	}

	private String mapResourceType( String resourceTypeKey ) {
		if( resourceTypeKey == null ) return null;

		// Swap com.avereon for com.acromere
		if( resourceTypeKey.startsWith( "com.avereon." ) ) {
			resourceTypeKey = resourceTypeKey.replaceFirst( "com.avereon", "com.acromere" );
		}

		// Swap AssetType for ResourceType
		if( resourceTypeKey.startsWith( "com.acromere." ) ) {
			// com.acromere.cartesia.Design2dAssetType
			int startIndex = resourceTypeKey.lastIndexOf( "AssetType" );
			if( startIndex >= 0 ) {
				int endIndex = startIndex + 9;
				String prefix = resourceTypeKey.substring( 0, startIndex );
				String suffix = resourceTypeKey.substring( endIndex );
				resourceTypeKey = prefix + "ResourceType" + suffix;
			}

			if( "program:/guide".equals( resourceTypeKey ) ) resourceTypeKey = XenonScheme.ID + ":/guide";
		}

		return resourceTypeKey;
	}

	ProgramTool loadTool( Settings settings ) throws ResourceException, ToolInstantiationException {
		String toolClassName = settings.get( Tool.SETTINGS_TYPE_KEY );
		URI uri = settings.get( Resource.SETTINGS_URI_KEY, URI.class );
		String assetTypeKey = settings.get( Resource.SETTINGS_TYPE_KEY );
		Integer order = settings.get( Tool.ORDER, Integer.class, -1 );

		assetTypeKey = mapResourceType( assetTypeKey );

		// Create the asset
		Resource resource;
		ResourceType resourceType = getProgram().getResourceManager().getAssetType( assetTypeKey );
		if( resourceType == null ) throw new AssetTypeNotFoundException( assetTypeKey );
		try {
			resource = getProgram().getResourceManager().createAsset( resourceType, uri );
		} catch( ResourceException exception ) {
			throw new ResourceNotFoundException( new Resource( resourceType, uri ), exception );
		}

		// Create the open asset request
		OpenAssetRequest openAssetRequest = new OpenAssetRequest();
		openAssetRequest.setToolId( settings.getName() );
		openAssetRequest.setResource( resource );
		openAssetRequest.setToolClassName( toolClassName );

		// Restore the tool
		ProgramTool tool = getProgram().getToolManager().restoreTool( openAssetRequest );
		if( tool == null ) {
			settings.delete();
			throw new ToolInstantiationException( settings.getName(), toolClassName );
		}

		tool.setOrder( order );

		return tool;
	}

	void linkAreasToSpaces() {
		// Sort the areas by order
		List<Workarea> areaList = new ArrayList<>( areas.values() );
		areaList.sort( Comparator.comparing( Workarea::getOrder ) );

		// Link the workareas to the workspaces
		for( Workarea area : areaList ) {
			try {
				Settings settings = getProgram().getSettingsManager().getSettings( ProgramSettings.AREA, area.getUid() );
				Workspace space = spaces.get( settings.get( Ui.PARENT_SPACE_ID ) );

				space.addWorkarea( area );

				// Save the active area for later
				if( area.isActive() ) spaceActiveAreas.put( space, area );

				if( hasActiveView( settings ) ) areaActiveViews.put( area, views.get( settings.get( Ui.VIEW_ACTIVE ) ) );
				if( hasDefaultView( settings ) ) areaDefaultViews.put( area, views.get( settings.get( Ui.VIEW_DEFAULT ) ) );
				if( hasMaximizedView( settings ) ) areaMaximizedViews.put( area, views.get( settings.get( Ui.VIEW_MAXIMIZED ) ) );
			} catch( Exception exception ) {
				errors.add( exception );
			}
		}
	}

	void linkEdgesAndViewsToAreas() {
		Map<Workpane, Set<WorkpaneEdge>> areaEdges = new HashMap<>();
		Map<Workpane, Set<WorkpaneView>> areaViews = new HashMap<>();

		// Link the edges
		for( WorkpaneEdge edge : edges.values() ) {
			Settings settings = getProgram().getSettingsManager().getSettings( ProgramSettings.EDGE, edge.getUid() );
			Workarea area = areas.get( settings.get( Ui.PARENT_AREA_ID ) );

			try {
				if( linkEdge( area, edge, settings ) ) {
					areaEdges.computeIfAbsent( area, _ -> new HashSet<>() ).add( edge );
				} else {
					log.atDebug().log( "Removing invalid workpane edge settings: %s", LazyEval.of( settings::getName ) );
					settings.delete();
				}
			} catch( Exception exception ) {
				log.atWarn( exception ).log( "Error linking edge: %s", LazyEval.of( edge::getUid ) );
			}
		}

		// Link the views
		for( WorkpaneView view : views.values() ) {
			Settings settings = getProgram().getSettingsManager().getSettings( ProgramSettings.VIEW, view.getUid() );
			Workarea area = areas.get( settings.get( Ui.PARENT_AREA_ID ) );

			try {
				if( linkView( area, view, settings ) ) {
					areaViews.computeIfAbsent( area, _ -> new HashSet<>() ).add( view );
				} else {
					log.atDebug().log( "Removing invalid workpane view settings: %s", LazyEval.of( settings::getName ) );
					settings.delete();
				}
			} catch( Exception exception ) {
				log.atWarn( exception ).log( "Error linking view: %s", LazyEval.of( view::getUid ), exception );
			}
		}

		// Restore edges and views to workpane
		for( Workarea area : areas.values() ) {
			Set<WorkpaneEdge> localAreaEdges = areaEdges.computeIfAbsent( area, _ -> new HashSet<>() );
			Set<WorkpaneView> localAreaViews = areaViews.computeIfAbsent( area, _ -> new HashSet<>() );
			linkArea( area, localAreaEdges, localAreaViews );
		}
	}

	boolean linkEdge( Workarea area, WorkpaneEdge edge, Settings settings ) {
		Orientation orientation = Objects.requireNonNull( edge.getOrientation() );

		if( orientation == Orientation.VERTICAL ) {
			edge.setEdge( Side.TOP, lookupEdge( area, settings.get( Ui.T, Ui.T ) ) );
			edge.setEdge( Side.BOTTOM, lookupEdge( area, settings.get( Ui.B, Ui.B ) ) );
		} else if( orientation == Orientation.HORIZONTAL ) {
			edge.setEdge( Side.LEFT, lookupEdge( area, settings.get( Ui.L, Ui.L ) ) );
			edge.setEdge( Side.RIGHT, lookupEdge( area, settings.get( Ui.R, Ui.R ) ) );
		}

		return true;
	}

	boolean linkView( Workarea area, WorkpaneView view, Settings settings ) {
		view.setEdge( Side.TOP, lookupEdge( area, settings.get( Ui.T, Ui.T ) ) );
		view.setEdge( Side.LEFT, lookupEdge( area, settings.get( Ui.L, Ui.L ) ) );
		view.setEdge( Side.RIGHT, lookupEdge( area, settings.get( Ui.R, Ui.R ) ) );
		view.setEdge( Side.BOTTOM, lookupEdge( area, settings.get( Ui.B, Ui.B ) ) );
		return true;
	}

	void linkArea( Workarea area, Set<WorkpaneEdge> edges, Set<WorkpaneView> views ) {
		area.restoreNodes( edges, views );
	}

	void linkToolsToViews() {
		try {
			// Map out all the tools to their respective views
			Map<WorkpaneView, Set<Tool>> viewToolMap = new HashMap<>();
			for( Tool tool : tools.values() ) {
				Settings settings = getProgram().getSettingsManager().getSettings( ProgramSettings.TOOL, tool.getUid() );
				WorkpaneView view = views.get( settings.get( Ui.PARENT_VIEW_ID ) );
				viewToolMap.computeIfAbsent( view, _ -> new HashSet<>() ).add( tool );
			}

			// Now go through the views and link the tools
			for( WorkpaneView view : viewToolMap.keySet() ) {
				Workarea area = (Workarea)view.getWorkpane();

				// Get the tools for the view and order them
				List<Tool> toolList = new ArrayList<>( viewToolMap.get( view ) );
				toolList.sort( Comparator.comparing( Tool::getOrder ) );

				toolList.forEach( tool -> {
					try {
						area.addTool( tool, view, false );
						log.atDebug().log( "Tool linked: %s: %s", LazyEval.of( tool::getClass ), LazyEval.of( () -> tool.getResource().getUri() ) );
					} catch( Exception exception ) {
						errors.add( exception );
					}
				} );
			}
		} catch( Exception exception ) {
			errors.add( exception );
		}
	}

	void linkSpaces() {
		List<Workspace> spacesList = new ArrayList<>( spaces.values() );
		spacesList.sort( Comparator.comparing( Workspace::getOrder ) );

		for( Workspace workspace : spacesList ) {
			getProgram().getWorkspaceManager().addWorkspace( workspace );
		}
	}

	private List<String> getUiSettingsIds( String path ) {
		return getProgram().getSettingsManager().getSettings( path ).getNodes();
	}

	private List<Settings> getUiSettings( String path ) {
		return getUiSettingsIds( path ).stream().map( id -> getProgram().getSettingsManager().getSettings( path, id ) ).toList();
	}

	private boolean isActive( Settings settings ) {
		return settings.get( Ui.ACTIVE, Boolean.class, false );
	}

	private boolean isMaximized( Settings settings ) {
		return settings.get( Ui.MAXIMIZED, Boolean.class, false );
	}

	private boolean hasActiveView( Settings settings ) {
		return settings.exists( Ui.VIEW_ACTIVE );
	}

	private boolean hasDefaultView( Settings settings ) {
		return settings.exists( Ui.VIEW_DEFAULT );
	}

	private boolean hasMaximizedView( Settings settings ) {
		return settings.exists( Ui.VIEW_MAXIMIZED );
	}

	private WorkpaneEdge lookupEdge( Workarea area, String id ) {
		if( area == null ) throw new NullPointerException( "Workarea cannot be null" );
		if( id == null ) throw new NullPointerException( "Edge id cannot be null" );

		WorkpaneEdge edge = edges.get( id );
		if( edge == null ) edge = area.getWallEdge( id.charAt( 0 ) );

		return edge;
	}

	private void doAwaitForSpaceLoad( long duration, TimeUnit unit ) throws InterruptedException, TimeoutException {
		spaceRestoreLock.lock();
		try {
			while( !spacesRestored ) {
				if( !spacesRestoredCondition.await( duration, unit ) ) throw new TimeoutException( "Timeout waiting for workspace restore" );
			}
		} finally {
			spaceRestoreLock.unlock();
		}
	}

	private void doAwaitForAssetLoad( long duration, TimeUnit unit ) throws InterruptedException, TimeoutException {
		try {
			assetLoadFuture.get( duration, unit );
		} catch( ExecutionException exception ) {
			log.atWarn( exception ).log();
		}
	}

	private void notifyUserOfErrors( List<Exception> exceptions ) {
		Set<String> messages = new HashSet<>();
		for( Exception exception : exceptions ) {
			log.atWarn( exception ).log();

			if( exception instanceof ToolInstantiationException toolException ) {
				messages.add( Rb.text( RbKey.PROGRAM, "tool-missing", toolException.getToolClass() ) );
			} else if( exception instanceof ResourceNotFoundException assetException ) {
				messages.add( Rb.text( RbKey.PROGRAM, "asset-missing", assetException.getAsset().getUri() ) );
			} else {
				messages.add( exception.getMessage() );
			}
		}

		StringBuilder builder = new StringBuilder();
		for( String message : messages ) builder.append( "\n" ).append( message );

		Notice notice = new Notice( Rb.text( RbKey.PROGRAM, "ui-restore-error-title" ) );
		notice.setMessage( builder.toString().trim() );
		getProgram().getNoticeManager().addNotice( notice );
	}

}
