package com.acromere.xenon.action;

import com.acromere.data.NodeSettings;
import com.acromere.xenon.ProgramAction;
import com.acromere.xenon.PropertiesToolEvent;
import com.acromere.xenon.Xenon;
import com.acromere.xenon.resource.Resource;
import com.acromere.xenon.resource.type.ProgramPropertiesType;
import com.acromere.xenon.task.Task;
import com.acromere.xenon.tool.settings.SettingsPage;
import com.acromere.xenon.workspace.Workspace;
import com.acromere.zerra.javafx.Fx;
import javafx.event.ActionEvent;
import lombok.CustomLog;

@CustomLog
public class PropertiesAction extends ProgramAction {

	public PropertiesAction( Xenon program ) {
		super( program );
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void handle( ActionEvent event ) {
		// Get the settings pages for the resource type
		Resource resource = getProgram().getResourceManager().getCurrentResource();
		SettingsPage page = resource.getType().getSettingsPages().get( "resource" );

		// Set the settings for the pages
		page.setSettings( new NodeSettings( resource.getModel() ) );

		// Switch to a task thread to get the tool
		getProgram().getTaskManager().submit( Task.of( () -> {
			try {
				// Show the properties tool
				getProgram().getResourceManager().openResource( ProgramPropertiesType.URI ).get();

				// Fire the event on the FX thread
				Workspace workspace = getProgram().getWorkspaceManager().getActiveWorkspace();
				Fx.run( () -> workspace.getEventBus().dispatch( new PropertiesToolEvent( PropertiesAction.this, PropertiesToolEvent.SHOW, page ) ) );
			} catch( Exception exception ) {
				log.atError( exception ).log();
			}
		} ) );
	}

}
