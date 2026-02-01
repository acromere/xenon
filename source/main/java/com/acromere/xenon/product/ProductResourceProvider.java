package com.acromere.xenon.product;

import java.util.Set;

public interface ProductResourceProvider {

	Set<ProductResource> getResources() throws Exception;

}
