package com.acromere.xenon.tool.guide;

import com.acromere.xenon.resource.Resource;
import com.acromere.xenon.resource.type.ProgramGuideType;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GuideToolGetRequiredResourcesUIT extends GuideToolUIT {

	@Test
	void execute() {
		Resource resource = new Resource( ProgramGuideType.URI );
		GuideTool tool = new GuideTool( getProgram(), resource );

		Set<URI> resources = tool.getResourceDependencies();
		assertThat( resources.size() ).isEqualTo( 0 );
	}

}
