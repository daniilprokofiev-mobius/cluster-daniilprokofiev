package org.restcomm.cache.infinispan;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.infinispan.transaction.tm.EmbeddedBaseTransactionManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.cluster.CacheDataExecutorService;
import org.restcomm.cluster.CacheExecutorConfiguration;
import org.restcomm.cluster.IDGenerator;
import org.restcomm.cluster.RestcommCluster;
import org.restcomm.cluster.UUIDGenerator;
import org.restcomm.cluster.data.ClusteredIDTreeSegment;
import org.restcomm.cluster.data.StringTreeSegment;
import org.restcomm.cluster.data.TreeSegment;
import org.restcomm.cluster.serializers.KryoSerializer;

public class PerfomanceKryoSerializerTest 
{
	static EmbeddedBaseTransactionManager transactionManager;
	static InfinispanCacheFactory factory;
	static RestcommCluster cluster;
	
	@BeforeClass
	public static void setClass() {	
		Configurator.initialize(new DefaultConfiguration());
	    Configurator.setRootLevel(Level.INFO);
	    
	    transactionManager=new EmbeddedBaseTransactionManager();
		
	    IDGenerator<UUID> generator=new UUIDGenerator();
		
	    CacheExecutorConfiguration configuration=new CacheExecutorConfiguration(16, 1000L, 1000L);
		factory=new InfinispanCacheFactory(transactionManager,new KryoSerializer(Thread.currentThread().getContextClassLoader(),new CacheDataExecutorService(configuration, generator, Thread.currentThread().getContextClassLoader()),true), generator, Thread.currentThread().getContextClassLoader(), new CacheDataExecutorService(configuration, generator, Thread.currentThread().getContextClassLoader()), 1000, false, true, false, 1, true);
		cluster=factory.getCluster("testperfk", true);
		cluster.startCluster(true);	
	}
	
	public void tearDown(TreeSegment<?> rootKey) throws Exception {	
		transactionManager.begin();
		
		List<TreeSegment<?>> keys=cluster.getChildren(rootKey);
		for(TreeSegment<?> curr:keys)
			cluster.treeRemove(curr, false);
		
		transactionManager.commit();
		
		try {
			Thread.sleep(500);
		}
		catch(InterruptedException ex) {
			
		}
	}
	
	@AfterClass
	public static void stopCluster() throws SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException, SystemException
	{
		factory.stop();
	}
	
	@Test
	public void testCreatePerfomance() throws Exception
	{
		List<String> hosts=new ArrayList<String>();
		hosts.add("127.0.0.1");
		IDGenerator<UUID> generator=new UUIDGenerator();
		
		TreeSegment<?> key1=new ClusteredIDTreeSegment(generator.generateID(), null);
		cluster.treeCreate(key1,false);
		
		long startTime=System.currentTimeMillis();
		ExecutorService executor=Executors.newFixedThreadPool(64);
		Semaphore releaseSemaphore=new Semaphore(1-100000);
		for(int i=0;i<100000;i++) {
			executor.execute(new CreateRunnable(new StringTreeSegment(String.valueOf(i), key1), releaseSemaphore));								
		}
		
		try {
			releaseSemaphore.acquire();
		}
		catch (InterruptedException e) {
		}
		
		long endTime=System.currentTimeMillis();

		tearDown(key1);
		
		long deleteTime=System.currentTimeMillis();
		
		assertTrue((endTime-startTime)<40000L);
		assertTrue((deleteTime-endTime)<40000L);
		assertTrue((deleteTime-startTime)<70000L);
	}
	
	@Test
	public void testCreatePerfomanceWithTx() throws Exception
	{
		IDGenerator<UUID> generator=new UUIDGenerator();
		List<String> hosts=new ArrayList<String>();
		hosts.add("127.0.0.1");
		
		TreeSegment<?> key1=new ClusteredIDTreeSegment(generator.generateID(), null);
		cluster.treeCreate(key1,false);
		
		long startTime=System.currentTimeMillis();
		for(int i=0;i<100000;i++) {
			if(i%1000==0)
				transactionManager.begin();
			
			TreeSegment<?> sub1=new StringTreeSegment(String.valueOf(i), key1);
			cluster.treeCreate(sub1,false);
			
			if(i%1000==999)
				transactionManager.commit();
		}
		
		long endTime=System.currentTimeMillis();

		tearDown(key1);
		
		long deleteTime=System.currentTimeMillis();
		
		assertTrue((endTime-startTime)<40000L);
		assertTrue((deleteTime-endTime)<40000L);
		assertTrue((deleteTime-startTime)<70000L);
	}
	
	private class CreateRunnable implements Runnable {
		private StringTreeSegment element;
		private Semaphore releaseSemaphore;
		public CreateRunnable(StringTreeSegment element,Semaphore releaseSemaphore) {
			this.element=element;
			this.releaseSemaphore=releaseSemaphore;
		}
		
		@Override
		public void run() {
			cluster.treeCreate(element,false);
			releaseSemaphore.release();
		}	
	}
}