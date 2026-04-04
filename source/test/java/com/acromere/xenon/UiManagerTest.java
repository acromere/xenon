package com.acromere.xenon;

import com.acromere.settings.Settings;
import com.acromere.settings.StoredSettings;
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
		Workarea workarea = uiManager.createWorkarea( "Test Workarea" );

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

		assertThat( areaFolder ).existsNoFollowLinks();
		assertThat( viewFolder ).existsNoFollowLinks();
		assertThat( areaSettingsFolder ).existsNoFollowLinks();

		Settings areaSettings = new StoredSettings( areaSettingsFolder );
		Settings viewSettings = new StoredSettings( viewFolder );
		String areaKey = workarea.getUid();
		String viewKey = viewSettings.getNodes().getFirst();
		viewSettings = viewSettings.getNode( viewSettings.getNodes().getFirst() );

		assertThat( areaSettings.get( UiManager.ACTIVE ) ).isEqualTo( "false" );
		assertThat( areaSettings.get( UiManager.COLOR ) ).isEqualTo( "#206080ff" );
		assertThat( areaSettings.get( UiManager.NAME ) ).isEqualTo( "Test Workarea" );
		assertThat( areaSettings.get( UiManager.PAINT ) ).startsWith( "linear-gradient" );
		assertThat( areaSettings.get( Ui.DOCK_BOTTOM_SIZE)).isNull();
		assertThat( areaSettings.get( Ui.DOCK_LEFT_SIZE)).isNull();
		assertThat( areaSettings.get( Ui.DOCK_RIGHT_SIZE)).isNull();
		assertThat( areaSettings.get( Ui.DOCK_TOP_SIZE)).isNull();
		assertThat( areaSettings.get( Ui.VIEW_ACTIVE ) ).isEqualTo( viewKey );
		assertThat( areaSettings.get( Ui.VIEW_DEFAULT ) ).isEqualTo( viewKey );
		assertThat( areaSettings.get( Ui.VIEW_MAXIMIZED ) ).isNull();
		assertThat( areaSettings.get( UiManager.PARENT_SPACE_ID ) ).isNull();

		assertThat( viewSettings.get( Ui.B ) ).isEqualTo( Ui.BOTTOM );
		assertThat( viewSettings.get( Ui.L ) ).isEqualTo( Ui.LEFT );
		assertThat( viewSettings.get( Ui.R ) ).isEqualTo( Ui.RIGHT );
		assertThat( viewSettings.get( Ui.T ) ).isEqualTo( Ui.TOP );
		assertThat( viewSettings.get( UiManager.PARENT_AREA_ID ) ).isEqualTo( areaKey );
	}

}
