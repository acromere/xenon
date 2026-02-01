package com.acromere.xenon.action;

import com.acromere.xenon.ProgramAction;
import com.acromere.xenon.Xenon;
import javafx.event.ActionEvent;

public class UpdateAction extends ProgramAction {

	public UpdateAction( Xenon program ) {
		super( program );
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void handle( ActionEvent event ) {
		getProgram().getProductManager().checkForUpdates( true );
	}

}
