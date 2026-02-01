package com.acromere.xenon.action;

import com.acromere.xenon.ProgramAction;
import com.acromere.xenon.Xenon;
import com.acromere.xenon.resource.type.ProgramNoticeType;
import com.acromere.xenon.tool.NoticeTool;
import com.acromere.xenon.workpane.Tool;
import javafx.event.ActionEvent;

import java.util.Set;

public class NoticeToggleAction extends ProgramAction {

	public NoticeToggleAction( Xenon program ) {
		super( program );
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void handle( ActionEvent event ) {
		Set<Tool> tools = getProgram().getWorkspaceManager().getActiveWorkpaneTools( NoticeTool.class );

		if( tools.isEmpty() ) {
			// Open the notice tool
			getProgram().getResourceManager().openAsset( ProgramNoticeType.URI );
		} else {
			// Close the notice tools
			tools.forEach( Tool::close );
		}
	}

}
