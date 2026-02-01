package com.acromere.xenon.tool.settings.panel.products;

import com.acromere.xenon.Xenon;
import com.acromere.xenon.XenonProgramProduct;
import com.acromere.xenon.product.ProductManager;
import javafx.scene.layout.GridPane;
import lombok.Getter;

@Getter
public class BaseTile extends GridPane {

	private final Xenon program;

	private final ProductsSettingsPanel productSettingsPanel;

	private final ProductManager productManager;

	public BaseTile( XenonProgramProduct product, ProductsSettingsPanel productSettingsPanel ) {
		this.program = product.getProgram();
		this.productSettingsPanel = productSettingsPanel;
		this.productManager = program.getProductManager();
	}

	public void updateTileState() {
		// Do nothing
	}

}
