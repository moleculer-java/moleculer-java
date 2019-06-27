/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2019 Andras Berkes [andras.berkes@programmer.net]<br>
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import services.moleculer.ServiceBroker;

/**
 * Skeleton of a simple, distributed Moleculer application.
 */
@SpringBootApplication(scanBasePackages = { "services.moleculer.config" })
public class SpringBootSample {

	@Autowired
	protected ServiceBroker broker;

	public static void main(String[] args) {
		SpringApplication.run(SpringBootSample.class, args);
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	public ServiceBroker getServiceBroker() {
		ServiceBrokerConfig cfg = new ServiceBrokerConfig();

		// Configure ServiceBroker:
		// cfg.setTransporter(new NatsTransporter("nats://localhost:4222"));
		// cfg.setCacher(new MemoryCacher(4096, 0));
		// cfg.setJsonReaders("boon,jackson");
		// cfg.setJsonWriters("jackson");

		return new ServiceBroker(cfg);
	}

	@Bean
	public SpringRegistrator getSpringRegistrator() {
		return new SpringRegistrator();
	}

}
