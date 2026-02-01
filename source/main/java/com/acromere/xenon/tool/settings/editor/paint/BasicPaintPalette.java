package com.acromere.xenon.tool.settings.editor.paint;

import com.acromere.product.Rb;
import com.acromere.xenon.RbKey;
import javafx.scene.paint.Color;

import java.util.List;

public class BasicPaintPalette extends BasePaintPalette {

	public BasicPaintPalette() {
		super( Rb.text( RbKey.LABEL, "palette-basic" ), List.of( Color.GRAY, Color.RED, Color.GREEN, Color.YELLOW, Color.BLUE, Color.MAGENTA, Color.CYAN ) );
	}

}
