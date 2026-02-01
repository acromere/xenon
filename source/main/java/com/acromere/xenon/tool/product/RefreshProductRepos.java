package com.acromere.xenon.tool.product;

import com.acromere.xenon.product.RepoState;
import com.acromere.xenon.product.RepoStateComparator;
import com.acromere.xenon.task.Task;
import com.acromere.zerra.javafx.Fx;

import java.util.ArrayList;
import java.util.List;

@Deprecated
class RefreshProductRepos extends Task<Void> {

	private ProductTool productTool;

	private boolean force;

	RefreshProductRepos( ProductTool productTool, boolean force ) {
		this.productTool = productTool;
		this.force = force;
	}

	@Override
	public Void call() {
		Fx.run( () -> productTool.getRepoPage().showUpdating() );
		List<RepoState> cards = new ArrayList<>( productTool.getProgram().getProductManager().getRepos() );
		cards.sort( new RepoStateComparator( RepoStateComparator.Field.NAME ) );
		cards.sort( new RepoStateComparator( RepoStateComparator.Field.RANK ) );
		Fx.run( () -> productTool.getRepoPage().setRepos( cards ) );
		return null;
	}

}
