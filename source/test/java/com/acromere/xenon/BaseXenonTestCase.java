package com.acromere.xenon;

import com.acromere.product.ProductCard;
import com.acromere.util.FileUtil;
import com.acromere.util.OperatingSystem;
import com.acromere.util.ThreadUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.acromere.xenon.test.ProgramTestConfig.LONG_TIMEOUT;

/**
 * The super class for {@link BasePartXenonTestCase} and {@link BaseFullXenonTestCase}
 * classes. This class should not be subclassed directly by tests, but should
 * use one of the previous classes.
 */
@ExtendWith( MockitoExtension.class )
public abstract class BaseXenonTestCase extends BaseForAllTests {

	private Xenon program;

	static {
		//System.setProperty( "prism.forceGPU", "true" );
		//System.setProperty( "--enable-native-access", "javafx.graphics" );
		runHeadless();
	}

	@BeforeEach
	protected void setup() throws Exception {
		super.setup();

		if( OperatingSystem.isWindows() ) {
			System.setProperty( "jpackage.app-path", "C:\\Program Files\\Xenon\\Xenon.exe" );
		} else {
			System.setProperty( "jpackage.app-path", "/opt/xenon/bin/Xenon" );
		}

		// Remove the existing program data folder
		String suffix = "-" + XenonMode.TEST;
		ProductCard metadata = ProductCard.info( Xenon.class );
		Path programDataFolder = OperatingSystem.getUserProgramDataFolder( metadata.getArtifact() + suffix, metadata.getName() + suffix );

		// Try to clean up the program data folder, but don't fail if we can't
		try {
			FileUtil.delete( programDataFolder );
		} catch( IOException exception ) {
			// Ignore
		}
	}

	@AfterEach
	protected void teardown() throws Exception {
		// Clean up the settings
		// This fixes the problem where unexpected workspaces were being restored
		// and there was not an active workarea.
		if( program != null ) program.getSettingsManager().getSettings( ProgramSettings.BASE ).delete();
	}

	protected final Xenon getProgram() {
		return program;
	}

	protected final Xenon setProgram( Xenon program ) {
		this.program = program;
		return program;
	}

	private boolean aggressiveDelete( Path path ) throws IOException {
		// NOTE It has been determined that the StoredSettings can cause problems.
		//  The StoredSettings class can put these files back due to the delayed
		//  persist nature of StoredSettings. Be sure to also delete settings in
		//  teardown methods to reduce test cross-contamination.

		long limit = System.currentTimeMillis() + LONG_TIMEOUT;
		IOException exception = null;
		while( Files.exists( path ) && System.currentTimeMillis() < limit ) {
			try {
				FileUtil.delete( path );
			} catch( IOException deleteException ) {
				exception = deleteException;
				ThreadUtil.pause( 10 );
			}
		}

		// Check for a timeout
		if( System.currentTimeMillis() >= limit && exception != null ) throw exception;

		return !Files.exists( path );
	}

	private static void runHeadless() {
		// Use Monocle to run UI tests
		// <!-- https://wiki.openjdk.java.net/display/OpenJFX/Monocle -->
		System.setProperty( "glass.platform", "Headless" );

		// Set prism.order to sw when running tests in headless mode
		//
		// This fixes the following error:
		// X Error of failed request:  BadDrawable (invalid Pixmap or Window parameter)
		// Major opcode of failed request:  152 (GLX)
		// 	 Minor opcode of failed request:  29 (X_GLXGetDrawableAttributes)
		System.setProperty( "prism.order", "sw" );

		// Not sure what this setting does, but it's in all the examples found
		//System.setProperty( "prism.text", "t2k" );

		// Set testfx.setup.timeout to a reasonable time
		// 5000 - GitHub Actions, Mintbox Mini
		//  | Slower computers
		//  |
		//  | Faster computers
		// 1000 - AMD Threadripper, Intel i9</pre>
		System.setProperty( "testfx.setup.timeout", "5000" );
	}

}
