package com.acromere.xenon;

import com.acromere.product.ProductCard;
import com.acromere.product.Rebrand;
import com.acromere.zenna.icon.XRingLargeIcon;
import com.acromere.zerra.javafx.Fx;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import javafx.stage.Stage;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;

@CustomLog
public class SplashScreenPane extends Pane {

	private static final double WIDTH = 640;

	private static final double HEIGHT = 0.5 * WIDTH;

	private static final double BAR_SIZE = 8;

	private static final double BAR_PAD = 20;

	private final String title;

	@Setter
	@Getter
	private int expectedSteps;

	private int completedSteps;

	private final Rectangle progressTray;

	private final Rectangle progressBar;

	public SplashScreenPane( ProductCard card ) {
		this.title = card.getName();
		getStyleClass().addAll( "splashscreen" );

		Rebrand rebrand = card.getRebrand();
		// Get the title font size from the product card
		double titleFontSize = rebrand == null ? 125.0 : rebrand.getSplashScreenTitleFontSize();

		// Get the background image from the product card
		Class<?> splashScreenBackgroundClass = rebrand == null ? null : rebrand.getSplashScreenBackgroundClass();

		Canvas backgroundImage = null;
		if( splashScreenBackgroundClass == null ) {
			backgroundImage = new XRingLargeIcon().resize( 256 );
			backgroundImage.setLayoutX( 0.5 * (WIDTH - backgroundImage.getWidth()) );
			backgroundImage.setLayoutY( 0.5 * (HEIGHT - backgroundImage.getHeight() - BAR_PAD - BAR_SIZE) );
		} else {
			try {
				backgroundImage = (Canvas) splashScreenBackgroundClass.getDeclaredConstructor().newInstance();
				backgroundImage.setLayoutX( 0.5 * (WIDTH - backgroundImage.getWidth()) );
				backgroundImage.setLayoutY( 0.5 * (HEIGHT - backgroundImage.getHeight() - BAR_PAD - BAR_SIZE) );
			} catch( Exception exception ) {
				log.atDebug().withCause( exception ).log( "Failed to create splash screen image from class {}", splashScreenBackgroundClass );
			}
		}

		Rectangle tint = new Rectangle( 0, 0, WIDTH, HEIGHT );
		tint.getStyleClass().addAll( "tint" );

		Text titleText = new Text( title );
		titleText.getStyleClass().addAll( "title" );
		titleText.setBoundsType( TextBoundsType.VISUAL );
		titleText.setFont( new Font( titleFontSize ) );

		titleText.setX( 0.5 * (WIDTH - titleText.getLayoutBounds().getWidth()) );
		titleText.setY( 0.5 * (HEIGHT - titleText.getLayoutBounds().getHeight() - BAR_PAD - BAR_SIZE) + titleText.getLayoutBounds().getHeight() );

		progressTray = new Rectangle( BAR_PAD, HEIGHT - BAR_PAD - BAR_SIZE, WIDTH - 2 * BAR_PAD, BAR_SIZE );
		progressTray.getStyleClass().addAll( "progress", "progress-tray" );

		progressBar = new Rectangle( BAR_PAD, HEIGHT - BAR_PAD - BAR_SIZE, 0, BAR_SIZE );
		progressBar.getStyleClass().addAll( "progress", "progress-incomplete" );

		if( backgroundImage != null ) getChildren().addAll( backgroundImage, tint );
		getChildren().addAll( titleText );
		getChildren().addAll( progressTray, progressBar );

		setWidth( WIDTH );
		setHeight( HEIGHT );
	}

	public SplashScreenPane show( Stage stage ) {
		Scene scene = new Scene( this, getWidth(), getHeight(), Color.BLACK );

		// NOTE Application.setUserAgentStylesheet() must be called in application for this to work properly
		scene.getStylesheets().addAll( Xenon.STYLESHEET );

		stage.setTitle( title );
		stage.setScene( scene );
		stage.sizeToScene();
		stage.centerOnScreen();
		stage.show();
		stage.toFront();

		return this;
	}

	public void update() {
		Fx.run( () -> doSetProgress( ((double)completedSteps++ / (double)expectedSteps) ) );
	}

	public void setCompletedSteps( final double completedSteps ) {
		Fx.run( () -> doSetProgress( completedSteps ) );
	}

	public void done() {
		Fx.run( () -> {
			if( completedSteps != expectedSteps ) log.atWarning().log( "Progress/step mismatch: %d of %d", completedSteps, expectedSteps );
			completedSteps = expectedSteps;
			doSetProgress( 1 );
		} );
	}

	public void hide() {
		if( getScene() != null ) Fx.run( () -> getScene().getWindow().hide() );
	}

	private void doSetProgress( double requestedProgress ) {
		if( requestedProgress >= 1.0 ) {
			requestedProgress = 1.0;
			progressTray.setVisible( false );
			progressBar.getStyleClass().remove( "progress-incomplete" );
			progressBar.getStyleClass().add( "progress-complete" );
		}
		progressBar.setWidth( (getWidth() - 2 * BAR_PAD) * requestedProgress );
	}

}
