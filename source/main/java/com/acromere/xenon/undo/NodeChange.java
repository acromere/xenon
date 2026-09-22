package com.acromere.xenon.undo;

import com.acromere.data.DataNode;
import lombok.CustomLog;
import lombok.Getter;

import java.util.List;
import java.util.Objects;

@CustomLog
public class NodeChange {

	private static final String CAPTURE_UNDO_CHANGES = NodeChange.class.getName() + ":capture-undo-changes";

	private static final boolean DEFAULT_CAPTURE_UNDO_CHANGES = false;

	@Getter
	private final DataNode node;

	@Getter
	private final String key;

	@Getter
	private final Object oldValue;

	@Getter
	private final Object newValue;

	@Getter
	private final boolean redo;

	@Getter
	private final List<NodeChange> changes;

	public NodeChange( DataNode node, String key, Object oldValue, Object newValue ) {
		this( node, key, oldValue, newValue, false );
	}

	public NodeChange( DataNode node, String key, Object oldValue, Object newValue, boolean isRedo ) {
		this.node = node;
		this.key = key;
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.redo = isRedo;
		this.changes = List.of( this );
	}

	public String toString() {
		return node + " " + key + "=" + oldValue + " -> " + newValue;
	}

	@Override
	public boolean equals( Object other ) {
		if( !(other instanceof NodeChange that) ) return false;
		if( this == that ) return true;
		if( this.getClass() != other.getClass() ) return false;
		return Objects.equals( this.node, that.node ) && Objects.equals( this.key, that.key ) && Objects.equals( oldValue, that.oldValue ) && Objects.equals( newValue, that.newValue );
	}

	@Override
	public int hashCode() {
		if( this.node != null ) return Objects.hash( node, key, oldValue, newValue );
		return super.hashCode();
	}

	public static boolean isCaptureUndoChanges( DataNode node ) {
		return node.getValue( CAPTURE_UNDO_CHANGES, DEFAULT_CAPTURE_UNDO_CHANGES );
	}

	public static void setCaptureUndoChanges( DataNode node, boolean capture ) {
		node.setValue( NodeChange.CAPTURE_UNDO_CHANGES, capture );
	}

}
