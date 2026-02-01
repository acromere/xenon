package com.acromere.xenon.action;

import com.acromere.xenon.Xenon;
import com.acromere.xenon.ProgramAction;
import javafx.event.ActionEvent;
import lombok.CustomLog;

@CustomLog
public class ActionMenuAction extends ProgramAction {

	public ActionMenuAction( Xenon program ) {
		super( program );
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void handle( ActionEvent event ) {
		getProgram().getWorkspaceManager().getActiveWorkspace().showProgramMenu( event );
	}

}
