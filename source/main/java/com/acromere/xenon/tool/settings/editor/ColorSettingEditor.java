package com.acromere.xenon.tool.settings.editor;

import com.acromere.product.Rb;
import com.acromere.settings.SettingsEvent;
import com.acromere.xenon.XenonProgramProduct;
import com.acromere.xenon.tool.settings.SettingData;
import com.acromere.xenon.tool.settings.SettingEditor;
import com.acromere.zerra.color.Colors;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import java.util.List;

public class ColorSettingEditor extends SettingEditor {

	private Label label;

	private ColorPicker colorPicker;

	private List<Node> nodes;

	public ColorSettingEditor( XenonProgramProduct product, String rbKey, SettingData setting ) {
		super( product, rbKey, setting );
	}

	@Override
	public void addComponents( GridPane pane, int row ) {
		String rbKey = setting.getRbKey();
		String value = setting.getSettings().get( getKey(), "#000000ff" );

		label = new Label( Rb.text( getProduct(), getRbKey(), rbKey ) );
		label.setMinWidth( Region.USE_PREF_SIZE );

		colorPicker = new ColorPicker();
		colorPicker.setValue( Colors.parse( value ) );
		colorPicker.setId( rbKey );
		colorPicker.setMaxWidth( Double.MAX_VALUE );

		nodes = List.of( label, colorPicker );

		// Add the event handlers
		colorPicker.setOnAction( this::doPickerValueChanged );

		// Set component state
		setDisable( setting.isDisable() );
		setVisible( setting.isVisible() );

		// Add the components
		pane.addRow( row, label, colorPicker );
	}

	@Override
	public List<Node> getComponents() {
		return nodes;
	}

	@Override
	protected void doSettingValueChanged( SettingsEvent event ) {
		Object value = event.getNewValue();
		Color color;
		try {
			color = Colors.parse( String.valueOf( value ) );
		} catch( Exception exception ) {
			color = Color.BLACK;
		}
		if( event.getEventType() == SettingsEvent.CHANGED && getKey().equals( event.getKey() ) ) colorPicker.setValue( color );
	}

	@Override
	protected void pageSettingsChanged() {
		Color color;
		try {
			color = Colors.parse( getCurrentValue() );
		} catch( Exception exception ) {
			color = Color.BLACK;
		}
		colorPicker.setValue( color );
	}

	private void doPickerValueChanged( ActionEvent event ) {
		setting.getSettings().set( setting.getKey(), Colors.toString( colorPicker.getValue() ) );
	}

}
