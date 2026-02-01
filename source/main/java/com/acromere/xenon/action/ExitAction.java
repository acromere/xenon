package com.acromere.xenon.action;

import com.acromere.xenon.ProgramAction;
import com.acromere.xenon.Xenon;
import javafx.event.ActionEvent;

public class ExitAction extends ProgramAction {

	public ExitAction( Xenon program ) {
		super( program );
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void handle( ActionEvent event ) {
		getProgram().requestExit( false, true );
	}

}
