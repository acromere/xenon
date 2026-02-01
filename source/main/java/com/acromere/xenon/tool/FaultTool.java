package com.acromere.xenon.tool;

import com.acromere.event.EventHandler;
import com.acromere.xenon.ProgramEvent;
import com.acromere.xenon.XenonProgramProduct;
import com.acromere.xenon.ProgramTool;
import com.acromere.xenon.resource.Resource;
import com.acromere.xenon.resource.OpenAssetRequest;
import com.acromere.xenon.task.Task;
import javafx.scene.control.TextArea;
import lombok.CustomLog;

import java.io.PrintWriter;
import java.io.StringWriter;

@CustomLog
public class FaultTool extends ProgramTool {

	private final TextArea text;

	private EventHandler<ProgramEvent> closingHandler;

	public FaultTool( XenonProgramProduct product, Resource resource ) {
		super( product, resource );
		setId( "tool-fault" );

		text = new TextArea();
		text.setId( "tool-fault-text" );
		text.setEditable( false );

		getChildren().addAll( text );
	}

	@Override
	protected void ready( OpenAssetRequest request ) {
		setTitle( getResource().getName() );
		setGraphic( getProgram().getIconLibrary().getIcon( "fault" ) );
		// Tasks have to finish before the program exits so this ensures the tool will close
		getProgram().register( ProgramEvent.STOPPING, closingHandler = ( e ) -> getProgram().getTaskManager().submit( Task.of( "", this::close ) ) );
	}

	@Override
	protected void open( OpenAssetRequest request ) {
		Throwable throwable = getResource().getModel();

		if( throwable != null ) {
			StringWriter writer = new StringWriter();
			PrintWriter printer = new PrintWriter( writer );
			throwable.printStackTrace( printer );
			text.setText( writer.toString() );
		}
	}

	@Override
	protected void deallocate() {
		getProgram().unregister( ProgramEvent.STOPPING, closingHandler );
	}

}
