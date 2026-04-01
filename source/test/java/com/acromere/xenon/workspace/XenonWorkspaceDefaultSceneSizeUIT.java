package com.acromere.xenon.workspace;

import com.acromere.xenon.Ui;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class XenonWorkspaceDefaultSceneSizeUIT extends ProgramWorkspaceUIT {

	@Test
	void execute() {
		Stage stage = getProgram().getWorkspaceManager().getActiveStage();
		assertThat( stage.isShowing() ).isTrue();
		assertThat( stage.getScene().getWidth() ).isEqualTo( Ui.DEFAULT_WIDTH );
		assertThat( stage.getScene().getHeight() ).isEqualTo( Ui.DEFAULT_HEIGHT );
	}

}
