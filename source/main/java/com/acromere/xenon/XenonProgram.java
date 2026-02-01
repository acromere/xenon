package com.acromere.xenon;

import com.acromere.event.Event;
import com.acromere.event.EventHandler;
import com.acromere.event.EventHub;
import com.acromere.event.EventType;
import com.acromere.product.ProductCard;
import com.acromere.product.Program;
import com.acromere.product.ProgramProduct;
import com.acromere.settings.Settings;
import com.acromere.xenon.resource.ResourceManager;
import com.acromere.xenon.resource.ResourceWatchService;
import com.acromere.xenon.index.IndexService;
import com.acromere.xenon.notice.NoticeManager;
import com.acromere.xenon.product.ProductManager;
import com.acromere.xenon.task.TaskManager;
import javafx.stage.Stage;

import java.nio.file.Path;

public interface XenonProgram extends Program, ProgramProduct, XenonProgramProduct {

	// THREAD JavaFX-Launcher
	// EXCEPTIONS Handled by the FX framework
	void init() throws Exception;

	// THREAD JavaFX Application Thread
	// EXCEPTIONS Handled by the FX framework
	void start( Stage stage );

	Xenon initForTesting( com.acromere.util.Parameters parameters ) throws Exception;

	// THREAD JavaFX Application Thread
	// EXCEPTIONS Handled by the FX framework
	void stop() throws Exception;

	// THREAD JavaFX Application Thread
	// EXCEPTIONS Handled by the FX framework
	void requestRestart( RestartJob.Mode mode, String... commands );

	boolean requestExit( boolean skipChecks );

	boolean requestExit( boolean skipVerifyCheck, boolean skipKeepAliveCheck );

	boolean isRunning();

	boolean isHardwareRendered();

	boolean isUpdateInProgress();

	void setUpdateInProgress( boolean updateInProgress );

	com.acromere.util.Parameters getProgramParameters();

	Xenon setProgramParameters( com.acromere.util.Parameters parameters );

	@Override
	Xenon getProgram();

	/**
	 * Get the execution profile.
	 *
	 * @see XenonFlag#PROFILE
	 */
	String getProfile();

	/**
	 * Get the execution mode.
	 *
	 * @see XenonFlag#MODE
	 */
	String getMode();

	/**
	 * Get the home folder. If the home folder is null that means that the program is not installed locally and was most likely started with a technology like
	 * Java Web Start.
	 *
	 * @return The home folder
	 */
	Path getHomeFolder();

	boolean isProgramUpdated();

	@Override
	ProductCard getCard();

	@Override
	Settings getSettings();

	Path getDataFolder();

	Path getLogFolder();

	Path getTempFolder();

	UpdateManager getUpdateManager();

	TaskManager getTaskManager();

	IconLibrary getIconLibrary();

	ActionLibrary getActionLibrary();

	SettingsManager getSettingsManager();

	ToolManager getToolManager();

	ResourceManager getResourceManager();

	ThemeManager getThemeManager();

	WorkspaceManager getWorkspaceManager();

	ProductManager getProductManager();

	NoticeManager getNoticeManager();

	IndexService getIndexService();

	ResourceWatchService getResourceWatchService();

	<T extends Event> EventHub register( EventType<? super T> type, EventHandler<? super T> handler );

	<T extends Event> EventHub unregister( EventType<? super T> type, EventHandler<? super T> handler );

	/**
	 * This implementation only returns the product card name.
	 */
	@Override
	String toString();

	Path getHomeFromLauncherPath();

}
