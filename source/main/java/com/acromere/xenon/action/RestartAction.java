package com.acromere.xenon.action;

import com.acromere.xenon.Xenon;
import com.acromere.xenon.ProgramAction;
import com.acromere.xenon.RestartJob;
import javafx.event.ActionEvent;
import lombok.CustomLog;

@CustomLog
public class RestartAction extends ProgramAction {

	public RestartAction( Xenon program ) {
		super( program );
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void handle( ActionEvent event ) {
		try {
			getProgram().requestRestart( RestartJob.Mode.RESTART );
		} catch( Throwable throwable ) {
			log.atError( throwable ).log( "Error requesting restart" );
		}
	}

}
