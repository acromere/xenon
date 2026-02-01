package com.acromere.xenon.action;

import com.acromere.xenon.Xenon;
import com.acromere.xenon.ProgramAction;
import com.acromere.xenon.resource.type.ProgramSearchType;
import com.acromere.xenon.tool.SearchTool;
import com.acromere.xenon.workpane.Tool;
import javafx.event.ActionEvent;

import java.util.Set;

public class SearchToggleAction extends ProgramAction {

	public SearchToggleAction( Xenon program ) {
		super( program );
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void handle( ActionEvent event ) {
		Set<Tool> tools = getProgram().getWorkspaceManager().getActiveWorkpaneTools( SearchTool.class );

		if( tools.isEmpty() ) {
			// Open the index search tool
			getProgram().getResourceManager().openAsset( ProgramSearchType.URI );
		} else {
			// Close the index search tool
			tools.forEach( Tool::close );
		}
	}

}

