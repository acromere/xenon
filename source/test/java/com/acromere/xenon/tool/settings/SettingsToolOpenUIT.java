package com.acromere.xenon.tool.settings;

import com.acromere.xenon.workpane.ToolEvent;
import com.acromere.xenon.workpane.Workpane;
import com.acromere.zerra.javafx.Fx;
import org.junit.jupiter.api.Test;

import static com.acromere.xenon.test.ProgramTestConfig.LONG_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;

class SettingsToolOpenUIT extends SettingsToolUIT {

	@Test
	void execute() throws Exception {
		Workpane pane = getWorkarea();
		assertToolCount( pane, 0 );

		openSettingsTool();
		getWorkpaneWatcher().waitForEvent( ToolEvent.ADDED );
		getWorkpaneWatcher().waitForEvent( ToolEvent.ADDED );
		Fx.waitForWithExceptions( LONG_TIMEOUT );
		assertThat( pane.getActiveTool() ).isInstanceOf( SettingsTool.class );
		assertToolCount( pane, 2 );
	}

}
