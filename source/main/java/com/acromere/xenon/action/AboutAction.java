package com.acromere.xenon.action;

import com.acromere.xenon.ProgramAction;
import com.acromere.xenon.Xenon;
import com.acromere.xenon.resource.type.ProgramAboutType;
import javafx.event.ActionEvent;

public class AboutAction extends ProgramAction {

	public AboutAction( Xenon program ) {
		super( program );
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void handle( ActionEvent event ) {
		getProgram().getResourceManager().openAsset( ProgramAboutType.URI );
	}

}
