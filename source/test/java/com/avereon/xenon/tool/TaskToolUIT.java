package com.acromere.xenon.tool;

import com.acromere.xenon.BaseToolUIT;
import com.acromere.xenon.test.ProgramTestConfig;
import com.acromere.zerra.javafx.Fx;

abstract class TaskToolUIT extends BaseToolUIT {

	void openTaskTool() throws Exception {
		openMenuItem( "#menu-view", "#menu-item-task" );
		Fx.waitForWithExceptions( ProgramTestConfig.LONG_TIMEOUT );
	}

}
