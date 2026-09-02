package com.acromere.xenon.workspace;

import com.acromere.xenon.BaseFullXenonTestCase;
import com.acromere.zerra.javafx.Fx;
import javafx.scene.paint.Color;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class WorkspaceTest extends BaseFullXenonTestCase {

	@Setter
	private Workspace workspace;

	@BeforeEach
	@Override
	protected void setup() throws Exception {
		super.setup();
		Fx.startup();
		Fx.run( () -> setWorkspace( new Workspace( getProgram() ) ) );
		Fx.waitForStability( 1000 );
	}

	@Test
	void pushPullMenuActions_areGuarded_andDoNotThrow() {
		assertThatCode( () -> Fx.call( () -> {
			// Ensure pulling when nothing is present is safe
			workspace.pullMenuActions();

			// Pushing an empty descriptor should be a no-op and not throw
			workspace.pushMenuActions( "" );

			// Pull again; should not throw regardless of internal marker state
			workspace.pullMenuActions();


			// Push a descriptor with known program actions; should not throw
			workspace.pushMenuActions( "maximize|minimize" );

			// Final pull cleanup; should be safe
			workspace.pullMenuActions();
			return null;
		} ) ).doesNotThrowAnyException();
	}

	@Test
	void pushPullToolbarActions_areGuarded_andDoNotThrow() {
		assertThatCode( () -> Fx.call( () -> {
			// Ensure pulling when nothing is present is safe
			workspace.pullToolbarActions();

			// Pushing an empty descriptor should be a no-op and not throw
			workspace.pushToolbarActions( "" );

			// Pull again; should not throw regardless of internal marker state
			workspace.pullToolbarActions();

			// Push a descriptor with known program actions; should not throw
			workspace.pushToolbarActions( "maximize|minimize" );

			// Final pull cleanup; should be safe
			workspace.pullToolbarActions();
			return null;
		} ) ).doesNotThrowAnyException();
	}

	@Test
	void switchingActiveWorkarea_rebindsTitle_safely_andSetsTitleFromActiveWorkarea() throws Exception {
		Workarea w1 = new Workarea();
		w1.setName( "One" );
		w1.setColor( Color.RED );

		Workarea w2 = new Workarea();
		w2.setName( "Two" );
		w2.setColor( Color.BLUE );

		// Add both workareas to the workspace
		workspace.addWorkarea( w1 );
		workspace.addWorkarea( w2 );

		assertThat( workspace.titleProperty().isBound() ).isFalse();

		// First switch binds the title to w1 via the active workarea listener
		Fx.call( () -> {
			workspace.setActiveWorkarea( w1 );
			return null;
		} );
		Fx.waitFor( 200 );
		assertThat( workspace.titleProperty().isBound() )
			.as( "Workspace title should be bound after first switch" )
			.isTrue();

		// Second switch would attempt a re-bind; with proper unbinds this must not throw
		assertThatCode( () -> Fx.call( () -> {
			workspace.setActiveWorkarea( w2 );
			return null;
		} ) ).doesNotThrowAnyException();

		// Give FX a moment to update the bound title
		Fx.waitFor( 200 );

		// Title should reflect the active workarea name
		String expectedTitle = w2.getName() + " - " + getProgram().getCard().getName();
		assertThat( workspace.getTitle() ).isEqualTo( expectedTitle );
	}

	@Test
	void uid_isGeneratedAndStable_whenConstructedWithNull() throws Exception {
		// Workspace in setup() is constructed with null id
		String uid1 = Fx.call( () -> workspace.getUid() );
		String uid2 = Fx.call( () -> workspace.getUid() );

		assertThat( uid1 ).as( "UID should be generated" ).isNotNull();
		assertThat( uid1 ).as( "UID should not be blank" ).isNotBlank();
		assertThat( uid2 ).as( "UID should be stable across calls" ).isEqualTo( uid1 );

		// Also verify creating an explicit new workspace yields a non-null UID
		Workspace ws2 = Fx.call( () -> new Workspace( getProgram() ) );
		String uid3 = Fx.call( ws2::getUid );
		assertThat( uid3 ).isNotNull().isNotBlank();
	}

}
