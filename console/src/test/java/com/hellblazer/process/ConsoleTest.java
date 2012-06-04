package com.hellblazer.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

import org.junit.Before;
import org.junit.Test;
import org.smartfrog.services.anubis.partition.test.controller.gui.GraphicController;
import org.smartfrog.services.anubis.partition.test.controller.gui.TestControllerConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hellblazer.jackal.testUtil.TestController;
import com.hellblazer.jackal.testUtil.gossip.GossipDiscoveryNode1Cfg;
import com.hellblazer.jackal.testUtil.gossip.GossipDiscoveryNode2Cfg;
import com.hellblazer.jackal.testUtil.gossip.GossipNodeCfg;
import com.hellblazer.jackal.testUtil.gossip.GossipTestCfg;
import com.hellblazer.process.impl.JavaProcessImpl;
import com.hellblazer.process.impl.ManagedProcessFactoryImpl;

import static junit.framework.Assert.*;

public class ConsoleTest extends ProcessTest {

	protected static final String TEST_DIR = "test-dirs/java-process-test";
	protected static final String TEST_JAR = "test.jar";
	MBeanServerConnection connection;
	JMXConnector connector;
	ManagedProcessFactoryImpl processFactory = new ManagedProcessFactoryImpl();
	protected File testDir;

	final int numberOfProcesses = 20;

	@Configuration
	static class member extends GossipNodeCfg {

		@Override
		@Bean
		public int node() {
			return id.incrementAndGet();
		}
	}

	@Configuration
	static class member1 extends GossipDiscoveryNode1Cfg {
		@Override
		public int node() {
			return id.incrementAndGet();
		}
	}

	@Configuration
	static class member2 extends GossipDiscoveryNode2Cfg {
		@Override
		public int node() {
			return id.incrementAndGet();
		}
	}

	private static final AtomicInteger id = new AtomicInteger(-1);

	static {
		GossipTestCfg.setTestPorts(24730, 24750);
	}

	private AnnotationConfigApplicationContext controllerContext;
	private GraphicController controller;

	protected void copyTestClassFile() throws Exception {

		ArrayList<Class> classFiles = new ArrayList<Class>();
		classFiles.add(Trial.class);
		classFiles.add(CallingThis.class);
		classFiles.add(SecondLayerCalling.class);
		// classFiles.add(AnnotationConfigApplicationContext.class);
		for (int i = 0; i < classFiles.size(); i++) {
			Class curr = classFiles.get(i);
			String classFileName = curr.getCanonicalName().replace('.', '/')
					+ ".class";
			URL classFile = getClass().getResource("/" + classFileName);
			assertNotNull(classFile);
			File copiedFile = new File(testDir, classFileName);
			if (i == 0) {
				assertTrue(copiedFile.getParentFile().mkdirs());
			}
			FileOutputStream out = new FileOutputStream(copiedFile);
			InputStream in = classFile.openStream();
			byte[] buffer = new byte[1024];
			for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
				out.write(buffer, 0, read);
			}
			in.close();
			out.close();
		}

	}

	protected void copyTestJarFile() throws Exception {
		String classFileName = Trial.class.getCanonicalName().replace('.', '/')
				+ ".class";
		URL classFile = getClass().getResource("/" + classFileName);
		assertNotNull(classFile);

		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.putValue("Manifest-Version", "1.0");
		attributes.putValue("Main-Class", HelloWorld.class.getCanonicalName());

		FileOutputStream fos = new FileOutputStream(new File(testDir, TEST_JAR));
		JarOutputStream jar = new JarOutputStream(fos, manifest);
		JarEntry entry = new JarEntry(classFileName);
		jar.putNextEntry(entry);
		InputStream in = classFile.openStream();
		byte[] buffer = new byte[1024];
		for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
			jar.write(buffer, 0, read);
		}
		in.close();
		jar.closeEntry();
		jar.close();
	}

	@Before
	public void setUp() {
		System.setProperty("sun.net.client.defaultConnectTimeout", "10000");
		System.setProperty("sun.net.client.defaultReadTimeout", "10000");
		System.setProperty("javax.net.debug", "all");
		Utils.initializeDirectory(TEST_DIR);
		testDir = new File(TEST_DIR);
	}

	protected Class<?>[] getConfigs() {
		return new Class<?>[] { member1.class, member2.class, member.class,
				member.class, member.class, member.class, member.class,
				member.class, member.class, member.class, member.class,
				member.class, member.class, member.class, member.class,
				member.class, member.class, member.class, member.class,
				member.class };
	}

	protected Class<?> getControllerConfig() {
		return TestControllerConfig.class;
	}

	@Test
	public void basicTest() throws Exception {
		controllerContext = new AnnotationConfigApplicationContext(
				getControllerConfig());
		controller = controllerContext.getBean(GraphicController.class);
		assertNotNull(controller);

		for (Class<?> config : getConfigs()) {
			new AnnotationConfigApplicationContext(config);
		}

		Thread.sleep(500000);
	}

	// This test tries to get the string version of the config class, convert it
	// back to class and create an
	// AnnotationConfigApplicationContext class. This is to see that converting
	// configs to strings can be converted
	// back to Class objects. This is to make sure that string version of Class
	// objects can be sent into
	// the main methods while the processes are spawned.
	@Test
	public void testMe() throws Exception {
		controllerContext = new AnnotationConfigApplicationContext(
				getControllerConfig());
		controller = controllerContext.getBean(GraphicController.class);
		assertNotNull(controller);

		for (Class<?> config : getConfigs()) {
			if (config.toString().equals(
					"class com.hellblazer.process.ConsoleTest$member1")) {
				String str = config.toString().replaceAll("class ", "");
				Class trial = Class.forName(str);
				new AnnotationConfigApplicationContext(trial);
			} else {
				new AnnotationConfigApplicationContext(config);
			}
		}

		Thread.sleep(500000);
	}

	// Try and create all the 20 members.class'es in a process that has been
	// spawned.
	// CURRENTLY NOT WORKING
	@Test
	public void testClassExecution() throws Exception {

		// controllerContext = new AnnotationConfigApplicationContext(
		// getControllerConfig());
		// controller = controllerContext.getBean(GraphicController.class);
		// assertNotNull(controller);

		copyTestClassFile();
		// copyCallingClassFile();
		System.out.println("comes here holy crap");
		JavaProcess process = new JavaProcessImpl(processFactory.create());

		process.setVmOptions(new String[] { "-cp",
				System.getProperty("java.class.path") });
		String[] args = new String[20];
		int i = 0;
		for (Class<?> config : getConfigs()) {
			System.out.println("class:" + config.toString());
			args[i] = config.toString().replaceFirst("class ", "");
			System.out.println(args[i]);

		}

		process.setArguments(args);
		process.setJavaClass(Trial.class.getCanonicalName());
		assertNull("No jar file set", process.getJarFile());
		process.setDirectory(testDir);
		process.setJavaExecutable(javaBin);
		process.start();
		// ////
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				process.getStdOut()));
		String line = reader.readLine();
		while (line != null) {
			System.out.println("line: " + line);
			line = reader.readLine();


		}
		assertEquals("Process exited normally", 0, process.waitFor());
		assertTrue("Process not active", !process.isActive());
		Thread.sleep(500000);

	}
}
