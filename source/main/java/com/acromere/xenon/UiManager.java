package com.acromere.xenon;

import com.acromere.skill.Controllable;
import com.acromere.xenon.workspace.Workarea;
import lombok.CustomLog;
import lombok.Getter;

@Getter
@CustomLog
public final class UiManager implements Controllable<UiManager> {

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
