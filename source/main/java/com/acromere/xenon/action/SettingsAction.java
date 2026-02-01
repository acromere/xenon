package com.acromere.xenon.action;

import com.acromere.xenon.ProgramAction;
import com.acromere.xenon.Xenon;
import com.acromere.xenon.resource.type.ProgramSettingsType;
import javafx.event.ActionEvent;

import java.net.URI;

public class SettingsAction extends ProgramAction {

	private URI uri = ProgramSettingsType.URI;

	public SettingsAction( Xenon program ) {
		this( program, null );
	}

	public SettingsAction( Xenon program, String fragment ) {
		super( program );
		if( fragment != null ) uri = ProgramSettingsType.URI.resolve( "#" + fragment );
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void handle( ActionEvent event ) {
		getProgram().getResourceManager().openAsset( uri );
	}

}
