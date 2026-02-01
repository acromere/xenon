package com.acromere.xenon;

import com.acromere.product.ProgramProduct;
import com.acromere.xenon.task.Task;

import java.util.concurrent.Callable;

public interface XenonProgramProduct extends ProgramProduct {

	Xenon getProgram();

	default void task( String name, Runnable runnable ) {
		getProgram().getTaskManager().submit( Task.of( name, runnable ) );
	}

	default <T> void task( String name, Callable<T> callable ) {
		getProgram().getTaskManager().submit( Task.of( name, callable ) );
	}

}
