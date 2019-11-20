# Native DLLs for Sigar Cpu Monitor

Moleculer Framework uses the Sigar API for CPU-based load-balancing.
Required Java dependencies:

 // https://mvnrepository.com/artifact/org.fusesource/sigar
 compile group: 'org.fusesource', name: 'sigar', version: '1.6.4'
 
 How to use Sigar monitor:
 
 ```java
 ServiceBroker broker = ServiceBroker.builder()
                                     .monitor(new SigarMonitor())
                                     .build()
                                     .start();
 ```
 
 If the "monitor" type is not set and the Sigar API is available in classpath,
 Moleculer Framework will automatically use Sigar API.