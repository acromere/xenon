package com.acromere.xenon.tool;

import com.acromere.xenon.resource.Resource;
import com.acromere.xenon.resource.type.ProgramAboutType;
import com.acromere.xenon.resource.type.ProgramGuideType;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AboutToolGetRequiredResourcesUIT extends AboutToolUIT {

	@Test
	void execute() {
		Resource resource = new Resource( ProgramAboutType.URI );
		AboutTool tool = new AboutTool( getProgram(), resource );
		Set<URI> resources = tool.getResourceDependencies();
		assertThat( resources ).contains( ProgramGuideType.URI );
	}

}
