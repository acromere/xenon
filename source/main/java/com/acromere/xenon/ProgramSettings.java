package com.acromere.xenon;

public interface ProgramSettings {

	String BASE = "/";

	String PRODUCT = BASE + "product/";

	String PROGRAM = BASE + "program/";

	String MANAGER = PROGRAM + "manager/";

	String RESOURCE = BASE + "resource/";

	String RESOURCE_TYPE = BASE + "resource/type/";

	String UPDATES = PROGRAM + "updates/";

	// User Interface Settings

	String UI = BASE + "ui/";

	String SPACE = UI + "space/";

	String AREA = UI + "area/";

	String EDGE = UI + "edge/";

	String VIEW = UI + "view/";

	String TOOL = UI + "tool/";

}
