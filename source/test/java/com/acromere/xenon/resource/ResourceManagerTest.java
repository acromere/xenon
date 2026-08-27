package com.acromere.xenon.resource;

import com.acromere.xenon.ProgramTestCase;
import com.acromere.xenon.scheme.FileScheme;
import com.acromere.xenon.scheme.NewScheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class ResourceManagerTest extends ProgramTestCase {

	private ResourceManager manager;

	@BeforeEach
	@Override
	protected void setup() throws Exception {
		super.setup();
		manager = new ResourceManager( getProgram() );
		manager.addScheme( new MockScheme( getProgram() ) );
		manager.addScheme( new NewScheme( getProgram() ) );
		manager.addResourceType( new MockResourceType( getProgram() ) );
	}

	@Test
	void testGetNullResourceType() {
		assertThat( manager.getResourceType( null ) ).isNull();
	}

	@Test
	void testNewResource() throws Exception {
		// New resources have a resource type when created.
		// The URI is assigned when the resource is saved.
		Resource newResource = manager.createResource( manager.getResourceType( MockScheme.ID ) );
		assertThat( newResource.isNew() ).isTrue();
	}

	@Test
	void testOldResource() throws Exception {
		// Old resources have a URI when created.
		// The resource type is assigned when the resource is opened.
		String uri = "mock:///home/user/temp/test.txt";
		Resource oldResource = manager.createResource( uri );
		assertThat( oldResource.isNew() ).isFalse();
	}

	@Test
	void testCreateResourceWithUri() throws Exception {
		URI uri = URI.create( "mock:///home/user/temp/test.txt" );
		Resource resource = manager.createResource( uri );
		assertThat( resource.getScheme() ).isEqualTo( manager.getScheme( MockScheme.ID ) );
		assertThat( resource.getUri() ).isEqualTo( uri );
		assertThat( resource.isOpen() ).isFalse();
	}

	@Test
	void testCreateResourceWithString() throws Exception {
		String uri = "mock:///home/user/temp/test.txt";
		Resource resource = manager.createResource( uri );
		assertThat( resource.getScheme() ).isEqualTo( manager.getScheme( MockScheme.ID ) );
		assertThat( resource.getUri() ).isEqualTo( URI.create( uri ) );
		assertThat( resource.isOpen() ).isFalse();
	}

	@Test
	void testOpenResources() throws Exception {
		String uri = "mock:///home/user/temp/test.txt";
		Resource resource = manager.createResource( uri );
		ResourceWatcher watcher = new ResourceWatcher();
		resource.getEventHub().register( ResourceEvent.ANY, watcher );
		assertThat( resource.isOpen() ).isFalse();

		manager.openResources( resource );
		watcher.waitForEvent( ResourceEvent.OPENED );
		assertThat( resource.isOpen() ).isTrue();
	}

	@Test
	void testOpenResourcesAndWait() throws Exception {
		String uri = "mock:///home/user/temp/test.txt";
		Resource resource = manager.createResource( uri );
		ResourceWatcher watcher = new ResourceWatcher();
		resource.getEventHub().register( ResourceEvent.ANY, watcher );
		assertThat( resource.isOpen() ).isFalse();

		manager.openResourcesAndWait( resource, 1, TimeUnit.SECONDS );
		assertThat( resource.isOpen() ).isTrue();
	}

	@Test
	void testLoadResources() throws Exception {
		String uri = "mock:///home/user/temp/test.txt";
		Resource resource = manager.createResource( uri );
		ResourceWatcher watcher = new ResourceWatcher();
		resource.getEventHub().register( ResourceEvent.ANY, watcher );
		assertThat( resource.isLoaded() ).isFalse();

		manager.loadResources( resource );
		watcher.waitForEvent( ResourceEvent.LOADED );
		assertThat( resource.isOpen() ).isTrue();
		assertThat( resource.isLoaded() ).isTrue();
	}

	@Test
	void testLoadResourcesAndWait() throws Exception {
		String uri = "mock:///home/user/temp/test.txt";
		Resource resource = manager.createResource( uri );
		ResourceWatcher watcher = new ResourceWatcher();
		resource.getEventHub().register( ResourceEvent.ANY, watcher );
		assertThat( resource.isLoaded() ).isFalse();

		manager.loadResourcesAndWait( resource );
		assertThat( resource.isOpen() ).isTrue();
		assertThat( resource.isLoaded() ).isTrue();
	}

	@Test
	void testReloadResources() throws Exception {
		String uri = "mock:///home/user/temp/test.txt";
		Resource resource = manager.createResource( uri );
		ResourceWatcher watcher = new ResourceWatcher();
		resource.getEventHub().register( ResourceEvent.ANY, watcher );
		assertThat( resource.isLoaded() ).isFalse();

		manager.loadResourcesAndWait( resource );
		assertThat( resource.isOpen() ).isTrue();
		assertThat( resource.isLoaded() ).isTrue();
		assertThat( watcher.getLastEvent().getEventType() ).isEqualTo( ResourceEvent.LOADED );
		ResourceEvent event = watcher.getLastEvent();

		manager.reloadResourcesAndWait( resource );
		assertThat( watcher.getLastEvent().getEventType() ).isEqualTo( ResourceEvent.LOADED );
		assertThat( event ).isNotEqualTo( watcher.getLastEvent() );
	}

	@Test
	void testSaveResources() throws Exception {
		String uri = "mock:///home/user/temp/test.txt";
		Resource resource = manager.createResource( uri );
		ResourceWatcher watcher = new ResourceWatcher();
		resource.getEventHub().register( ResourceEvent.ANY, watcher );
		assertThat( resource.isSaved() ).isFalse();

		// Resource must be open to be saved
		manager.openResources( resource );
		watcher.waitForEvent( ResourceEvent.OPENED );
		assertThat( resource.isOpen() ).isTrue();

		// Resource must be loaded to be saved
		manager.loadResources( resource );
		watcher.waitForEvent( ResourceEvent.LOADED );
		assertThat( resource.isLoaded() ).isTrue();

		// And an resource must be modified to be saved
		resource.setModified( true );
		assertThat( resource.isSafeToSave() ).isTrue();

		manager.saveResources( resource );
		watcher.waitForEvent( ResourceEvent.SAVED );
		assertThat( resource.isSaved() ).isTrue();
	}

	@Test
	void testSaveResourcesAndWait() throws Exception {
		String uri = "mock:///home/user/temp/test.txt";
		Resource resource = manager.createResource( uri );
		ResourceWatcher watcher = new ResourceWatcher();
		resource.getEventHub().register( ResourceEvent.ANY, watcher );
		assertThat( resource.isSaved() ).isFalse();

		// Resource must be open to be saved
		manager.openResourcesAndWait( resource, 1, TimeUnit.SECONDS );
		assertThat( resource.isOpen() ).isTrue();

		// Resource must be loaded to be saved
		manager.loadResources( resource );
		watcher.waitForEvent( ResourceEvent.LOADED );
		assertThat( resource.isLoaded() ).isTrue();

		// And an resource must be modified to be saved
		resource.setModified( true );
		assertThat( resource.isSafeToSave() ).isTrue();

		manager.saveResourcesAndWait( resource );
		assertThat( resource.isSaved() ).isTrue();
	}

	@Test
	void testCloseResources() throws Exception {
		String uri = "mock:///home/user/temp/test.txt";
		Resource resource = manager.createResource( uri );
		ResourceWatcher watcher = new ResourceWatcher();
		resource.getEventHub().register( ResourceEvent.ANY, watcher );

		// Resource must be open to be closed
		manager.openResources( resource );
		watcher.waitForEvent( ResourceEvent.OPENED );
		assertThat( resource.isOpen() ).isTrue();

		manager.closeResources( resource );
		watcher.waitForEvent( ResourceEvent.CLOSED );
		assertThat( resource.isOpen() ).isFalse();
	}

	@Test
	void testCloseResourcesAndWait() throws Exception {
		String uri = "mock:///home/user/temp/test.txt";
		Resource resource = manager.createResource( uri );
		ResourceWatcher watcher = new ResourceWatcher();
		resource.getEventHub().register( ResourceEvent.ANY, watcher );

		// Resource must be open to be closed
		manager.openResourcesAndWait( resource, 1, TimeUnit.SECONDS );
		assertThat( resource.isOpen() ).isTrue();

		manager.closeResourceAndWait( resource );
		assertThat( resource.isOpen() ).isFalse();
	}

	@Test
	void testAutoDetectResourceTypeWithOpaqueUri() throws Exception {
		Resource resource = manager.createResource( URI.create( "mock:test" ) );
		manager.autoDetectResourceType( resource );
		assertThat( resource.getType() ).isInstanceOf( MockResourceType.class );
	}

	@Test
	void testAutoDetectCodecs() throws Exception {
		ResourceType type = manager.getResourceType( new MockResourceType( getProgram() ).getKey() );
		Resource resource = manager.createResource( URI.create( "mock:test.mock" ) );
		Set<Codec> codecs = manager.autoDetectCodecs( resource );
		assertThat( codecs ).isEqualTo( type.getCodecs() );
	}

	@Test
	void canRenameResourceWithNull() {
		assertThat( manager.canRenameResource( null ) ).isFalse();
	}

	@Test
	void canRenameResourceWithNewResource() throws Exception {
		Resource resource = manager.createResource( manager.getResourceType( FileScheme.ID ), "mock://test.mock" );
		assertThat( manager.canRenameResource( resource ) ).isFalse();
	}

	@Test
	void canRenameResourceWithOldResource() throws Exception {
		Resource resource = manager.createResource( "mock://test.mock" );
		manager.openResourcesAndWait( resource, 100, TimeUnit.MILLISECONDS );
		assertThat( manager.canRenameResource( resource ) ).isTrue();
	}

	@Test
	void cleanupUri() {
		URI provided = URI.create( "mock:///home/user/temp/test.txt?param1=one&param2=two#readwrite" );
		URI expected = URI.create( "mock:/home/user/temp/test.txt" );
		assertThat( manager.uriCleanup( provided ) ).isEqualTo( expected );
	}

}
