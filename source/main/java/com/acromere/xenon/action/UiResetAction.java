package com.acromere.xenon.action;

import com.acromere.xenon.ProgramAction;
import com.acromere.xenon.RestartJob;
import com.acromere.xenon.UiFactory;
import com.acromere.xenon.Xenon;
import javafx.event.ActionEvent;
import lombok.CustomLog;

@CustomLog
public class UiResetAction extends ProgramAction {

	public UiResetAction( Xenon program ) {
		super( program );
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void handle( ActionEvent event ) {
		// Reset the UI settings before restarting
		new UiFactory( getProgram() ).reset();

		// Restart the application
		try {
			getProgram().requestRestart( RestartJob.Mode.RESTART );
		} catch( Throwable throwable ) {
			log.atError( throwable ).log( "Error requesting restart" );
		}
	}

}
