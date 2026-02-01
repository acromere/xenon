package com.acromere.xenon.action.common;

import com.acromere.skill.Resettable;
import com.acromere.xenon.ProgramAction;
import com.acromere.xenon.Xenon;
import javafx.event.ActionEvent;

public class ResetAction extends ProgramAction {

	private final Resettable resettable;

	public ResetAction( Xenon program, Resettable resettable ) {
		super( program );
		this.resettable = resettable;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void handle( ActionEvent actionEvent ) {
		resettable.reset();
	}

}
