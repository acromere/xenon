package com.acromere.xenon.tool.settings.editor;

import com.acromere.xenon.XenonProgramProduct;
import com.acromere.xenon.tool.settings.SettingData;

public class InfoAreaSettingEditor extends InfoSettingEditor {

	public InfoAreaSettingEditor( XenonProgramProduct product, String rbKey, SettingData setting ) {
		super( product, rbKey, setting, InfoSettingEditor.Type.AREA );
	}

}
