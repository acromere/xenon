package com.acromere.xenon.action;

import com.acromere.xenon.ProgramAction;
import com.acromere.xenon.Xenon;
import com.acromere.xenon.RestartJob;
import javafx.event.ActionEvent;

public class MockUpdateAction extends ProgramAction {

	public MockUpdateAction( Xenon program ) {
		super( program );
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void handle( ActionEvent event ) {
		getProgram().requestRestart( RestartJob.Mode.MOCK_UPDATE );
	}

}
