package org.sagebionetworks.repo.manager.jmx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

public class JmxManagerImpl implements JmxManager {
	private final Logger logger = LogManager.getLogger(JmxManagerImpl.class);
	private MemoryMXBean memoryMXBean;
	
	public JmxManagerImpl() {
		memoryMXBean = ManagementFactory.getMemoryMXBean();
	}

	@Override
	public void triggerFired() {
		MemoryUsage heapMemUsage = memoryMXBean.getHeapMemoryUsage();
		logger.debug("Heap used: " + heapMemUsage.getUsed());
	}

}
