/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * Based on Moleculer Framework for NodeJS [https://moleculer.services].
 * <br><br>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:<br>
 * <br>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.<br>
 * <br>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package services.moleculer.config;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.springframework.context.support.AbstractXmlApplicationContext;

import io.datatree.Tree;
import junit.framework.TestCase;
import services.moleculer.ServiceBroker;

public class MoleculerRunnerTest extends TestCase {

	// --- SPRING CONTEXT ---
	
	private AbstractXmlApplicationContext ctx;
	
	// --- TEST METHOD ---

	@Test
	public void testRunner() throws Exception {
		ServiceBroker broker = ctx.getBean(ServiceBroker.class);
		Tree rsp = broker.call("structService.action").waitFor(2000);
		assertEquals(1, rsp.get("a", 0));
		assertEquals(2, rsp.get("b", 0));
		assertEquals(3, rsp.get("c[0]", 0));
		assertEquals(4, rsp.get("c[1]", 0));
		assertEquals(5, rsp.get("c[2]", 0));
	}
	
	// --- START INSTANCE ---

	@Override
	protected void setUp() throws Exception {
		StringBuilder xml = new StringBuilder(512);
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
		xml.append("<beans xmlns=\"http://www.springframework.org/schema/beans\"\r\n");
		xml.append("	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:context=\"http://www.springframework.org/schema/context\"\r\n");
		xml.append("	xsi:schemaLocation=\"http://www.springframework.org/schema/beans\r\n");
		xml.append("	   http://www.springframework.org/schema/beans/spring-beans-3.0.xsd\r\n");
		xml.append("	   http://www.springframework.org/schema/context\r\n");
		xml.append("	   http://www.springframework.org/schema/context/spring-context-3.0.xsd\">\r\n");
		xml.append("	<context:annotation-config />\r\n");
		xml.append("	<bean id=\"registrator\" class=\"services.moleculer.config.SpringRegistrator\" depends-on=\"broker\">\r\n");
		xml.append("		<property name=\"packagesToScan\" value=\"services.moleculer.config\" />\r\n");
		xml.append("	</bean>\r\n");
		xml.append("	<bean id=\"broker\" class=\"services.moleculer.ServiceBroker\"\r\n");
		xml.append("		init-method=\"start\" destroy-method=\"stop\">\r\n");
		xml.append("		<constructor-arg ref=\"brokerConfig\" />\r\n");
		xml.append("	</bean>\r\n");
		xml.append("	<bean id=\"brokerConfig\" class=\"services.moleculer.config.ServiceBrokerConfig\">\r\n");
		xml.append("		<property name=\"namespace\"           value=\"\" />\r\n");
		xml.append("		<property name=\"nodeID\"              value=\"node-1\" />\r\n");
		xml.append("	</bean>\r\n");
		xml.append("</beans>");
		
		File file = new File("test.xml");
		FileOutputStream out = new FileOutputStream(file);
		out.write(xml.toString().getBytes(StandardCharsets.UTF_8));
		out.flush();
		out.close();		
		String path = "test.xml";
		file.deleteOnExit();

		String[] args = new String[3];
		args[0] = path;
		args[1] = "6789";
		args[2] = "secret123";
		
		MoleculerRunner.main(args);
		
		for (int i = 0; i < 10; i++) {
			ctx = MoleculerRunner.context.get();
			if (ctx != null) {
				break;
			}
			Thread.sleep(200);
		}
		assertNotNull(ctx);
		file.delete();
	}

	// --- STOP INSTANCE ---

	@Override
	protected void tearDown() throws Exception {
		if (ctx != null) {
			ctx.stop();
		}
	}
	
}