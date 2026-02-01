package com.acromere.xenon.tool.product;

import com.acromere.xenon.resource.Resource;
import com.acromere.xenon.resource.type.ProgramGuideType;
import com.acromere.xenon.resource.type.ProgramModuleType;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProductToolGetRequiredAssetsUIT extends ProductToolUIT {

	@Test
	void execute() {
		Resource resource = new Resource( ProgramModuleType.URI );
		ProductTool tool = new ProductTool( getProgram(), resource );
		Set<URI> assets = tool.getAssetDependencies();
		assertThat( assets ).contains( ProgramGuideType.URI );
	}

}
