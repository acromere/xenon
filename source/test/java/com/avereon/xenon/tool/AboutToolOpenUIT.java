package com.acromere.xenon.tool;

import com.acromere.xenon.workpane.ToolEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AboutToolOpenUIT extends AboutToolUIT {

	@Test
	void execute() throws Exception {
		assertToolCount( getWorkarea(), 0 );

		openAboutTool();
		getWorkpaneWatcher().waitForEvent( ToolEvent.ADDED );
		getWorkpaneWatcher().waitForEvent( ToolEvent.ADDED );

		assertThat( getWorkarea().getActiveTool() ).isInstanceOf( AboutTool.class );
		assertToolCount( getWorkarea(), 2 );
	}

}
