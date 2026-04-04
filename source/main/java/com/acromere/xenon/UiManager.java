package com.acromere.xenon;

import com.acromere.skill.Controllable;
import com.acromere.xenon.workspace.Workarea;
import lombok.CustomLog;
import lombok.Getter;

@Getter
@CustomLog
public final class UiManager implements Controllable<UiManager> {

	public static final String SPACE = "space";

	public static final String AREA = "area";

	public static final String VIEW = "view";

	public static final String EDGE = "edge";

	public static final String TOOL = "tool";

	public static final String PARENT_SPACE_ID = "space-id";

	public static final String PARENT_AREA_ID = "area-id";

	public static final String PARENT_VIEW_ID = "view-id";

	public static final String ICON = "icon";

	public static final String TITLE = "title";

	public static final String NAME = "name";

	public static final String DESCRIPTION = "description";

	public static final String ORDER = "order";

	public static final String ACTIVE = "active";

	public static final String MAXIMIZED = "maximized";

	public static final String PAINT = "paint";

	public static final String COLOR = "color";

	public static final String ORIENTATION = "orientation";

	public static final String POSITION = "position";

	private final Xenon program;

	private final UiWorkspaceFactory workspaceFactory;

	private final UiWorkareaFactory workareaFactory;

	private boolean running;

	public UiManager( Xenon program ) {
		this.program = program;
		this.workspaceFactory = new UiWorkspaceFactory( program );
		this.workareaFactory = new UiWorkareaFactory( program );
	}

	public UiManager start() {
		running = true;
		return this;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	public UiManager stop() {
		running = false;
		return this;
	}

	public Workarea createWorkarea( String name ) {
		return workareaFactory.createWorkarea( name );
	}

	public void reset() {
		getProgram().getSettingsManager().getSettings( ProgramSettings.UI ).delete();
	}
}
