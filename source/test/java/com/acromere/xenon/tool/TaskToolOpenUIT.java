package com.acromere.xenon.tool;

import com.acromere.xenon.workpane.ToolEvent;
import com.acromere.xenon.workpane.Workpane;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskToolOpenUIT extends TaskToolUIT {

	@Test
	void execute() throws Exception {
		Workpane pane = getWorkarea();
		assertToolCount( pane, 0 );

		openTaskTool();
		getWorkpaneWatcher().waitForEvent( ToolEvent.ADDED );

		assertThat( pane.getActiveTool() ).isInstanceOf( TaskTool.class );
		assertToolCount( pane, 1 );
	}

}
