package com.acromere.xenon.tool.settings;

import com.acromere.xenon.ProgramTool;
import com.acromere.xenon.resource.type.ProgramSettingsType;
import com.acromere.xenon.workpane.ToolEvent;
import com.acromere.xenon.workspace.Workarea;
import com.acromere.zerra.javafx.Fx;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Future;

import static com.acromere.xenon.test.ProgramTestConfig.LONG_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;

class SettingsToolCloseResourceCloseToolUIT extends SettingsToolUIT {

	@Test
	void execute() throws Exception {
		Workarea area = getProgram().getWorkspaceManager().getActiveWorkspace().getActiveWorkarea();
		assertToolCount( area, 0 );

		Future<ProgramTool> future = getProgram().getResourceManager().openAsset( ProgramSettingsType.URI );
		getWorkpaneWatcher().waitForEvent( ToolEvent.ADDED );
		getWorkpaneWatcher().waitForEvent( ToolEvent.ADDED );
		Fx.waitForWithExceptions( LONG_TIMEOUT );
		assertThat( area.getActiveTool() ).isInstanceOf( SettingsTool.class );
		assertToolCount( area, 2 );

		getProgram().getResourceManager().closeAssets( future.get().getResource() );
		getWorkpaneWatcher().waitForEvent( ToolEvent.REMOVED );
		Fx.waitForWithExceptions( LONG_TIMEOUT );
		assertToolCount( area, 1 );
	}

}
