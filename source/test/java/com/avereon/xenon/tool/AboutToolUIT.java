package com.acromere.xenon.tool;

import com.acromere.xenon.BaseToolUIT;
import com.acromere.xenon.test.ProgramTestConfig;
import com.acromere.zerra.javafx.Fx;

abstract class AboutToolUIT extends BaseToolUIT {

	void openAboutTool() throws Exception {
		openMenuItem( "#menu-help", "#menu-item-about" );
		Fx.waitForWithExceptions( ProgramTestConfig.LONG_TIMEOUT );
	}

}
