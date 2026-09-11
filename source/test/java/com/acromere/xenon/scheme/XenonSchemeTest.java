package com.acromere.xenon.scheme;

import com.acromere.xenon.BasePartXenonTestCase;
import com.acromere.xenon.resource.Codec;
import com.acromere.xenon.resource.MockResourceType;
import com.acromere.xenon.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith( MockitoExtension.class )
public class XenonSchemeTest extends BasePartXenonTestCase {

	private XenonScheme scheme;

	@Mock
	private Codec codec;

	private Resource resource;

	@BeforeEach
	protected void setup() throws Exception {
		super.setup();
		scheme = new XenonScheme( getProgram() );
		resource = new Resource( new MockResourceType( getProgram() ), URI.create( "xenon:/test" ) );
	}

	@Test
	void testGetName() {
		assertThat( scheme.getName() ).isEqualTo( XenonScheme.ID );
	}

	@Test
	void testCanLoad() throws Exception {
		assertThat( scheme.canLoad( resource ) ).isTrue();
	}

	@Test
	void testExists() throws Exception {
		assertThat( scheme.exists( resource ) ).isTrue();
	}

	@Test
	void testLoadDelegatesToCodec() throws Exception {
		scheme.load( resource, codec );

		verify( codec, times( 1 ) ).load( resource, null );
	}

}
