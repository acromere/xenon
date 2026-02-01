package com.acromere.xenon.action;

import com.acromere.xenon.ProgramAction;
import com.acromere.xenon.Xenon;
import com.acromere.xenon.task.Task;
import javafx.event.ActionEvent;
import lombok.CustomLog;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

@CustomLog
public class HelpAction extends ProgramAction {

	private static final URI USER_GUIDE = URI.create( "https://www.acromere.com/product/xenon/docs/user-guide" );

	public HelpAction( Xenon program ) {
		super( program );
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void handle( ActionEvent event ) {
		getProgram().getTaskManager().submit( Task.of( () -> {
			try {
				// TODO This could be context sensitive at some point
				Desktop.getDesktop().browse( USER_GUIDE );
			} catch( IOException exception ) {
				log.atWarn( exception );
			}
		} ) );

	}

}

