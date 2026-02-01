package com.acromere.xenon.tool.settings.editor;

import com.acromere.product.Rb;
import com.acromere.settings.SettingsEvent;
import com.acromere.util.DateUtil;
import com.acromere.xenon.RbKey;
import com.acromere.xenon.Xenon;
import com.acromere.xenon.XenonProgramProduct;
import com.acromere.xenon.tool.settings.SettingData;
import com.acromere.xenon.tool.settings.SettingEditor;
import com.acromere.zerra.javafx.Fx;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import lombok.CustomLog;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@CustomLog
public class UpdateSettingViewer extends SettingEditor {

	private Label lastUpdateCheckField;

	private Label nextUpdateCheckField;

	private List<Node> nodes;

	public UpdateSettingViewer( XenonProgramProduct product, String rbKey, SettingData setting ) {
		super( product, rbKey, setting );
	}

	@Override
	public void addComponents( GridPane pane, int row ) {
		Label lastUpdateCheckLabel = new Label( Rb.text( getProduct(), RbKey.UPDATE, "product-update-check-last" ) );
		Label nextUpdateCheckLabel = new Label( Rb.text( getProduct(), RbKey.UPDATE, "product-update-check-next" ) );
		lastUpdateCheckLabel.setId( "product-update-check-last-prompt" );
		nextUpdateCheckLabel.setId( "product-update-check-next-prompt" );
		lastUpdateCheckLabel.getStyleClass().add( "prompt" );
		nextUpdateCheckLabel.getStyleClass().add( "prompt" );

		lastUpdateCheckField = new Label();
		nextUpdateCheckField = new Label();
		lastUpdateCheckField.setId( "product-update-check-last-field" );
		nextUpdateCheckField.setId( "product-update-check-next-field" );

		lastUpdateCheckLabel.setLabelFor( lastUpdateCheckField );
		nextUpdateCheckLabel.setLabelFor( nextUpdateCheckField );

		nodes = List.of( lastUpdateCheckLabel, lastUpdateCheckField, nextUpdateCheckLabel, nextUpdateCheckField );

		Pane spring = new Pane();
		HBox.setHgrow( spring, Priority.ALWAYS );

		HBox container = new HBox();
		container.getChildren().addAll( lastUpdateCheckLabel, lastUpdateCheckField, spring, nextUpdateCheckLabel, nextUpdateCheckField );

		GridPane.setHgrow( container, Priority.ALWAYS );
		GridPane.setColumnSpan( container, row );
		pane.addRow( row, container );

		updateLabels();
	}

	@Override
	public List<Node> getComponents() {
		return nodes;
	}

	@Override
	protected void doSettingValueChanged( SettingsEvent event ) {
		updateLabels();
	}

	@Override
	protected void pageSettingsChanged() {
		updateLabels();
	}

	private void updateLabels() {
		Xenon program = product.getProgram();
		String unknown = Rb.text( getProduct(), RbKey.UPDATE, "unknown" );
		String notScheduled = Rb.text( getProduct(), RbKey.UPDATE, "not-scheduled" );

		Long lastUpdateCheck = program.getProductManager().getLastUpdateCheck();
		Long nextUpdateCheck = program.getProductManager().getNextUpdateCheck();

		final Long finalLastUpdateCheck = lastUpdateCheck;
		final Long finalNextUpdateCheck = nextUpdateCheck;

		// Update the labels
		Fx.run( () -> {
			lastUpdateCheckField.setText( (finalLastUpdateCheck == null ? unknown : DateUtil.format( new Date( finalLastUpdateCheck ), DateUtil.DEFAULT_DATE_FORMAT, TimeZone.getDefault() )) );
			nextUpdateCheckField.setText( (finalNextUpdateCheck == null ? notScheduled : DateUtil.format( new Date( finalNextUpdateCheck ), DateUtil.DEFAULT_DATE_FORMAT, TimeZone.getDefault() )) );
		} );
	}

}
