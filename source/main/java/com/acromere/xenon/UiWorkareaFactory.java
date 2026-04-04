package com.acromere.xenon;

import com.acromere.product.Rb;
import com.acromere.settings.Settings;
import com.acromere.util.IdGenerator;
import com.acromere.xenon.workpane.Workpane;
import com.acromere.xenon.workpane.WorkpaneEdge;
import com.acromere.xenon.workpane.WorkpaneView;
import com.acromere.xenon.workspace.Workarea;
import com.acromere.zerra.color.Colors;
import com.acromere.zerra.color.Paints;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import lombok.CustomLog;

// For use with UiManager only
@CustomLog
class UiWorkareaFactory {

	private final Xenon program;

	UiWorkareaFactory( Xenon program ) {
		this.program = program;
	}

	Workarea createWorkarea( String name ) {
		LinearGradient paint = new LinearGradient( 0, 0, 0.5, 1, true, CycleMethod.NO_CYCLE, new Stop( 0, Color.BLUEVIOLET.darker().darker() ), new Stop( 1, Color.TRANSPARENT ) );

		Workarea area = new Workarea();
		area.setUid( IdGenerator.getId() );
		area.setName( name );
		area.setPaint( paint );
		area.setIcon( Ui.WORKAREA );

		Settings areaSettings = program.getSettingsManager().getSettings( ProgramSettings.AREA, area.getUid() );
		restoreWorkareaFromSettings( area, areaSettings );
		bindSettings( area, areaSettings );

		return area;
	}

	Workarea createDefaultWorkarea() {
		Workarea area = new Workarea();
		area.setUid( IdGenerator.getId() );
		area.setIcon( Ui.WORKAREA );
		area.setName( Rb.text( RbKey.WORKAREA, Ui.WORKAREA_NEW_TITLE, "New Workarea" ) );
		Settings areaSettings = program.getSettingsManager().getSettings( ProgramSettings.AREA, area.getUid() );
		restoreWorkareaFromSettings( area, areaSettings );
		bindSettings( area, areaSettings );
		return area;
	}

	void restoreWorkareaFromSettings( Workarea area, Settings settings ) {
		// Restore state from settings
		area.setUid( settings.getName() );
		area.setName( settings.get( Ui.NAME, area.getName() ) );
		area.setOrder( settings.get( Ui.ORDER, Integer.class, area.getOrder() ) );
		area.setPaint( Paints.parse( settings.get( Ui.PAINT, Paints.toString( area.getPaint() ) ) ) );
		area.setColor( Colors.parse( settings.get( Ui.COLOR, Colors.toString( area.getColor() ) ) ) );
		area.setActive( settings.get( Ui.ACTIVE, Boolean.class, area.isActive() ) );
	}

	@SuppressWarnings("unused")
	void bindSettings( Workarea area, Settings settings ) {
		// Store the current values
		settings.set( Ui.PAINT, Paints.toString( area.getPaint() ) );
		settings.set( Ui.COLOR, Colors.toString( area.getColor() ) );
		settings.set( Ui.NAME, area.getName() );
		settings.set( Ui.ACTIVE, area.isActive() );
		
		settings.set( Ui.DOCK_TOP_SIZE, area.getTopDockSize() );
		settings.set( Ui.DOCK_LEFT_SIZE, area.getLeftDockSize() );
		settings.set( Ui.DOCK_RIGHT_SIZE, area.getRightDockSize() );
		settings.set( Ui.DOCK_BOTTOM_SIZE, area.getBottomDockSize() );

		settings.set( Ui.VIEW_ACTIVE, area.getActiveView() == null ? null : area.getActiveView().getUid() );
		settings.set( Ui.VIEW_DEFAULT, area.getDefaultView() == null ? null : area.getDefaultView().getUid() );
		settings.set( Ui.VIEW_MAXIMIZED, area.getMaximizedView() == null ? null : area.getMaximizedView().getUid() );

		// Add the change listeners
		area.nameProperty().addListener( ( p, o, n ) -> settings.set( Ui.NAME, n ) );
		area.orderProperty().addListener( ( p, o, n ) -> settings.set( Ui.ORDER, n ) );
		area.paintProperty().addListener( ( p, o, n ) -> settings.set( Ui.PAINT, Paints.toString( n ) ) );
		area.activeProperty().addListener( ( p, o, n ) -> settings.set( Ui.ACTIVE, n ) );
		area.workspaceProperty().addListener( ( p, o, n ) -> settings.set( Ui.PARENT_SPACE_ID, n == null ? null : n.getUid() ) );

		// Bind existing views and edges
		area.getEdges().forEach( e -> bindEdge( area, e ) );
		area.getViews().forEach( v -> bindView( area, v ) );

		// Add the change listeners
		area.topDockSizeProperty().addListener( ( p, o, n ) -> settings.set( Ui.DOCK_TOP_SIZE, n ) );
		area.leftDockSizeProperty().addListener( ( p, o, n ) -> settings.set( Ui.DOCK_LEFT_SIZE, n ) );
		area.rightDockSizeProperty().addListener( ( p, o, n ) -> settings.set( Ui.DOCK_RIGHT_SIZE, n ) );
		area.bottomDockSizeProperty().addListener( ( p, o, n ) -> settings.set( Ui.DOCK_BOTTOM_SIZE, n ) );
		area.activeViewProperty().addListener( ( p, o, n ) -> settings.set( Ui.VIEW_ACTIVE, n == null ? null : n.getUid() ) );
		area.defaultViewProperty().addListener( ( p, o, n ) -> settings.set( Ui.VIEW_DEFAULT, n == null ? null : n.getUid() ) );
		area.maximizedViewProperty().addListener( ( p, o, n ) -> settings.set( Ui.VIEW_MAXIMIZED, n == null ? null : n.getUid() ) );
		area.getChildrenUnmodifiable().addListener( (ListChangeListener<? super Node>)c -> processAreaChildChanges( area, c ) );
	}

	@SuppressWarnings("unused")
	private void bindEdge( Workpane pane, WorkpaneEdge edge ) {
		// Store the current values
		Settings edgeSettings = program.getSettingsManager().getSettings( ProgramSettings.EDGE, edge.getUid() );
		edgeSettings.set( Ui.PARENT_AREA_ID, pane.getUid() );
		edgeSettings.set( Ui.POSITION, edge.getPosition() );
		edgeSettings.set( Ui.ORIENTATION, edge.getOrientation().name().toLowerCase() );
		edgeSettings.set( Ui.T, edge.getTopEdge() == null ? null : edge.getTopEdge().getUid() );
		edgeSettings.set( Ui.L, edge.getLeftEdge() == null ? null : edge.getLeftEdge().getUid() );
		edgeSettings.set( Ui.R, edge.getRightEdge() == null ? null : edge.getRightEdge().getUid() );
		edgeSettings.set( Ui.B, edge.getBottomEdge() == null ? null : edge.getBottomEdge().getUid() );

		// Add the change listeners
		edge.positionProperty().addListener( ( p, o, n ) -> edgeSettings.set( Ui.POSITION, n ) );
		edge.orientationProperty().addListener( ( p, o, n ) -> edgeSettings.set( Ui.ORIENTATION, n ) );
		edge.topEdgeProperty().addListener( ( p, o, n ) -> edgeSettings.set( Ui.T, n == null ? null : n.getUid() ) );
		edge.leftEdgeProperty().addListener( ( p, o, n ) -> edgeSettings.set( Ui.L, n == null ? null : n.getUid() ) );
		edge.rightEdgeProperty().addListener( ( p, o, n ) -> edgeSettings.set( Ui.R, n == null ? null : n.getUid() ) );
		edge.bottomEdgeProperty().addListener( ( p, o, n ) -> edgeSettings.set( Ui.B, n == null ? null : n.getUid() ) );
	}

	private void unbindEdge( WorkpaneEdge edge ) {
		String id = edge.getUid();
		if( id == null ) return;
		program.getSettingsManager().getSettings( ProgramSettings.EDGE, id ).delete();
	}

	@SuppressWarnings("unused")
	private void bindView( Workpane workpane, WorkpaneView view ) {
		// Store the current values
		Settings viewSettings = program.getSettingsManager().getSettings( ProgramSettings.VIEW, view.getUid() );
		viewSettings.set( Ui.PARENT_AREA_ID, workpane.getUid() );
		viewSettings.set( Ui.PLACEMENT, view.getPlacement() == null ? null : view.getPlacement().name().toLowerCase() );
		viewSettings.set( Ui.T, view.getTopEdge() == null ? null : view.getTopEdge().getUid() );
		viewSettings.set( Ui.L, view.getLeftEdge() == null ? null : view.getLeftEdge().getUid() );
		viewSettings.set( Ui.R, view.getRightEdge() == null ? null : view.getRightEdge().getUid() );
		viewSettings.set( Ui.B, view.getBottomEdge() == null ? null : view.getBottomEdge().getUid() );

		// Add the change listeners
		view.placementProperty().addListener( ( p, o, n ) -> viewSettings.set( Ui.PLACEMENT, n == null ? null : n.name().toLowerCase() ) );
		view.topEdgeProperty().addListener( ( p, o, n ) -> viewSettings.set( Ui.T, n == null ? null : n.getUid() ) );
		view.leftEdgeProperty().addListener( ( p, o, n ) -> viewSettings.set( Ui.L, n == null ? null : n.getUid() ) );
		view.rightEdgeProperty().addListener( ( p, o, n ) -> viewSettings.set( Ui.R, n == null ? null : n.getUid() ) );
		view.bottomEdgeProperty().addListener( ( p, o, n ) -> viewSettings.set( Ui.B, n == null ? null : n.getUid() ) );
	}

	private void unbindView( WorkpaneView view ) {
		String id = view.getUid();
		if( id == null ) return;
		program.getSettingsManager().getSettings( ProgramSettings.VIEW, id ).delete();
	}

	private void processAreaChildChanges( Workpane area, ListChangeListener.Change<? extends Node> change ) {
		while( change.next() ) {
			change.getAddedSubList().stream().filter( WorkpaneEdge.class::isInstance ).forEach( n -> bindEdge( area, (WorkpaneEdge)n ) );
			change.getAddedSubList().stream().filter( WorkpaneView.class::isInstance ).forEach( n -> bindView( area, (WorkpaneView)n ) );
			change.getRemoved().stream().filter( WorkpaneEdge.class::isInstance ).forEach( n -> unbindEdge( (WorkpaneEdge)n ) );
			change.getRemoved().stream().filter( WorkpaneView.class::isInstance ).forEach( n -> unbindView( (WorkpaneView)n ) );
		}
	}

}
