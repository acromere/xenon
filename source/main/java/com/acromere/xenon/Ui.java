package com.acromere.xenon;

import javafx.scene.layout.BorderStroke;

public interface Ui {

	String BOTTOM = "bottom";

	String LEFT = "left";

	String RIGHT = "right";

	String TOP = "top";

	String MAXIMIZED = "maximized";

	String ACTIVE = "active";

	String AREA = "area";

	String COLOR = "color";

	String DESCRIPTION = "description";

	String EDGE = "edge";

	String ICON = "icon";

	String NAME = "name";

	String ORDER = "order";

	String ORIENTATION = "orientation";

	String PAINT = "paint";

	String POSITION = "position";

	String SPACE = "space";

	String TITLE = "title";

	String TOOL = "tool";

	String VIEW = "view";

	String PARENT_AREA_ID = "area-id";

	String PARENT_SPACE_ID = "space-id";

	String PARENT_VIEW_ID = "view-id";

	String PLACEMENT = "placement";

	String THEME = "theme";

	String WORKAREA = "workarea";

	String WORKAREA_NEW_TITLE = "workarea-new-title";

	// Bottom key
	String B = "b";

	// Left key
	String L = "l";

	// Right key
	String R = "r";

	// Top key
	String T = "t";

	// X-coordinate key
	String X = "x";

	// Y-coordinate key
	String Y = "y";

	// Width key
	String W = "w";

	// Height key
	String H = "h";
	double DEFAULT_WIDTH = 960;
	double DEFAULT_HEIGHT = 600;
	double PAD = BorderStroke.THICK.getTop();
	String DOCK_TOP_SIZE = "dock-top-size";
	String DOCK_LEFT_SIZE = "dock-left-size";
	String DOCK_RIGHT_SIZE = "dock-right-size";
	String DOCK_BOTTOM_SIZE = "dock-bottom-size";
	String VIEW_ACTIVE = "view-active";
	String VIEW_DEFAULT = "view-default";
	String VIEW_MAXIMIZED = "view-maximized";
}
