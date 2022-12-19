package org.restcomm.cache.infinispan;
/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.infinispan.transaction.tm.EmbeddedBaseTransactionManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.cluster.AsyncCacheCallback;
import org.restcomm.cluster.CacheDataExecutorService;
import org.restcomm.cluster.CacheExecutorConfiguration;
import org.restcomm.cluster.DataRemovalListener;
import org.restcomm.cluster.IDGenerator;
import org.restcomm.cluster.RestcommCluster;
import org.restcomm.cluster.UUIDGenerator;
import org.restcomm.cluster.data.ClusteredCacheData;

public class AsyncReplicatedCacheTest 
{
	static EmbeddedBaseTransactionManager transactionManager;
	static InfinispanCacheFactory factory;
	static InfinispanCacheFactory factory2;
	static RestcommCluster cluster,cluster2;
	
	@BeforeClass
	public static void initCluster()
	{
		Configurator.initialize(new DefaultConfiguration());
	    Configurator.setRootLevel(Level.INFO);
	    
		transactionManager=null;	    
		IDGenerator<UUID> generator=new UUIDGenerator();
	    
		CacheExecutorConfiguration configuration=new CacheExecutorConfiguration(16, 1000L, 1000L);
		factory=new InfinispanCacheFactory(transactionManager, null, generator, Thread.currentThread().getContextClassLoader(), new CacheDataExecutorService(configuration, generator, Thread.currentThread().getContextClassLoader()), 1000, true, true, false, 1, true);
		cluster=factory.getCluster("testr", false);
		cluster.startCluster(true);		
		
		factory2=new InfinispanCacheFactory(transactionManager, null, generator, Thread.currentThread().getContextClassLoader(), new CacheDataExecutorService(configuration, generator, Thread.currentThread().getContextClassLoader()), 1000, true, true, false, 1, true);
		cluster2=factory2.getCluster("testr", false);
		cluster2.startCluster(true);
	}
	
	@After
	public void clearData() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
	{
		Set<?> keys=cluster.getAllKeys();
		Semaphore releaseSemaphore=new Semaphore(1-keys.size());
		AsyncCacheCallback<Object> removeCallback=new AsyncCacheCallback<Object>() {
			
			@Override
			public void onSuccess(Object value) {
				releaseSemaphore.release();
			}
			
			@Override
			public void onError(Throwable error) {
				releaseSemaphore.release();
			}
		};
		
		for(Object curr:keys)
			cluster.removeAsync(curr, false, removeCallback);
		
		if(keys.size()>0) {
			try {
				releaseSemaphore.acquire();
			}
			catch(InterruptedException ex) {
				
			}
		}
	}
	
	@AfterClass
	public static void stopCluster() throws SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException, SystemException
	{
		factory.stop();		
		factory2.stop();
	}
	
	@Test
	public void testRemoteNoTx() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
	{
		AtomicInteger removedCount=new AtomicInteger(0);
		DataRemovalListener drl=new DataRemovalListener() {
			
			@Override
			public void dataRemoved(Object key) {
				removedCount.incrementAndGet();
			}
		};
		
		cluster.addDataRemovalListener(drl);
		cluster2.addDataRemovalListener(drl);
		
		Integer testDataStart=0;
		
		ExecutorService executor=Executors.newFixedThreadPool(2);
		final Semaphore write1Semaphore=new Semaphore(0);
		final Semaphore write2Semaphore=new Semaphore(0);
		final Semaphore waitSemaphore=new Semaphore(0);
		for(int n=0;n<2;n++)
		{
			final Integer currIteration=n;
			executor.execute(new Runnable() {
				
				@Override
				public void run() {	
					int firstItem=currIteration*10;
					RestcommCluster localCluster;
					if(currIteration==0)
						localCluster=cluster;
					else
						localCluster=cluster2;
					
					Semaphore writeSemaphore;
					if(currIteration==0)
						writeSemaphore=write1Semaphore;
					else
						writeSemaphore=write2Semaphore;
					
					try {
						writeSemaphore.acquire();
					}
					catch(InterruptedException ex) {
						
					}
					
					final Semaphore asyncSemaphore=new Semaphore(0);
					
					AsyncCacheCallback<Void> callback=new AsyncCacheCallback<Void>() {
						
						@Override
						public void onSuccess(Void value) {
							asyncSemaphore.release();
						}
						
						@Override
						public void onError(Throwable error) {
							asyncSemaphore.release();
						}
					};
					
					for(int i=0;i<10;i++)
					{
						String key="testr_Key_" + (testDataStart + firstItem+i+1);
						ClusteredCacheData<String, Integer> cacheData=new ClusteredCacheData<String, Integer>(key, localCluster);
						cacheData.putValueAsync(new Integer(firstItem+i+1),callback);
					}
					
					try {
						asyncSemaphore.acquire(10);
					}
					catch(InterruptedException ex) {
						
					}
					
					waitSemaphore.release();
					
					try {
						writeSemaphore.acquire();
					}
					catch(InterruptedException ex) {
						
					}
					
					AsyncCacheCallback<Integer> removeCallback=new AsyncCacheCallback<Integer>() {
						
						@Override
						public void onSuccess(Integer value) {
							asyncSemaphore.release();
						}
						
						@Override
						public void onError(Throwable error) {
							asyncSemaphore.release();
						}
					};
					
					for(int i=0;i<10;i+=2)
					{
						String key="testr_Key_" + (testDataStart + firstItem+i+1);
						ClusteredCacheData<String, Integer> cacheData=new ClusteredCacheData<String, Integer>(key, localCluster);
						cacheData.removeElementAsync(removeCallback);					
					}
					
					try {
						asyncSemaphore.acquire(5);
					}
					catch(InterruptedException ex) {
						
					}
					
					waitSemaphore.release();																								
				}
			});
		}
		
		List<String> expectedKeys=new ArrayList<String>(); 
		List<Integer> expectedValues=new ArrayList<Integer>(); 
		for(int i=0;i<20;i++)
		{
			String key="testr_Key_" + (testDataStart + i+1);
			expectedKeys.add(key);
			expectedValues.add(new Integer(i+1));
		}
		
		//here only 2 iterations due to autocommit
		write1Semaphore.release(1);
		write2Semaphore.release(1);
		try {
			waitSemaphore.acquire(2);
		} 
		catch(InterruptedException ex) {
			
		}
		
		AtomicBoolean dataCorrect=new AtomicBoolean(true);
		Semaphore pendingSemaphore=new Semaphore(0);		
		AsyncCacheCallback<Boolean> existsCallback=new AsyncCacheCallback<Boolean>() {
			
			@Override
			public void onSuccess(Boolean value) {
				if(value==null || !value)
					dataCorrect.set(false);
				
				pendingSemaphore.release();
			}
			
			@Override
			public void onError(Throwable error) {
				dataCorrect.set(false);
				pendingSemaphore.release();
			}
		};
		
		for(int i=0;i<20;i++)
		{
			String key="testr_Key_" + (testDataStart + i+1);
			ClusteredCacheData<String, Integer> cacheData=new ClusteredCacheData<String, Integer>(key, cluster);
			cacheData.existsAsync(existsCallback);
			
			final Integer currValue=i;
			cacheData.getValueAsync(new AsyncCacheCallback<Integer>() {
				
				@Override
				public void onSuccess(Integer value) {
					if(value==null || !value.equals(new Integer(currValue+1)))
						dataCorrect.set(false);
					
					pendingSemaphore.release();
				}
				
				@Override
				public void onError(Throwable error) {
					dataCorrect.set(false);
					pendingSemaphore.release();
				}
			});
		}
		
		try {
			pendingSemaphore.acquire(40);
		}
		catch(InterruptedException ex) {
			
		}
		
		assertTrue(dataCorrect.get());
		
		Set<?> keys=cluster.getAllKeys();
		assertEquals(keys.size(),20);
		for(int i=0;i<20;i++)
			assertTrue(keys.contains(expectedKeys.get(i)));
		
		Map<?,?> data=cluster.getAllElements();
		assertEquals(data.size(),20);
		for(int i=0;i<20;i++)
		{
			assertTrue(data.containsKey(expectedKeys.get(i)));
			assertEquals(data.get(expectedKeys.get(i)),expectedValues.get(i));
		}
		
		write1Semaphore.release(1);
		write2Semaphore.release(1);
		try {
			waitSemaphore.acquire(2);
		} 
		catch(InterruptedException ex) {
			
		}
		
		AsyncCacheCallback<Boolean> nonExistsCallback=new AsyncCacheCallback<Boolean>() {
			
			@Override
			public void onSuccess(Boolean value) {
				if(value==null || value)
					dataCorrect.set(false);
				
				pendingSemaphore.release();
			}
			
			@Override
			public void onError(Throwable error) {
				dataCorrect.set(false);
				pendingSemaphore.release();
			}
		};
		
		for(int i=0;i<20;i+=2)
		{
			String key="testr_Key_" + (testDataStart + i+1);
			ClusteredCacheData<String, Integer> cacheData=new ClusteredCacheData<String, Integer>(key, cluster);
			cacheData.existsAsync(nonExistsCallback);
			cacheData.getValueAsync(new AsyncCacheCallback<Integer>() {
				
				@Override
				public void onSuccess(Integer value) {
					if(value!=null)
						dataCorrect.set(false);
					
					pendingSemaphore.release();
				}
				
				@Override
				public void onError(Throwable error) {
					dataCorrect.set(false);
					pendingSemaphore.release();
				}
			});
		}
				
		for(int i=1;i<20;i+=2)
		{
			String key="testr_Key_" + (testDataStart + i+1);
			ClusteredCacheData<String, Integer> cacheData=new ClusteredCacheData<String, Integer>(key, cluster);
			cacheData.existsAsync(existsCallback);
			final Integer currValue=i;
			cacheData.getValueAsync(new AsyncCacheCallback<Integer>() {
				
				@Override
				public void onSuccess(Integer value) {
					if(value==null || !value.equals(expectedValues.get(currValue)))
						dataCorrect.set(false);
					
					pendingSemaphore.release();
				}
				
				@Override
				public void onError(Throwable error) {
					dataCorrect.set(false);
					pendingSemaphore.release();
				}
			});
		}
				
		try {
			pendingSemaphore.acquire(40);
		}
		catch(InterruptedException ex) {
			
		}
		
		assertTrue(dataCorrect.get());
		
		keys=cluster.getAllKeys();
		assertEquals(keys.size(),10);
		for(int i=0;i<20;i+=2)
			assertFalse(keys.contains(expectedKeys.get(i)));
				
		for(int i=1;i<20;i+=2)
			assertTrue(keys.contains(expectedKeys.get(i)));
				
		data=cluster.getAllElements();
		assertEquals(data.size(),10);
		for(int i=0;i<20;i+=2)
		{
			assertFalse(data.containsKey(expectedKeys.get(i)));
			assertNull(data.get(expectedKeys.get(i)));
		}
				
		for(int i=1;i<20;i+=2)
		{
			assertTrue(data.containsKey(expectedKeys.get(i)));
			assertEquals(data.get(expectedKeys.get(i)),expectedValues.get(i));
		}
		
		assertTrue(removedCount.get()>=10);
		assertTrue(removedCount.get()<=20);
		cluster.removeDataRemovalListener(drl);
		cluster2.removeDataRemovalListener(drl);
	}
}