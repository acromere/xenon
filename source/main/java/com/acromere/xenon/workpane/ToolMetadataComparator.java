package com.acromere.xenon.workpane;

import com.acromere.xenon.ToolRegistration;

import java.util.Comparator;

public class ToolMetadataComparator implements Comparator<ToolRegistration> {

	@Override
	public int compare( ToolRegistration metadata1, ToolRegistration metadata2 ) {
		return metadata1.getName().compareToIgnoreCase( metadata2.getName() );
	}

}
