package com.acromere.xenon.tool.guide;

import com.acromere.xenon.resource.type.ProgramGuideType;
import com.acromere.xenon.workpane.ToolEvent;
import com.acromere.xenon.workpane.Workpane;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GuideToolOpenUIT extends GuideToolUIT {

	@Test
	void execute() throws Exception {
		Workpane pane = getWorkarea();
		assertToolCount( pane, 0 );

		getProgram().getResourceManager().openAsset( ProgramGuideType.URI );
		getWorkpaneWatcher().waitForEvent( ToolEvent.ADDED );
		assertThat( pane.getActiveTool() ).isInstanceOf( GuideTool.class );
		assertToolCount( pane, 1 );
	}

}
