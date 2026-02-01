package com.acromere.xenon;

import java.lang.management.ManagementFactory;

public class StartupTimeCheck {

	private static final long programStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();

	public static void main( String[] parameters ) {
		System.err.println( System.currentTimeMillis() - programStartTime );
	}

}
