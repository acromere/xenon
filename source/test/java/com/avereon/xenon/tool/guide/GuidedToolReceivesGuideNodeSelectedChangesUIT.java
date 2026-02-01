package com.acromere.xenon.tool.guide;

import com.acromere.zerra.javafx.Fx;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.acromere.xenon.test.ProgramTestConfig.LONG_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;

public class GuidedToolReceivesGuideNodeSelectedChangesUIT extends GuidedToolUIT {

	@Test
	void execute() throws Exception {
		// Assert initial state
		assertThat( mockGuidedTool.getSelectedNodes().size() ).isEqualTo( 0 );

		Fx.run( () -> mockGuidedTool.getGuideContext().setSelectedIds( Set.of( "general" ) ) );
		Fx.waitForWithExceptions( LONG_TIMEOUT );
		assertThat( mockGuidedTool.getSelectedNodes() ).contains( mockGuidedTool.getCurrentGuide().getNode( "general" ) );
	}

}
