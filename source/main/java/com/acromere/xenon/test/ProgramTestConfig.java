package com.acromere.xenon.test;

import com.acromere.xenon.XenonFlag;
import com.acromere.xenon.XenonMode;
import com.acromere.xenon.XenonTestFlag;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public final class ProgramTestConfig {

	/**
	 * The wait timeout for many operations. Common values are:
	 * <pre>
	 * 5000 - GitHub Actions, Mintbox Mini
	 *  | Slower computers
	 *  |
	 *  | Faster computers
	 * 1000 - AMD Threadripper, Intel i9</pre>
	 */
	public static final int TIMEOUT = 5000;

	/**
	 * The long wait timeout for many operations. Common values are:
	 * <pre>
	 * 50000 - GitHub Actions, Mintbox Mini
	 *  | Slower computers
	 *  |
	 *  | Faster computers
	 * 10000 - AMD Threadripper, Intel i9</pre>
	 */
	public static final int LONG_TIMEOUT = 10 * TIMEOUT;

	@Getter
	@Setter
	private static List<String> parameters;

	static {
		List<String> values = new ArrayList<>();
		values.add( XenonFlag.RESET );
		values.add( XenonFlag.NO_SPLASH );
		values.add( XenonFlag.NO_UPDATES );
		values.add( XenonFlag.MODE );
		values.add( XenonMode.TEST );
		values.add( XenonFlag.LOG_LEVEL );
		values.add( XenonFlag.WARN );
		values.add( XenonTestFlag.EMPTY_WORKSPACE );
		parameters = values;
	}

	public static void addFlag(String flag) {
		parameters.add( flag );
	}

	public static void removeFlag( String flag ) {
		parameters.remove( flag );
	}

}
