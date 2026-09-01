package com.acromere.xenon.tool;

import com.acromere.log.LogLevel;
import com.acromere.product.Rb;
import com.acromere.util.FileUtil;
import com.acromere.util.OperatingSystem;
import com.acromere.util.UriUtil;
import com.acromere.xenon.*;
import com.acromere.xenon.resource.*;
import com.acromere.xenon.resource.exception.ResourceException;
import com.acromere.xenon.scheme.FileScheme;
import com.acromere.xenon.task.Task;
import com.acromere.xenon.tool.guide.Guide;
import com.acromere.xenon.tool.guide.GuideNode;
import com.acromere.xenon.tool.guide.GuidedTool;
import com.acromere.xenon.workpane.ToolException;
import com.acromere.zerra.javafx.Fx;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;

import javax.swing.filechooser.FileSystemView;
import java.awt.event.KeyEvent;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@CustomLog
public class ResourceTool extends GuidedTool {

	public enum Mode {
		OPEN,
		SAVE
	}

	private final Callback<ResourceWatchEvent, Void> eventCallback;

	private final TextField uriField;

	private final Button goButton;

	private final HBox userNotice;

	private final Label userMessage;

	private final ComboBox<ResourceFilter> filters;

	private final TableColumn<Resource, Node> iconColumn;

	private final TableColumn<Resource, Label> nameColumn;

	private final TableColumn<Resource, String> uriColumn;

	private final TableColumn<Resource, Node> sizeColumn;

	private final TableView<Resource> resourceTable;

	private final ObservableList<Resource> resources;

	private final FilteredList<Resource> filtered;

	private final SortedList<Resource> sorted;

	private final RefreshAction refreshAction;

	private final ProgramAction priorAction;

	private final ProgramAction nextAction;

	private final ProgramAction parentAction;

	private final NewFolderAction newFolderAction;

	private final DeleteAction deleteAction;

	private final LinkedList<String> history;

	private Mode mode = Mode.OPEN;

	private Resource parentResource;

	@Getter
	private Resource currentFolder;

	@Getter
	private String currentFilename;

	private int currentIndex;

	@Setter
	private Consumer<Resource> saveActionConsumer;

	public ResourceTool( XenonProgramProduct product, Resource resource ) {
		super( product, resource );
		setId( "tool-resource" );

		this.eventCallback = this::handleExternalResourceEvent;

		// URI input bar
		uriField = new TextField();
		HBox.setHgrow( uriField, Priority.ALWAYS );
		goButton = new Button();

		// Resource filters
		filters = new ComboBox<>();
		ResourceFilter anyResourceFilter = new AnyResourceFilter();
		filters.getItems().add( anyResourceFilter );
		filters.getSelectionModel().select( anyResourceFilter );

		// User notice bar
		userMessage = new Label( "user message" );
		userMessage.setMaxWidth( Double.MAX_VALUE );
		userMessage.setMaxHeight( Double.MAX_VALUE );
		userMessage.setPadding( new Insets( 0, Ui.PAD, 0, Ui.PAD ) );
		HBox.setHgrow( userMessage, Priority.ALWAYS );
		Label closeLabel = new Label( "", getProgram().getIconLibrary().getIcon( "workarea-close" ) );
		closeLabel.setPadding( new Insets( Ui.PAD ) );
		userNotice = new HBox( Ui.PAD, userMessage, closeLabel );
		userNotice.setId( "user-notice" );
		userNotice.setAlignment( Pos.CENTER );
		closeLabel.setOnMousePressed( e -> closeUserNotice() );

		String nameColumnHeader = Rb.text( RbKey.LABEL, "name", "Name" );
		String uriColumnHeader = Rb.text( RbKey.LABEL, "uri", "URI" );
		String sizeColumnHeader = Rb.text( RbKey.LABEL, "size", "Size" );

		// The resource collections
		resources = FXCollections.observableArrayList();
		filtered = new FilteredList<>( resources, i -> true );
		sorted = new SortedList<>( filtered );

		// Table columns -----------------------------------------------------------
		Node icon = getProgram().getIconLibrary().getIcon( "resource" );
		double columnWidth = icon.getBoundsInLocal().getWidth() + 8;

		iconColumn = new TableColumn<>( "" );
		iconColumn.setCellValueFactory( new IconValueFactory( getProgram() ) );
		iconColumn.setSortable( false );
		iconColumn.setResizable( false );
		iconColumn.setMaxWidth( columnWidth );
		iconColumn.setMinWidth( iconColumn.getMaxWidth() );

		nameColumn = new TableColumn<>( nameColumnHeader );
		nameColumn.setCellValueFactory( new NameValueFactory() );
		nameColumn.setComparator( new ResourceLabelComparator() );
		nameColumn.setSortType( TableColumn.SortType.ASCENDING );
		// TODO Make custom TextFieldTableCell to avoid the double-click to edit
		nameColumn.setCellFactory( TextFieldTableCell.forTableColumn( new StringLabelConverter() ) );
		nameColumn.onEditCommitProperty().set( this::doUpdateResourceName );

		uriColumn = new TableColumn<>( uriColumnHeader );
		uriColumn.setCellValueFactory( new PropertyValueFactory<>( "uri" ) );

		sizeColumn = new TableColumn<>( sizeColumnHeader );
		sizeColumn.setCellValueFactory( new SizeValueFactory() );
		sizeColumn.setComparator( new ResourceSizeComparator() );
		sizeColumn.setStyle( "-fx-alignment: CENTER-RIGHT;" );

		// Resource table -------------------------------------------------------------
		resourceTable = new TableView<>( sorted );
		resourceTable.setEditable( true );
		resourceTable.getColumns().add( iconColumn );
		resourceTable.getColumns().add( nameColumn );
		resourceTable.getColumns().add( uriColumn );
		resourceTable.getColumns().add( sizeColumn );
		resourceTable.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN );
		VBox.setVgrow( resourceTable, Priority.ALWAYS );

		sorted.comparatorProperty().bind( resourceTable.comparatorProperty() );
		filtered.predicateProperty().bind( filters.getSelectionModel().selectedItemProperty() );

		// Tool layout
		VBox layout = new VBox( Ui.PAD );
		layout.setPadding( new Insets( Ui.PAD ) );
		layout.getChildren().add( new HBox( Ui.PAD, uriField, goButton ) );
		layout.getChildren().add( filters );
		layout.getChildren().add( userNotice );
		layout.getChildren().add( resourceTable );
		getChildren().add( layout );

		// Actions
		refreshAction = new RefreshAction( getProgram() );
		priorAction = new PriorAction( getProgram() );
		nextAction = new NextAction( getProgram() );
		parentAction = new ParentAction( getProgram() );
		newFolderAction = new NewFolderAction( getProgram() );
		deleteAction = new DeleteAction( getProgram() );

		history = new LinkedList<>();
		currentIndex = -1;

		// Basic behavior
		uriField.setOnKeyPressed( e -> {
			if( e.getCode().getCode() == KeyEvent.VK_ESCAPE && userNotice.isVisible() ) closeUserNotice();
		} );
		uriField.setOnAction( e -> selectResource( uriField.getText() ) );
		goButton.setOnAction( this::doGoAction );
		resourceTable.setOnMousePressed( this::doMousePressed );
		resourceTable.getSelectionModel().selectedItemProperty().addListener( ( p, o, n ) -> updateActionState() );

		Guide guide = createGuide();
		getGuideContext().getGuides().add( guide );
		getGuideContext().setCurrentGuide( guide );

		closeUserNotice();
	}

	public ObservableList<ResourceFilter> getFilters() {
		return filters.getItems();
	}

	public ResourceFilter getSelectedFilter() {
		return filters.getSelectionModel().getSelectedItem();
	}

	public void setSelectedFilter( ResourceFilter filter ) {
		Fx.run( () -> this.filters.getSelectionModel().select( filter ) );
	}

	public ObservableValue<ResourceFilter> selectedFilter() {
		return filters.getSelectionModel().selectedItemProperty();
	}

	@Override
	protected void ready( OpenResourceRequest request ) {
		// TODO Put the columns in the preferred order
		resourceTable.getSortOrder().clear();
		resourceTable.getSortOrder().add( nameColumn );

		// TODO Set the columns to the preferred size
	}

	@Override
	protected void open( OpenResourceRequest request ) {
		// Update the mode
		mode = resolveMode( request.getUri() );

		// Set the title depending on the mode requested
		String action = mode.name().toLowerCase();
		setTitle( Rb.text( "action", action + ".name" ) );
		setGraphic( getProgram().getIconLibrary().getIcon( "resource-" + action ) );
		goButton.setGraphic( getProgram().getIconLibrary().getIcon( "resource-" + action ) );

		// Determine the current folder
		Path currentFolder = getProgram().getResourceManager().getCurrentFileFolder();

		// Select the current resource
		URI uri;
		try {
			uri = resolveUri( request.getQueryParameters() );
			if( uri != null && !uri.isAbsolute() ) uri = currentFolder.resolve( uri.getPath() ).toUri();
			if( uri == null ) uri = currentFolder.toUri();
			selectResource( uri );
		} catch( URISyntaxException exception ) {
			log.atWarn( exception ).log();
		}

		if( mode == Mode.OPEN ) addSupportedFilters();
	}

	@Override
	protected void activate() throws ToolException {
		super.activate();

		pushAction( "refresh", refreshAction );
		pushAction( "up", parentAction );
		pushAction( "prior", priorAction );
		pushAction( "next", nextAction );
		pushAction( "new-folder", newFolderAction );
		pushAction( "delete", deleteAction );

		pushTools( "refresh | prior next up | new-folder | delete" );
	}

	@Override
	protected void conceal() throws ToolException {
		super.conceal();

		pullTools();

		pullAction( "delete", deleteAction );
		pullAction( "new-folder", newFolderAction );
		pullAction( "up", parentAction );
		pullAction( "next", nextAction );
		pullAction( "prior", priorAction );
		pullAction( "refresh", refreshAction );
	}

	@Override
	protected void guideNodesSelected( Set<GuideNode> oldNodes, Set<GuideNode> newNodes ) {
		if( newNodes.isEmpty() ) return;
		selectResource( newNodes.stream().findAny().get().getId() );
	}

	private static Mode resolveMode( URI uri ) {
		if( uri == null ) return Mode.OPEN;

		Map<String, String> query = UriUtil.parseQuery( UriUtil.decode( uri.getQuery() ) );
		String parameter = query.get( "mode" );
		if( parameter == null ) return Mode.OPEN;

		try {
			return Mode.valueOf( parameter.toUpperCase() );
		} catch( IllegalArgumentException exception ) {
			return Mode.OPEN;
		}
	}

	@Deprecated
	private static Mode resolveMode( String fragment ) {
		if( fragment == null ) return Mode.OPEN;
		try {
			return Mode.valueOf( fragment.trim().toUpperCase() );
		} catch( IllegalArgumentException exception ) {
			return Mode.OPEN;
		}
	}

	private static URI resolveUri( Map<String, String> parameters ) throws URISyntaxException {
		if( parameters == null ) return null;
		String uriString = parameters.get( "uri" );
		return uriString == null ? null : URI.create( uriString.replace( " ", "%20" ) );
	}

	private void addSupportedFilters() {
		List<ResourceFilter> filters = getProgram()
			.getResourceManager()
			.getResourceTypes()
			.stream()
			.filter( ResourceType::isUserType )
			.flatMap( t -> t.getCodecs().stream() )
			.map( CodecResourceFilter::new )
			.sorted()
			.collect( Collectors.toList() );
		getFilters().addAll( 0, filters );
	}

	private void doMousePressed( MouseEvent event ) {
		if( event.isPrimaryButtonDown() ) {
			selectResourceFromTable( event );
		} else if( event.isSecondaryButtonDown() ) {
			editResourceFromTable( event );
		}
	}

	@SuppressWarnings( "unchecked" )
	private void selectResourceFromTable( MouseEvent event ) {
		int clickCount = Integer.parseInt( getSettings().get( "click-count", "2" ) );
		if( event.getClickCount() < clickCount ) return;

		TableView<Resource> table = (TableView<Resource>)event.getSource();
		Resource item = table.getSelectionModel().getSelectedItem();
		if( item == null ) return;

		try {
			if( mode == Mode.OPEN ) {
				selectResource( item.getUri() );
			} else if( mode == Mode.SAVE ) {
				if( item.isFolder() ) {
					selectResource( item.getUri().resolve( currentFilename ) );
				} else {
					selectResource( item.getUri() );
				}
			}
		} catch( ResourceException exception ) {
			handleResourceException( exception );
		}
	}

	@SuppressWarnings( "unchecked" )
	private void editResourceFromTable( MouseEvent event ) {
		TableView<Resource> table = (TableView<Resource>)event.getSource();
		Resource item = table.getSelectionModel().getSelectedItem();
		if( item == null ) return;

		editResourceName( item );
	}

	private void doGoAction( ActionEvent event ) {
		selectResource( uriField.getText() );
		try {
			if( mode == Mode.SAVE ) requestSaveResource();
		} catch( ResourceException exception ) {
			handleResourceException( exception );
		}
		event.consume();
	}

	private void selectResource( Resource resource ) {
		selectResource( resource.getUri() );
	}

	private void selectResource( URI uri ) {
		selectResource( uri.toString() );
	}

	private void selectResource( String path ) {
		selectResource( path, true );
	}

	private void selectResource( final String path, boolean updateHistory ) {
		Objects.requireNonNull( path );

		if( updateHistory ) {
			while( history.size() - 1 > currentIndex ) {
				history.pop();
			}
			history.add( path );
			currentIndex++;
		}

		try {
			Resource resource = getProgram().getResourceManager().createResource( path );

			uriField.setText( UriUtil.decode( path ) );

			if( mode == Mode.OPEN ) {
				if( !resource.exists() ) {
					notifyUser( "resource-not-found", path );
				} else {
					if( resource.isFolder() ) {
						loadFolder( resource );
					} else {
						// TODO If the user chose "open with" then allow the user to chose a tool
						//List<Class<? extends ProgramTool>> tools = getProgram().getToolManager().getRegisteredTools( resource.getType() );

						// Use the resource manager to open the resource
						getProgram().getResourceManager().openResource( resource.getUri() );

						// Close this tool
						close();
						return;
					}
				}
			} else if( mode == Mode.SAVE ) {
				if( resource.isFolder() ) {
					loadFolder( resource );
					uriField.setText( UriUtil.resolveToString( resource.getUri(), currentFilename ) );
				} else {
					currentFilename = resource.getFileName();
					loadFolder( getProgram().getResourceManager().getParent( resource ) );
				}
			}
			activateUriField();
		} catch( ResourceException exception ) {
			handleResourceException( exception );
		} finally {
			updateActionState();
		}
	}

	private void editResourceName( Resource resource ) {
		int index = resourceTable.getItems().indexOf( resource );
		log.at( LogLevel.DEBUG ).log( "Editing resource %s named: %s", index, resource.getName() );
		if( index >= 0 ) Fx.run( () -> resourceTable.edit( index, nameColumn ) );
	}

	private void requestSaveResource() throws ResourceException {
		Resource target = getProgram().getResourceManager().resolve( currentFolder, currentFilename );
		if( saveActionConsumer != null ) saveActionConsumer.accept( target );
		close();
	}

	private void updateActionState() {
		refreshAction.updateEnabled();
		priorAction.updateEnabled();
		nextAction.updateEnabled();
		parentAction.updateEnabled();
		newFolderAction.updateEnabled();
	}

	private Void handleExternalResourceEvent( ResourceWatchEvent event ) {
		//log.atConfig().log( "External resource event: %s %s", event.type(), event.resource() );
		try {
			Resource folder = event.resource();
			if( !event.resource().isFolder() ) folder = getProgram().getResourceManager().getParent( event.resource() );

			// If the event is for the current folder...reload the folder
			if( folder.equals( currentFolder ) ) loadFolder( currentFolder );
		} catch( ResourceException exception ) {
			handleResourceException( exception );
		}

		return null;
	}

	private void loadFolder( Resource resource ) {
		loadFolder( resource, null );
	}

	private void loadFolder( Resource resource, Resource editResource ) {
		if( isEditing() ) return;

		try {
			// Unregister the resource from the watcher
			if( FileScheme.ID.equals( resource.getScheme().getName() ) ) {
				getProgram().getResourceWatchService().removeWatch( resource, eventCallback );
			}

			currentFolder = resource;

			// Set the current folder with the resource manager
			if( FileScheme.ID.equals( resource.getScheme().getName() ) ) {
				getProgram().getResourceManager().setCurrentFileFolder( resource );
			}

			Fx.run( this::updateActionState );
			Fx.run( this::closeUserNotice );

			// Register the resource with the watcher
			if( FileScheme.ID.equals( resource.getScheme().getName() ) ) {
				getProgram().getResourceWatchService().registerWatch( resource, eventCallback );
			}
		} catch( ResourceException exception ) {
			handleResourceException( exception );
		}

		getProgram().getTaskManager().submit( Task.of(
			"load-resource", () -> {
				try {
					parentResource = getProgram().getResourceManager().getParent( resource );
					List<Resource> resources = resource.getChildren();
					Fx.run( () -> {
						this.resources.setAll( resources );
						if( editResource != null ) editResourceName( editResource );
					} );
				} catch( ResourceException exception ) {
					handleResourceException( exception );
				}
			}
		) );
	}

	private void activateUriField() {
		Fx.run( () -> {
			uriField.requestFocus();
			uriField.positionCaret( uriField.getText().length() );
		} );
	}

	private void doCreateNewFolder() {
		if( currentFolder == null ) return;

		try {
			Scheme scheme = currentFolder.getScheme();

			// Start with the current folder
			String newFolderName = Rb.textOr( RbKey.LABEL, "new-folder", "New Folder" ) + "/";
			Resource resource = getProgram().getResourceManager().resolve( currentFolder, newFolderName );
			Resource newFolder = getNextIndexedResource( resource );

			// Get next indexed resource
			scheme.createFolder( newFolder );

			// Reload the current folder, and start editing the new folder name
			loadFolder( currentFolder, newFolder );
		} catch( ResourceException exception ) {
			handleResourceException( exception );
		}
	}

	private void doDeleteSelectedFiles() {
		List<Resource> selectedResources = new ArrayList<>( resourceTable.getSelectionModel().getSelectedItems() );
		getProgram().getResourceManager().deleteResources( selectedResources );
	}

	private Resource getNextIndexedResource( Resource resource ) throws ResourceException {
		Scheme scheme = resource.getScheme();

		if( !scheme.exists( resource ) ) return resource;

		boolean isFolder = resource.isFolder();
		Resource parent = getProgram().getResourceManager().getParent( resource );
		List<String> children = scheme.listResources( parent ).stream().map( Resource::getName ).toList();
		String nextName = FileUtil.getNextIndexedName( children, resource.getName() ) + (isFolder ? "/" : "");
		return getProgram().getResourceManager().resolve( parent, nextName );
	}

	private void notifyUser( String messageKey, String... parameters ) {
		String message = Rb.text( "program", messageKey, (Object[])parameters );

		Fx.run( () -> {
			userMessage.setText( message );
			userNotice.setManaged( true );
			userNotice.setVisible( true );
		} );
	}

	private void closeUserNotice() {
		userNotice.setVisible( false );
		userNotice.setManaged( false );
	}

	private Guide createGuide() {
		Guide guide = new Guide();

		// TODO Load the resource roots
		// Go through the supported schemes and get the roots
		// or, let the roots be defined in resource manager
		// or, let the roots be defined in resource tool

		try {
			// Bookmarks

			// Project folders

			// User folders
			Path home = FileSystemView.getFileSystemView().getHomeDirectory().toPath();
			guide.addNode( createGuideNode( "Home", "resource-home", home ) );
			guide.addNode( createGuideNode( "Desktop", "resource-desktop", OperatingSystem.UserFolder.DESKTOP ) );
			guide.addNode( createGuideNode( "Documents", "resource-documents", OperatingSystem.UserFolder.DOCUMENTS ) );
			guide.addNode( createGuideNode( "Downloads", "resource-download", OperatingSystem.UserFolder.DOWNLOAD ) );
			guide.addNode( createGuideNode( "Music", "resource-music", OperatingSystem.UserFolder.MUSIC ) );
			guide.addNode( createGuideNode( "Photos", "resource-photos", OperatingSystem.UserFolder.PHOTOS ) );
			guide.addNode( createGuideNode( "Videos", "resource-videos", OperatingSystem.UserFolder.VIDEOS ) );

			// Recent

			// Computer with all the drives
			for( Path path : FileSystems.getDefault().getRootDirectories() ) {
				guide.addNode( createGuideNode( UriUtil.parseName( path.toUri() ), "resource-root", path.toString() ) );
			}
		} catch( ResourceException exception ) {
			handleResourceException( exception );
		}

		return guide;
	}

	private GuideNode createGuideNode( String name, String icon, OperatingSystem.UserFolder folder ) throws ResourceException {
		return createGuideNode( name, icon, OperatingSystem.getUserFolder( folder ) );
	}

	private GuideNode createGuideNode( String name, String icon, String path ) throws ResourceException {
		return createGuideNode( name, icon, Paths.get( path ) );
	}

	private GuideNode createGuideNode( String name, String icon, Path path ) throws ResourceException {
		Resource resource = getProgram().getResourceManager().createResource( path );
		GuideNode node = new GuideNode( getProgram(), resource.getUri().toString(), name, icon );
		resource.register( Resource.ICON, e -> node.setIcon( e.getNewValue() ) );
		return node;
	}

	private boolean isEditing() {
		return resourceTable.getEditingCell() != null;
	}

	private void doUpdateResourceName( TableColumn.CellEditEvent<Resource, Label> event ) {
		try {
			Resource resource = event.getRowValue();
			String newName = UriUtil.encode( event.getNewValue().getText() );
			URI parent = UriUtil.getParent( resource.getUri() );
			URI uri = parent.resolve( newName );

			Resource newResource = getProgram().getResourceManager().createResource( uri );
			resource.getScheme().rename( resource, newResource );
		} catch( ResourceException exception ) {
			handleResourceException( exception );
		}
	}

	private void handleResourceException( ResourceException exception ) {
		notifyUser( "resource-error", exception.getMessage() );
		log.atSevere().withCause( exception ).log();
	}

	/**
	 * A table value factory for the resource icon.
	 */
	private static class IconValueFactory implements Callback<TableColumn.CellDataFeatures<Resource, Node>, ObservableValue<Node>> {

		private final Xenon program;

		public IconValueFactory( Xenon program ) {
			this.program = program;
		}

		@Override
		public ObservableValue<Node> call( TableColumn.CellDataFeatures<Resource, Node> resourceStringCellDataFeatures ) {
			Resource resource = resourceStringCellDataFeatures.getValue();
			Node icon = program.getIconLibrary().getIcon( resourceStringCellDataFeatures.getValue().getIcon() );
			Label label = new Label( "", icon );
			// Add the resource, as a property on the label, so it can be used for sorting
			label.getProperties().put( "resource", resource );
			return new ReadOnlyObjectWrapper<>( label );
		}

	}

	/**
	 * A table value factory for the resource label.
	 */
	private static class NameValueFactory implements Callback<TableColumn.CellDataFeatures<Resource, Label>, ObservableValue<Label>> {

		@Override
		public ObservableValue<Label> call( TableColumn.CellDataFeatures<Resource, Label> resourceStringCellDataFeatures ) {
			Resource resource = resourceStringCellDataFeatures.getValue();
			String name = resource.getName();
			Label label = new Label( name );
			// Add the resource, as a property on the label, so it can be used for sorting
			label.getProperties().put( "resource", resource );
			return new ReadOnlyObjectWrapper<>( label );
		}

	}

	/**
	 * A table value factory for the resource size.
	 */
	private static class SizeValueFactory implements Callback<TableColumn.CellDataFeatures<Resource, Node>, ObservableValue<Node>> {

		@Override
		public ObservableValue<Node> call( TableColumn.CellDataFeatures<Resource, Node> resourceStringCellDataFeatures ) {
			try {
				Resource resource = resourceStringCellDataFeatures.getValue();
				long size = resource.getSize();

				String text = Rb.text( "resource", "resource-open-folder-size", size );
				if( !resource.isFolder() ) text = FileUtil.getHumanSize( size, false, true );

				Label label = new Label( text );
				// Add the resource, as a property on the label, so it can be used for sorting
				label.getProperties().put( "resource", resource );
				return new ReadOnlyObjectWrapper<>( label );
			} catch( ResourceException exception ) {
				log.atWarn().withCause( exception ).log();
				return new ReadOnlyObjectWrapper<>( null );
			}
		}

	}

	private static final class ResourceLabelComparator implements Comparator<Label> {

		private final Comparator<Resource> resourceComparator = new ResourceTypeAndNameComparator();

		@Override
		public int compare( Label o1, Label o2 ) {
			Resource resource1 = (Resource)o1.getProperties().get( "resource" );
			Resource resource2 = (Resource)o2.getProperties().get( "resource" );
			return resourceComparator.compare( resource1, resource2 );
		}

	}

	private static final class ResourceSizeComparator implements Comparator<Node> {

		private final Comparator<Resource> resourceComparator = new ResourceTypeAndSizeComparator();

		@Override
		public int compare( Node o1, Node o2 ) {
			Resource resource1 = (Resource)o1.getProperties().get( "resource" );
			Resource resource2 = (Resource)o2.getProperties().get( "resource" );
			return resourceComparator.compare( resource1, resource2 );
		}

	}

	private static final class StringLabelConverter extends StringConverter<Label> {

		@Override
		public String toString( Label object ) {
			if( object == null ) return null;
			return object.getText();
		}

		@Override
		public Label fromString( String string ) {
			if( string == null ) return null;
			return new Label( string );
		}

	}

	private final class RefreshAction extends ProgramAction {

		private RefreshAction( Xenon program ) {
			super( program );
		}

		@Override
		public boolean isEnabled() {
			return currentFolder != null;
		}

		@Override
		public void handle( ActionEvent event ) {
			loadFolder( currentFolder );
		}

	}

	private final class PriorAction extends ProgramAction {

		private PriorAction( Xenon program ) {
			super( program );
		}

		@Override
		public boolean isEnabled() {
			return currentIndex > 0;
		}

		@Override
		public void handle( ActionEvent event ) {
			if( currentIndex > 0 ) currentIndex--;
			selectResource( history.get( currentIndex ), false );
		}

	}

	private final class NextAction extends ProgramAction {

		private NextAction( Xenon program ) {
			super( program );
		}

		@Override
		public boolean isEnabled() {
			return currentIndex < history.size() - 1;
		}

		@Override
		public void handle( ActionEvent event ) {
			if( currentIndex < history.size() - 1 ) currentIndex++;
			selectResource( history.get( currentIndex ), false );
		}

	}

	private final class ParentAction extends ProgramAction {

		private ParentAction( Xenon program ) {
			super( program );
		}

		@Override
		public boolean isEnabled() {
			return currentFolder != null && UriUtil.hasParent( currentFolder.getUri() );
		}

		@Override
		public void handle( ActionEvent event ) {
			if( mode == Mode.OPEN ) {
				selectResource( parentResource );
			} else if( mode == Mode.SAVE ) {
				selectResource( parentResource.getUri().resolve( currentFilename ) );
			}
		}

	}

	private final class NewFolderAction extends ProgramAction {

		private NewFolderAction( Xenon program ) {
			super( program );
		}

		@Override
		public boolean isEnabled() {
			return currentFolder != null;
		}

		@Override
		public void handle( ActionEvent event ) {
			doCreateNewFolder();
		}

	}

	private final class DeleteAction extends ProgramAction {

		private DeleteAction( Xenon program ) {
			super( program );
		}

		@Override
		public boolean isEnabled() {
			return !resourceTable.getSelectionModel().getSelectedItems().isEmpty();
		}

		@Override
		public void handle( ActionEvent event ) {
			doDeleteSelectedFiles();
		}

	}

}
