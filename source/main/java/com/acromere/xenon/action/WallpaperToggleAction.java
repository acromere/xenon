package com.acromere.xenon.action;

import com.acromere.settings.Settings;
import com.acromere.xenon.ProgramAction;
import com.acromere.xenon.Xenon;
import com.acromere.xenon.ProgramSettings;
import javafx.event.ActionEvent;

public class WallpaperToggleAction extends ProgramAction {

	public WallpaperToggleAction( Xenon program ) {
		super( program );
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void handle( ActionEvent event ) {
		Settings settings = getProgram().getSettingsManager().getSettings( ProgramSettings.PROGRAM );
		boolean enabled = Boolean.parseBoolean( settings.get( "workspace-scenery-image-enabled", "true" ) );
		settings.set( "workspace-scenery-image-enabled", !enabled );
	}

}
