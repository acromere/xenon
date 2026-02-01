package com.acromere.xenon.tool.settings.panel.products;

import com.acromere.product.ProductCard;
import com.acromere.product.ProductCardComparator;
import com.acromere.xenon.product.ProgramProductCardComparator;
import com.acromere.xenon.task.Task;
import com.acromere.xenon.task.TaskManager;
import com.acromere.xenon.tool.settings.panel.ModulesUpdatesSettingsPanel;
import com.acromere.zerra.javafx.Fx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RefreshModuleUpdates extends Task<Void> {

	private final ModulesUpdatesSettingsPanel parent;

	private final boolean force;

	public RefreshModuleUpdates( ModulesUpdatesSettingsPanel parent, boolean force ) {
		this.parent = parent;
		this.force = force;
	}

	@Override
	public Void call() {
		TaskManager.taskThreadCheck();
		Fx.run( parent::showUpdating );
		List<ProductCard> cards = new ArrayList<>( parent.getProgram().getProductManager().findAvailableUpdates( force ) );
		cards.sort( new ProgramProductCardComparator( parent.getProgram(), ProductCardComparator.Field.NAME ) );

		//productTool.getUpdatesPage().reloadProducts( cards );
		Map<String, ProductCard> installedProducts = new HashMap<>();
		Map<String, ProductCard> productUpdates = new HashMap<>();

		// Installed product map
		for( ProductCard card : parent.getProgram().getProductManager().getInstalledProductCards( false ) ) {
			installedProducts.put( card.getProductKey(), card );
		}

		// Product update map
		for( ProductCard card : cards ) {
			productUpdates.put( card.getProductKey(), card );
		}

		// Installed product list
		List<ProductCard> newCards = new ArrayList<>();
		for( ProductCard card : cards ) {
			newCards.add( installedProducts.get( card.getProductKey() ) );
		}

		Fx.run( () -> parent.setProducts( newCards, productUpdates ) );

		return null;
	}

}
