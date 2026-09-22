package com.acromere.xenon.undo;

import com.acromere.data.DataNode;
import com.acromere.data.DataNodeEvent;
import com.acromere.transaction.Txn;
import com.acromere.transaction.TxnEvent;
import lombok.CustomLog;
import org.fxmisc.undo.UndoManager;
import org.fxmisc.undo.UndoManagerFactory;
import org.reactfx.EventSource;
import org.reactfx.EventStream;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@CustomLog
public class DataNodeUndoProvider {

	private static final String UNDO_CHANGES = DataNodeUndoProvider.class.getName() + ":undo-changes";

	public static UndoManager<List<NodeChange>> manager( DataNode node ) {
		return UndoManagerFactory.unlimitedHistorySingleChangeUM( events( node ), DataNodeUndoProvider::invert, DataNodeUndoProvider::apply );
	}

	private static EventStream<List<NodeChange>> events( DataNode node ) {
		EventSource<List<NodeChange>> events = new EventSource<>();

		final LinkedList<NodeChange> changes = new LinkedList<>();
		node.setValue( UNDO_CHANGES, changes );

		node.register(
			DataNodeEvent.VALUE_CHANGED, e -> {
				DataNode eventNode = e.getNode();
				String eventKey = e.getKey();
				boolean isModifying = eventNode.isModifyingKey( eventKey );
				boolean isCaptureUndoChanges = NodeChange.isCaptureUndoChanges( node );

				if( isModifying && isCaptureUndoChanges ) {
					synchronized( changes ) {
						changes.add( new NodeChange( eventNode, eventKey, e.getOldValue(), e.getNewValue() ) );
					}
				}
			}
		);

		node.register(
			TxnEvent.COMMIT_END, _ -> {
				if( !changes.isEmpty()) {
					synchronized( changes ) {
						events.push( new ArrayList<>( changes ) );
						changes.clear();
					}
				}
			}
		);

		return events;
	}

	private static List<NodeChange> invert( List<NodeChange> changes ) {
		return changes.stream().map( DataNodeUndoProvider::invert ).collect( Collectors.toList() );
	}

	private static NodeChange invert( NodeChange change ) {
		return new NodeChange( change.getNode(), change.getKey(), change.getNewValue(), change.getOldValue() );
	}

	private static void apply( List<NodeChange> changes ) {
		Txn.run( () -> changes.forEach( c -> c.getNode().setValue( c.getKey(), c.getNewValue() ) ) );
	}

}
