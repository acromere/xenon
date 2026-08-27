package com.acromere.xenon;

import com.acromere.xenon.resource.OpenResourceRequest;
import com.acromere.xenon.resource.Resource;
import com.acromere.xenon.resource.type.ProgramAboutType;
import com.acromere.xenon.tool.AboutTool;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class ToolManagerUIT extends BaseFullXenonTestCase {

	@Test
	void testOpenDependencies() throws Exception {
		// given
		Resource resource = getProgram().getResourceManager().createResource( ProgramAboutType.URI );
		ProgramTool tool = new AboutTool( getProgram(), resource );
		OpenResourceRequest request = new OpenResourceRequest();

		// when
		boolean result = getProgram().getToolManager().openDependencies( request, tool );

		// then
		assertThat( result ).isTrue();
	}

	@Test
	void testOpenDependenciesReturnFalseOnException() throws Exception {
		// given
		Resource resource = getProgram().getResourceManager().createResource( ProgramAboutType.URI );
		ProgramTool tool = new MockProgramTool( getProgram(), resource );
		tool.getResourceDependencies().add( URI.create( "mock:///not-really-an-resource" ) );
		OpenResourceRequest request = new OpenResourceRequest();

		// when
		boolean result = getProgram().getToolManager().openDependencies( request, tool );

		// then
		assertThat( result ).isFalse();
	}

}
