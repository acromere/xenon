package com.acromere.xenon;

import com.acromere.util.FileUtil;
import com.acromere.xenon.workspace.Workarea;
import com.acromere.zerra.javafx.Fx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class UiManagerTest extends BasePartXenonTestCase {

	private UiManager uiManager;

	@BeforeEach
	@Override
	public void setup() throws Exception {
		super.setup();
		uiManager = new UiManager( getProgram() );
		Fx.startup();
	}

	@Test
	void testUiManagerCreation() {
		assertThat( uiManager ).isNotNull();
	}

	@Test
	void testUiManagerGetProgram() {
		assertThat( uiManager.getProgram() ).isEqualTo( getProgram() );
	}

	@Test
	void testNewWorkarea() throws Exception {
		// given

		// when
		Workarea workarea = uiManager.newWorkarea( "Test Workarea" );

		// then
		assertThat( workarea ).isNotNull();
		assertThat( workarea.getName() ).isEqualTo( "Test Workarea" );

		// Check that settings files exist
		long timeout = getProgram().getSettingsManager().getMaxFlushLimit() * 3;
		Path settingsFolder = getProgram().getDataFolder().resolve( SettingsManager.ROOT );
		Path areaFolder = settingsFolder.resolve( ProgramSettings.AREA.substring( 1 ) );
		Path areaSettingsFolder = settingsFolder.resolve( ProgramSettings.AREA.substring( 1 ) ).resolve( workarea.getUid() );
		Path viewFolder = settingsFolder.resolve( ProgramSettings.VIEW.substring( 1 ) );

		FileUtil.waitToExist( settingsFolder, timeout, TimeUnit.MILLISECONDS );
		FileUtil.waitToExist( areaFolder, timeout, TimeUnit.MILLISECONDS );
		FileUtil.waitToExist( viewFolder, timeout, TimeUnit.MILLISECONDS );
		FileUtil.waitToExist( areaSettingsFolder, timeout, TimeUnit.MILLISECONDS );
		// TODO We don't know the view id yet

		assertThat( areaFolder ).existsNoFollowLinks();
		assertThat( viewFolder ).existsNoFollowLinks();
		assertThat( areaSettingsFolder ).existsNoFollowLinks();
	}

}
