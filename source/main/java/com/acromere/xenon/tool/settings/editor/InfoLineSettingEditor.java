package com.acromere.xenon.tool.settings.editor;

import com.acromere.xenon.XenonProgramProduct;
import com.acromere.xenon.tool.settings.SettingData;

public class InfoLineSettingEditor extends InfoSettingEditor {

	public InfoLineSettingEditor( XenonProgramProduct product, String rbKey, SettingData setting ) {
		super( product, rbKey, setting, Type.FIELD );
	}

}
