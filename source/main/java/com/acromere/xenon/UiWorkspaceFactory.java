package com.acromere.xenon;

import com.acromere.product.Rb;
import com.acromere.settings.Settings;
import com.acromere.util.IdGenerator;
import com.acromere.xenon.resource.type.ProgramWelcomeType;
import com.acromere.xenon.workspace.Workarea;
import com.acromere.xenon.workspace.Workspace;
import lombok.Getter;

// For use with UiManager only
class UiWorkspaceFactory {

	@Getter
	private final Xenon program;

	UiWorkspaceFactory( Xenon program ) {
		this.program = program;
	}

	Workspace create() {
		Workspace space = new Workspace( program );
		space.setUid( IdGenerator.getId() );
		return space;
	}

	Workspace createDefaultWorkspace( UiWorkareaFactory areaFactory ) {
		// Create the default workspace
		Workspace space = new Workspace( program );
		space.setUid( IdGenerator.getId() );
		space.initializeScene( Ui.DEFAULT_WIDTH, Ui.DEFAULT_HEIGHT );

		Settings spaceSettings = program.getSettingsManager().getSettings( ProgramSettings.SPACE, space.getUid() );
		applyWorkspaceSettings( space, spaceSettings );
		linkWorkspaceSettingsListeners( space, spaceSettings );

		String themeId = program.getWorkspaceManager().getThemeId();
		space.setTheme( program.getThemeManager().getMetadata( themeId ).getUrl() );

		// Create the default workarea
		Workarea area = new Workarea();
		area.setUid( IdGenerator.getId() );
		area.setIcon( "workarea" );
		area.setName( Rb.text( RbKey.WORKAREA, "workarea-new-title", "New Workarea" ) );
		Settings areaSettings = program.getSettingsManager().getSettings( ProgramSettings.AREA, area.getUid() );
		areaFactory.applyWorkareaSettings( area, areaSettings );
		areaFactory.linkWorkareaSettingsListeners( area, areaSettings );

		// Add the workarea to the workspace
		space.addWorkarea( area );

		// Activate the new workarea and workspace
		space.setActiveWorkarea( area );
		program.getWorkspaceManager().setActiveWorkspace( space );

		// Add the welcome tool to the default workarea
		boolean isEmptyWorkspace = program.getProgramParameters().isSet( XenonTestFlag.EMPTY_WORKSPACE );
		if( !isEmptyWorkspace ) program.getResourceManager().openAsset( ProgramWelcomeType.URI );
		return space;
	}

	Workspace applyWorkspaceSettings( Workspace workspace, Settings settings ) {
		// Due to differences in how FX handles stage sizes (width and height) on
		// different operating systems, the width and height from the scene, not the
		// stage, are used. This includes the listeners for the width and height
		// properties below.
		Double w = settings.get( Ui.W, Double.class, Ui.DEFAULT_WIDTH );
		Double h = settings.get( Ui.H, Double.class, Ui.DEFAULT_HEIGHT );
		workspace.initializeScene( w, h );

		// Position the stage if x and y are specified
		// If not specified the stage is centered on the screen
		Double x = settings.get( Ui.X, Double.class, null );
		Double y = settings.get( Ui.Y, Double.class, null );
		if( x != null ) workspace.setX( x );
		if( y != null ) workspace.setY( y );

		updateThemeFromSettings( workspace, settings );
		workspace.applySettings();

		return workspace;
	}

	Workspace linkWorkspaceSettingsListeners( Workspace workspace, Settings settings ) {
		settings.set( Ui.MAXIMIZED, workspace.isMaximized() );
		settings.set( Ui.X, workspace.getX() );
		settings.set( Ui.Y, workspace.getY() );
		settings.set( Ui.W, workspace.getScene().getWidth() );
		settings.set( Ui.H, workspace.getScene().getHeight() );

		// Add the property listeners
		workspace.maximizedProperty().addListener( ( v, o, n ) -> {
			if( workspace.isShowing() ) settings.set( Ui.MAXIMIZED, n );
		} );
		workspace.xProperty().addListener( ( v, o, n ) -> {
			if( !workspace.isMaximized() ) settings.set( Ui.X, n );
		} );
		workspace.yProperty().addListener( ( v, o, n ) -> {
			if( !workspace.isMaximized() ) settings.set( Ui.Y, n );
		} );
		workspace.getScene().widthProperty().addListener( ( v, o, n ) -> {
			if( !workspace.isMaximized() ) settings.set( Ui.W, n );
		} );
		workspace.getScene().heightProperty().addListener( ( v, o, n ) -> {
			if( !workspace.isMaximized() ) settings.set( Ui.H, n );
		} );
		return workspace;
	}

	private void updateThemeFromSettings( Workspace workspace, Settings settings ) {
		String themeId = settings.get( "theme", getProgram().getWorkspaceManager().getThemeId() );
		workspace.setTheme( getProgram().getThemeManager().getMetadata( themeId ).getUrl() );
	}

}
