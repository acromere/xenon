package com.acromere.xenon.action;

import com.acromere.xenon.ProgramAction;
import com.acromere.xenon.Xenon;
import com.acromere.xenon.resource.type.ProgramNoticeType;
import javafx.event.ActionEvent;

public class NoticeAction extends ProgramAction {

	public NoticeAction( Xenon program ) {
		super( program );
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void handle( ActionEvent event ) {
		// Open the notice tool
		getProgram().getResourceManager().openAsset( ProgramNoticeType.URI );
	}

}
