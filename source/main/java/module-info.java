// NOTE Multiple attempts have been made to consolidate test classes into this
// module without success. There are several reasons this has not worked:
// 1. JUnit does not like to be both a test-time library and a compile-time library.
// 2. Trying to expose base test classes for mods did not work due to previous reason.
// 3. Trying to extract base test classes to a separate library caused a circular reference.

@SuppressWarnings( "requires-transitive-automatic" )
module com.acromere.xenon {

	// Compile-time only
	requires static lombok;
	requires static org.jspecify;

	// Both compile-time and run-time
	requires transitive com.acromere.zenna;
	requires transitive com.acromere.zerra;
	requires transitive com.acromere.zevra;
	requires transitive javafx.controls;
	requires transitive javafx.graphics;
	requires transitive javafx.fxml;
	requires transitive javafx.swing;
	requires transitive javafx.web;
	requires transitive org.fxmisc.undo;
	requires com.acromere.weave;
	requires java.net.http;
	requires java.logging;
	requires java.management;
	requires java.sql;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.annotation;
	requires org.controlsfx.controls;
	requires reactfx;

	// General exports
	exports com.acromere.xenon;
	exports com.acromere.xenon.action.common;
	exports com.acromere.xenon.demo;
	exports com.acromere.xenon.index;
	exports com.acromere.xenon.notice;
	exports com.acromere.xenon.product;
	exports com.acromere.xenon.resource;
	exports com.acromere.xenon.resource.exception;
	exports com.acromere.xenon.resource.type;
	exports com.acromere.xenon.scheme;
	exports com.acromere.xenon.task;
	exports com.acromere.xenon.test;
	exports com.acromere.xenon.throwable;
	exports com.acromere.xenon.tool;
	exports com.acromere.xenon.tool.guide;
	exports com.acromere.xenon.tool.settings;
	exports com.acromere.xenon.tool.settings.editor;
	exports com.acromere.xenon.trial;
	exports com.acromere.xenon.undo;
	exports com.acromere.xenon.util;
	exports com.acromere.xenon.ui;
	exports com.acromere.xenon.ui.util;
	exports com.acromere.xenon.workpane;
	exports com.acromere.xenon.workspace;

	opens com.acromere.xenon;
	opens com.acromere.xenon.bundles;
	opens com.acromere.xenon.product;
	opens com.acromere.xenon.settings;
	opens com.acromere.xenon.undo;
	opens com.acromere.xenon.trial;

	uses com.acromere.xenon.Module;
}
