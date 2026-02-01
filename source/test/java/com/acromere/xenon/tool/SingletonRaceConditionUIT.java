package com.acromere.xenon.tool;

import com.acromere.xenon.BaseToolUIT;
import com.acromere.xenon.resource.type.ProgramGuideType;
import com.acromere.xenon.tool.guide.GuideTool;
import com.acromere.xenon.workpane.ToolEvent;
import com.acromere.xenon.workpane.Workpane;
import com.acromere.zerra.javafx.Fx;
import org.junit.jupiter.api.Test;

import static com.acromere.xenon.test.ProgramTestConfig.LONG_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;

class SingletonRaceConditionUIT extends BaseToolUIT {

	@Test
	void testOpenToolRaceCondition() throws Exception {
		getProgram().getResourceManager().openAsset( ProgramGuideType.URI, true, false );
		getProgram().getResourceManager().openAsset( ProgramGuideType.URI, true, false );

		getWorkpaneWatcher().waitForEvent( ToolEvent.ADDED );
		Fx.waitForWithExceptions( LONG_TIMEOUT );
		Workpane pane = getWorkarea();

		assertThat( pane.getTools( GuideTool.class ) ).hasSize( 1 );
	}

}
