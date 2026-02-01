package com.acromere.xenon;

import com.acromere.util.Parameters;
import com.acromere.xenon.task.TaskManager;
import com.acromere.xenon.test.ProgramTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.spy;

/**
 * This class is a duplicate of com.acromere.zenna.BaseXenonUiTestCase which is
 * intended to be visible for mod testing but is not available to Xenon to
 * avoid a circular dependency. Attempts at making this
 * class publicly available have run in to various challenges with the most
 * recent being with Surefire not putting JUnit 5 on the module path.
 */
@ExtendWith( MockitoExtension.class )
public abstract class BasePartXenonTestCase extends BaseXenonTestCase {

	protected TaskManager taskManager;

	@BeforeEach
	protected void setup() throws Exception {
		super.setup();

		// Create the program
		Xenon xenon = setProgram( spy( new Xenon() ) );
		xenon.setProgramParameters( Parameters.parse( ProgramTestConfig.getParameters() ) );
		xenon.init();

		this.taskManager = xenon.getTaskManager();
	}

}
