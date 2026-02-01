package com.acromere.xenon;

import com.acromere.xenon.workpane.Tool;
import com.acromere.xenon.workpane.ToolEvent;
import com.acromere.xenon.workpane.Workpane;
import javafx.event.EventType;

public class ProgramToolEvent extends ToolEvent {

	public static final EventType<ProgramToolEvent> TOOL = new EventType<>( ToolEvent.ANY, "TOOL" );

	public static final EventType<ProgramToolEvent> ANY = TOOL;

	public static final EventType<ProgramToolEvent> READY = new EventType<>( TOOL, "READY" );

	public static final EventType<ProgramToolEvent> TOOL_OPEN_REQUEST_FINISHED = new EventType<>( TOOL, "OPEN" );

	public ProgramToolEvent( Object source, EventType<? extends ToolEvent> type, Workpane workpane, Tool tool ) {
		super( source, type, workpane, tool );
	}

}
