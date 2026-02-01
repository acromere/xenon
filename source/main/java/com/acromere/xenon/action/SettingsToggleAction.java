package com.acromere.xenon.action;

import com.acromere.xenon.ProgramAction;
import com.acromere.xenon.Xenon;
import com.acromere.xenon.resource.type.ProgramSettingsType;
import com.acromere.xenon.tool.settings.SettingsTool;
import com.acromere.xenon.workpane.Tool;
import javafx.event.ActionEvent;

import java.util.Set;

public class SettingsToggleAction extends ProgramAction {

	public SettingsToggleAction( Xenon program ) {
		super( program );
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void handle( ActionEvent event ) {
		Set<Tool> tools = getProgram().getWorkspaceManager().getActiveWorkpaneTools( SettingsTool.class );

		if( tools.isEmpty() ) {
			// Open the settings tool
			getProgram().getResourceManager().openAsset( ProgramSettingsType.URI );
		} else {
			// Close the settings tools
			tools.forEach( Tool::close );
		}
	}
}
