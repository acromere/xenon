package com.acromere.xenon;

import com.acromere.settings.Settings;
import com.acromere.util.IdGenerator;
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

	public Workspace linkWorkspaceSettingsListeners( Workspace workspace, Settings settings ) {
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
