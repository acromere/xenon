package com.acromere.xenon.tool.settings;

import com.acromere.xenon.BaseToolUIT;
import com.acromere.xenon.test.ProgramTestConfig;
import com.acromere.zerra.javafx.Fx;

abstract class SettingsToolUIT extends BaseToolUIT {

	void openSettingsTool() throws Exception {
		openMenuItem( "#menu-file", "#menu-item-settings" );
		Fx.waitForWithExceptions( ProgramTestConfig.LONG_TIMEOUT );
	}

}
