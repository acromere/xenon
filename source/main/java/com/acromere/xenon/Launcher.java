package com.acromere.xenon;

import com.acromere.weave.ElevatedFlag;
import com.acromere.weave.WeaveFlag;
import com.acromere.weave.Weave;

/**
 * The Launcher class is the entry point for the application. The Launcher is
 * also responsible for determining if the application should be updated or
 * if the application should be launched normally, based on the command line.
 */
public class Launcher {

	public static void main( String[] commands ) {
		XenonLauncherConfig.setCustomLauncherSystemProperty();

		com.acromere.util.Parameters parameters = com.acromere.util.Parameters.parse( commands );
		boolean callback = parameters.isSet( ElevatedFlag.CALLBACK_SECRET );
		boolean update = parameters.isSet( WeaveFlag.UPDATE );
		boolean updating = update || callback;

		if( updating ) {
			Weave.launch( commands );
		} else {
			Xenon.launch( commands );
		}
	}

}
