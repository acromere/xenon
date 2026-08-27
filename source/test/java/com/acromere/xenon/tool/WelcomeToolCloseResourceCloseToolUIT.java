package com.acromere.xenon.tool;

import com.acromere.xenon.ProgramTool;
import com.acromere.xenon.resource.type.ProgramWelcomeType;
import com.acromere.xenon.workpane.ToolEvent;
import com.acromere.xenon.workpane.Workpane;
import com.acromere.zerra.javafx.Fx;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Future;

import static com.acromere.xenon.test.ProgramTestConfig.LONG_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;

class WelcomeToolCloseResourceCloseToolUIT extends WelcomeToolUIT {

	@Test
	void execute() throws Exception {
		Workpane pane = getWorkarea();
		assertToolCount( pane, 0 );

		Future<ProgramTool> future = getProgram().getResourceManager().openResource( ProgramWelcomeType.URI );
		getWorkpaneWatcher().waitForEvent( ToolEvent.ADDED );
		Fx.waitFor( LONG_TIMEOUT );
		assertThat( pane.getActiveTool() ).isInstanceOf( WelcomeTool.class );
		assertThat( pane.getActiveView().isMaximized() ).isFalse();
		assertToolCount( pane, 1 );

		getProgram().getResourceManager().closeResources( future.get().getResource() );
		getWorkpaneWatcher().waitForEvent( ToolEvent.REMOVED );
		Fx.waitFor( LONG_TIMEOUT );
		assertThat( pane.getMaximizedView() ).isNull();
		assertToolCount( pane, 0 );
	}

}
