package com.acromere.xenon.throwable;

import com.acromere.log.LazyEval;
import lombok.CustomLog;

@CustomLog
public class ProgramUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

	@Override
	public void uncaughtException( Thread thread, Throwable throwable ) {
		log.atError( throwable ).log( "Uncaught exception on %s thread", LazyEval.of( thread::getName ) );
	}

}
