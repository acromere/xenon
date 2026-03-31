package com.acromere.xenon;

public interface ProgramSettings {

	String BASE = "/";

	String ASSET = BASE + "asset/";

	String ASSET_TYPE = BASE + "asset/type/";

	String PRODUCT = BASE + "product/";

	String PROGRAM = BASE + "program/";

	String MANAGER = PROGRAM + "manager/";

	String UPDATES = PROGRAM + "updates/";

	// User Interface Settings

	String UI = BASE + "ui/";

	String SPACE = UI + "space/";

	String AREA = UI + "area/";

	// TODO Remove in 1.9-SNAPSHOT
	@Deprecated( since = "1.7", forRemoval = true )
	String PANE = UI + "pane/";

	String EDGE = UI + "edge/";

	String VIEW = UI + "view/";

	String TOOL = UI + "tool/";

}
