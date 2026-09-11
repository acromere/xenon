package com.acromere.xenon.resource;

import com.acromere.index.Document;
import com.acromere.product.Rb;
import com.acromere.xenon.BasePartXenonTestCase;
import com.acromere.xenon.index.IndexService;
import com.acromere.xenon.resource.type.ProgramHelpType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

public class HelpContentCodecTest extends BasePartXenonTestCase {

	private HelpContentCodec codec;

	private Resource resource;

	private IndexService indexService;

	@BeforeEach
	protected void setup() throws Exception {
		super.setup();
		Rb.init( getProgram() );
		indexService = new IndexService( getProgram() ).start();
		lenient().when( getProgram().getIndexService() ).thenReturn( indexService );
		codec = new HelpContentCodec( getProgram() );
		resource = new Resource( new ProgramHelpType( getProgram() ), ProgramHelpType.URI );
	}

	@AfterEach
	protected void teardown() {
		if( indexService != null ) indexService.stop();
	}

	@Test
	void testKeyAndName() {
		assertThat( codec.getKey() ).isEqualTo( "content" );
		assertThat( codec.getName() ).isEqualTo( "content" );
	}

	@Test
	void testCanLoadAndSave() {
		assertThat( codec.canLoad() ).isTrue();
		assertThat( codec.canSave() ).isFalse();
	}

	@Test
	void testLoadFromInputStream() throws Exception {
		String testHtml = "<html><body><h1>Help Title</h1><p>Help text</p></body></html>";
		ByteArrayInputStream input = new ByteArrayInputStream( testHtml.getBytes( StandardCharsets.UTF_8 ) );

		codec.load( resource, input );

		assertThat( resource.<String>getModel() ).isEqualTo( testHtml );
	}

	@Test
	void testLoadFromEmptyInputStreamThrowsException() {
		ByteArrayInputStream input = new ByteArrayInputStream( new byte[0] );

		assertThatThrownBy( () -> codec.load( resource, input ) )
			.isInstanceOf( IOException.class )
			.hasMessage( "No content loaded!" );
	}

	@Test
	void testLoadFromIndexCacheWhenDocumentExists() throws Exception {
		URI helpUri = ProgramHelpType.URI;
		String helpHtml = "<html><body>Cached Help Content</body></html>";
		Document document = new Document( helpUri, "icon", "Help", helpHtml );
		document.properties().put( IndexService.STORE_CONTENT, Boolean.TRUE );

		getProgram().getIndexService().submit( document );

		codec.load( resource, null );

		assertThat( resource.<String>getModel() ).isEqualTo( helpHtml );
	}

	@Test
	void testLoadFromIndexCacheWhenDocumentNotFound() throws Exception {
		URI unknownUri = URI.create( "xenon:/help/nonexistent" );
		Resource unknownResource = new Resource( new ProgramHelpType( getProgram() ), unknownUri );

		codec.load( unknownResource, null );

		String message = Rb.text( getProgram(), "program", "help-document-not-found" );
		assertThat( unknownResource.<String>getModel() ).isEqualTo( "<html><body>" + message + "</body></html>" );
	}

}
