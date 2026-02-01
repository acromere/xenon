package com.acromere.xenon.tool.product;

import com.acromere.xenon.BaseToolUIT;
import com.acromere.xenon.test.ProgramTestConfig;
import com.acromere.zerra.javafx.Fx;

abstract class ProductToolUIT extends BaseToolUIT {

	void openProductTool() throws Exception {
		openMenuItem( "#menu-view", "#menu-item-product" );
		Fx.waitForWithExceptions( ProgramTestConfig.LONG_TIMEOUT );
	}

}
