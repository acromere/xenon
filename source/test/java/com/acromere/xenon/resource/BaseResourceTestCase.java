package com.acromere.xenon.resource;

import com.acromere.xenon.XenonProgramProduct;
import com.acromere.xenon.mod.MockMod;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseResourceTestCase {

	@Getter
	private XenonProgramProduct product;

	@BeforeEach
	public void setup() throws Exception {
		product = new MockMod();
	}

}
