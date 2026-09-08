package com.acromere.xenon.tool;

import com.acromere.xenon.XenonProgramProduct;
import com.acromere.xenon.resource.OpenResourceRequest;
import com.acromere.xenon.resource.Resource;
import com.acromere.xenon.tool.guide.GuidedTool;
import com.acromere.xenon.workpane.ToolException;
import com.acromere.zerra.color.Paints;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.web.WebView;
import lombok.CustomLog;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@CustomLog
public class HelpTool extends GuidedTool {

	// Consider using JPro Markdown https://github.com/JPro-one/jpro-platform#jpro-markdown

	private final Label label;

	private final WebView web;

	public HelpTool( XenonProgramProduct product, Resource resource ) {
		super( product, resource );

		setGraphic( product.getProgram().getIconLibrary().getIcon( "help" ) );

		// The CSS takes care of the transparent background
		String cssUrl = getClass().getResource("/xenon-help.css").toExternalForm();

		// Needed to keep track of the style text fill paint
		label = new Label( "Label" );
		label.setVisible( false );

		// The help content view
		web = new WebView();
		web.setPageFill( Color.TRANSPARENT );
		web.getEngine().setUserStyleSheetLocation(cssUrl);
		web.getEngine().loadContent( "<body>Web Content</body>" );

		// Add listeners
		label.textFillProperty().addListener( (p,o,n)-> updateBodyTextColor( web.getEngine().getDocument(), n ));
		web.getEngine().titleProperty().addListener( ( p, o, n ) -> setTitle( n ) );
		web.getEngine().documentProperty().addListener( ( p, o, n ) -> updateBodyTextColor( n, label.getTextFill() ) );

		getChildren().addAll( label, web );
	}

	@Override
	protected void ready( OpenResourceRequest request ) throws ToolException {
		String content = request.getResource().getModel();
		web.getEngine().loadContent( content );
	}

	private void updateBodyTextColor( org.w3c.dom.Document n, Paint color ) {
		if( n == null ) return;

		NodeList bodyList = n.getElementsByTagName( "body" );
		int count = bodyList.getLength();
		for( int index = 0; index < count; index++ ) {
			org.w3c.dom.Node node = bodyList.item( index );
			if( node instanceof Element element ) {
				element.setAttribute( "style", "color: " + Paints.toString( color ) );
			}
		}
	}

}
