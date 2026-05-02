package com.acromere.xenon;

public interface Rebrandable {

	/**
	 * Called when the program is started to allow the program to register
	 * resources used to rebrand itself.
	 *
	 * @param program the XenonProgram instance being started
	 */
	void register( XenonProgram program );

	/**
	 * Called when the program is stopped to allow the program to unregister
	 * resources used to rebrand itself.
	 *
	 * @param program the XenonProgram instance being stopped
	 */
	void unregister( XenonProgram program );
}
