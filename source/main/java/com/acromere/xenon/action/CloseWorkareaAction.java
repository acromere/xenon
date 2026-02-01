package com.acromere.xenon.action;

import com.acromere.product.Rb;
import com.acromere.xenon.ProgramAction;
import com.acromere.xenon.Xenon;
import com.acromere.xenon.ProgramScope;
import com.acromere.zerra.stage.DialogUtil;
import com.acromere.xenon.workspace.Workarea;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.util.Optional;

public class CloseWorkareaAction extends ProgramAction {

	public CloseWorkareaAction( Xenon program ) {
		super( program );
	}

	@Override
	public boolean isEnabled() {
		return getProgram().getWorkspaceManager().getActiveWorkspace().getWorkareas().size() > 1;
	}

	@Override
	public void handle( ActionEvent event ) {
		//Program program = getProgram();
		Workarea workarea = getProgram().getWorkspaceManager().getActiveWorkspace().getActiveWorkarea();

		if( !getProgram().getWorkspaceManager().handleModifiedAssets( ProgramScope.WORKAREA, workarea.getModifiedAssets() ) ) return;

		Alert alert = new Alert( Alert.AlertType.CONFIRMATION );
		alert.setTitle( Rb.text( "workarea", "workarea-close-title" ) );
		alert.setHeaderText( Rb.text( "workarea", "workarea-close-message" ) );
		alert.setContentText( Rb.text( "workarea", "workarea-close-prompt", workarea.getName() ) );

		Stage stage = getProgram().getWorkspaceManager().getActiveStage();
		Optional<ButtonType> result = DialogUtil.showAndWait( stage, alert );

		if( result.isPresent() && result.get() == ButtonType.OK ) closeWorkarea( workarea );
	}

	private void closeWorkarea( Workarea workarea ) {
		getProgram().getWorkspaceManager().getActiveWorkspace().removeWorkarea( workarea );
	}

}
