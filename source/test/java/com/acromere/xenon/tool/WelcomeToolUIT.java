package com.acromere.xenon.tool;

import com.acromere.xenon.BaseToolUIT;
import com.acromere.xenon.test.ProgramTestConfig;
import com.acromere.zerra.javafx.Fx;

abstract class WelcomeToolUIT extends BaseToolUIT {

	protected void openWelcomeTool() throws Exception {
		openMenuItem( "#menu-help", "#menu-item-welcome" );
		Fx.waitFor( ProgramTestConfig.LONG_TIMEOUT );
	}

}
