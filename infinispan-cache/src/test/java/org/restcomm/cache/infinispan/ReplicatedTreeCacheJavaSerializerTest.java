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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
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
import org.restcomm.cluster.CacheDataExecutorService;
import org.restcomm.cluster.CacheExecutorConfiguration;
import org.restcomm.cluster.DataListener;
import org.restcomm.cluster.DataRemovalListener;
import org.restcomm.cluster.IDGenerator;
import org.restcomm.cluster.RestcommCluster;
import org.restcomm.cluster.UUIDGenerator;
import org.restcomm.cluster.data.ClusteredTreeCacheData;
import org.restcomm.cluster.data.StringTreeSegment;
import org.restcomm.cluster.data.TreeSegment;
import org.restcomm.cluster.serializers.JavaSerializer;

public class ReplicatedTreeCacheJavaSerializerTest 
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
	    
	    transactionManager=new EmbeddedBaseTransactionManager();
	    IDGenerator<UUID> generator=new UUIDGenerator();
		
		CacheExecutorConfiguration configuration=new CacheExecutorConfiguration(16, 1000L, 1000L);
		factory=new InfinispanCacheFactory(transactionManager, new JavaSerializer(Thread.currentThread().getContextClassLoader()), generator, Thread.currentThread().getContextClassLoader(), new CacheDataExecutorService(configuration, generator, Thread.currentThread().getContextClassLoader()), 1000, false, true, false, 1, true);
		cluster=factory.getCluster("testtrj", true);
		cluster.startCluster(false);		
		
		factory2=new InfinispanCacheFactory(transactionManager, new JavaSerializer(Thread.currentThread().getContextClassLoader()), generator, Thread.currentThread().getContextClassLoader(), new CacheDataExecutorService(configuration, generator, Thread.currentThread().getContextClassLoader()), 1000, false, true, false, 1, true);
		cluster2=factory2.getCluster("testtrj", true);
		cluster2.startCluster(false);
	}
	
	@After
	public void clearData() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
	{
		transactionManager.begin();
		
		List<TreeSegment<?>> keys=cluster.getChildren(null);
		for(TreeSegment<?> curr:keys)
			cluster.treeRemove(curr, false);
		
		transactionManager.commit();
	}
	
	@AfterClass
	public static void stopCluster() throws SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException, SystemException
	{
		factory.stop();		
		factory2.stop();
	}
	
	@Test
	public void testRemoteTxCommit() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
	{
		Integer testDataStart=0;
		AtomicInteger removedCount=new AtomicInteger(0);
		DataRemovalListener drl=new DataRemovalListener() {
			
			@Override
			public void dataRemoved(Object key) {
				removedCount.incrementAndGet();
			}
		};
		
		cluster.addDataRemovalListener(drl);
		cluster2.addDataRemovalListener(drl);
		
		
		ExecutorService executor=Executors.newFixedThreadPool(2);
		//in tree , whenever the child is wrotten the parent is also locked, therefore different logic should be applied
		final Semaphore write1Semaphore=new Semaphore(0);
		final Semaphore write2Semaphore=new Semaphore(0);
		final Semaphore waitSemaphore=new Semaphore(0);
		
		List<String> expectedKeys=new ArrayList<String>(); 
		List<Integer> expectedValues=new ArrayList<Integer>(); 
		for(int i=0;i<20;i++)
		{
			String key="testtrj_Key_" + (testDataStart + i+1);
			expectedKeys.add(key);
			expectedValues.add(new Integer(i+1));
		}
		
		StringTreeSegment firstSegment=new StringTreeSegment("a", null);
		ClusteredTreeCacheData<String, Integer> firstSub=new ClusteredTreeCacheData<String, Integer>(firstSegment, cluster);
		assertFalse(firstSub.exists());
		for(int i=0;i<20;i++)
		{
			String key="testtrj_Key_" + (testDataStart + i+1);
			ClusteredTreeCacheData<String, Integer> child=new ClusteredTreeCacheData<String, Integer>(new StringTreeSegment(key, firstSegment), cluster);
			assertNull(child.getTreeValue());
		}
		
		firstSub.create();
		
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
					
					try {
						transactionManager.begin();
					}
					catch(Exception ex) {
						
					}
					
					for(int i=0;i<10;i++)
					{
						String key="testtrj_Key_" + (testDataStart + firstItem+i+1);
						ClusteredTreeCacheData<String, Integer> child=new ClusteredTreeCacheData<String, Integer>(new StringTreeSegment(key, firstSegment), localCluster);
						child.putValue(new Integer(firstItem+i+1));
					}
					
					waitSemaphore.release();
					
					try {
						writeSemaphore.acquire();
					}
					catch(InterruptedException ex) {
						
					}
					
					try {
						transactionManager.commit();
					}
					catch(Exception ex) {
					}
					
					waitSemaphore.release();
					
					try {
						writeSemaphore.acquire();
					}
					catch(InterruptedException ex) {
						
					}
					
					try {
						transactionManager.begin();
					}
					catch(Exception ex) {
						
					}
					
					for(int i=0;i<10;i+=2)
					{
						String key="testtrj_Key_" + (testDataStart + firstItem+i+1);
						ClusteredTreeCacheData<String, Integer> child=new ClusteredTreeCacheData<String, Integer>(new StringTreeSegment(key, firstSegment), localCluster);
						child.removeValue();
					}
					
					waitSemaphore.release();
					
					try {
						writeSemaphore.acquire();
					}
					catch(InterruptedException ex) {
						
					}
					
					try {
						transactionManager.commit();
					}
					catch(Exception ex) {
					}
					
					waitSemaphore.release();										
				}
			});
		}
		
		//4 iterations overall -> wrote but did not commited , wrote and committed , deleted and did not commited , deleted and committed
		write1Semaphore.release(1);
		write2Semaphore.release(1);
		try {
			waitSemaphore.acquire(2);
		} 
		catch(InterruptedException ex) {
			
		}
		
		assertTrue(firstSub.exists());
		assertNull(firstSub.getTreeValue());
		
		for(int i=0;i<20;i++)
		{
			String key="testtrj_Key_" + (testDataStart + i+1);
			ClusteredTreeCacheData<String, Integer> child=new ClusteredTreeCacheData<String, Integer>(new StringTreeSegment(key, firstSegment), cluster);
			assertNull(child.getTreeValue());
		}
		
		List<TreeSegment<?>> keys=cluster.getChildren(null);
		assertEquals(keys.size(),1);
		assertEquals(keys.get(0).getSegment(),firstSegment.getSegment());
		
		Map<TreeSegment<?>, Object> data=cluster.getChildrenData(firstSegment);
		assertEquals(data.size(),0);
		for(int i=0;i<10;i++)
		{
			Boolean found=false;
			Object value=null;
			Iterator<Entry<TreeSegment<?>, Object>> iterator=data.entrySet().iterator();
			while(iterator.hasNext()) {
				Entry<TreeSegment<?>, Object> currEntry=iterator.next();
				if(currEntry.getKey().getSegment().equals(expectedKeys.get(i))) {
					found=true;
					value=currEntry.getValue();
					break;
				}
			}
			
			assertFalse(found);
			assertNull(value);
		}
		
		write1Semaphore.release(1);
		write2Semaphore.release(1);
		try {
			waitSemaphore.acquire(2);
		} 
		catch(InterruptedException ex) {
			
		}
		
		assertTrue(firstSub.exists());
		for(int i=0;i<20;i++)
		{
			String key="testtrj_Key_" + (testDataStart + i+1);
			ClusteredTreeCacheData<String, Integer> child=new ClusteredTreeCacheData<String, Integer>(new StringTreeSegment(key, firstSegment), cluster);
			assertEquals(child.getTreeValue(),new Integer(i+1));
		}
		
		keys=cluster.getChildren(null);
		assertEquals(keys.size(),1);
		assertEquals(keys.get(0).getSegment(),firstSegment.getSegment());
		
		data=cluster.getChildrenData(firstSegment);
		assertEquals(data.size(),20);
		for(int i=0;i<20;i++)
		{
			Boolean found=false;
			Object value=null;
			Iterator<Entry<TreeSegment<?>, Object>> iterator=data.entrySet().iterator();
			while(iterator.hasNext()) {
				Entry<TreeSegment<?>, Object> currEntry=iterator.next();
				if(currEntry.getKey().getSegment().equals(expectedKeys.get(i))) {
					found=true;
					value=currEntry.getValue();
					break;
				}
			}
			
			assertTrue(found);
			assertEquals(value,expectedValues.get(i));
		}
		
		write1Semaphore.release(1);
		write2Semaphore.release(1);
		try {
			waitSemaphore.acquire(2);
		} 
		catch(InterruptedException ex) {
			
		}
		
		assertTrue(firstSub.exists());
		for(int i=0;i<20;i++)
		{
			String key="testtrj_Key_" + (testDataStart + i+1);
			ClusteredTreeCacheData<String, Integer> child=new ClusteredTreeCacheData<String, Integer>(new StringTreeSegment(key, firstSegment), cluster);
			assertEquals(child.getTreeValue(),new Integer(i+1));
		}
		
		keys=cluster.getChildren(null);
		assertEquals(keys.size(),1);
		assertEquals(keys.get(0).getSegment(),firstSegment.getSegment());
		
		data=cluster.getChildrenData(firstSegment);
		assertEquals(data.size(),20);
		for(int i=0;i<20;i++)
		{
			Boolean found=false;
			Object value=null;
			Iterator<Entry<TreeSegment<?>, Object>> iterator=data.entrySet().iterator();
			while(iterator.hasNext()) {
				Entry<TreeSegment<?>, Object> currEntry=iterator.next();
				if(currEntry.getKey().getSegment().equals(expectedKeys.get(i))) {
					found=true;
					value=currEntry.getValue();
					break;
				}
			}
			
			assertTrue(found);
			assertEquals(value,expectedValues.get(i));
		}
		
		write1Semaphore.release(1);
		write2Semaphore.release(1);
		try {
			waitSemaphore.acquire(2);
		}
		catch(InterruptedException ex) {
			
		}
		
		assertTrue(firstSub.exists());
		for(int i=0;i<20;i+=2)
		{
			String key="testtrj_Key_" + (testDataStart + i+1);
			ClusteredTreeCacheData<String, Integer> child=new ClusteredTreeCacheData<String, Integer>(new StringTreeSegment(key, firstSegment), cluster);
			assertNull(child.getTreeValue());
		}
		
		for(int i=1;i<20;i+=2)
		{
			String key="testtrj_Key_" + (testDataStart + i+1);
			ClusteredTreeCacheData<String, Integer> child=new ClusteredTreeCacheData<String, Integer>(new StringTreeSegment(key, firstSegment), cluster);
			assertEquals(child.getTreeValue(),new Integer(i+1));
		}
		
		keys=cluster.getChildren(null);
		assertEquals(keys.size(),1);
		assertEquals(keys.get(0).getSegment(),firstSegment.getSegment());
		
		data=cluster.getChildrenData(firstSegment);
		assertEquals(data.size(),10);
		for(int i=1;i<20;i+=2)
		{
			Boolean found=false;
			Object value=null;
			Iterator<Entry<TreeSegment<?>, Object>> iterator=data.entrySet().iterator();
			while(iterator.hasNext()) {
				Entry<TreeSegment<?>, Object> currEntry=iterator.next();
				if(currEntry.getKey().getSegment().equals(expectedKeys.get(i))) {
					found=true;
					value=currEntry.getValue();
					break;
				}
			}
			
			assertTrue(found);
			assertEquals(value,expectedValues.get(i));
		}
		
		//there is no notifications for data removal
		assertEquals(removedCount.get(),0);
		cluster.removeDataRemovalListener(drl);
		cluster2.removeDataRemovalListener(drl);
	}
	
	@Test
	public void testRemoteTxRollback() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
	{
		Integer testDataStart=0;
		AtomicInteger removedCount=new AtomicInteger(0);
		DataRemovalListener drl=new DataRemovalListener() {
			
			@Override
			public void dataRemoved(Object key) {
				removedCount.incrementAndGet();
			}
		};
		
		cluster.addDataRemovalListener(drl);
		cluster2.addDataRemovalListener(drl);
		
		
		ExecutorService executor=Executors.newFixedThreadPool(2);
		//in tree , whenever the child is wrotten the parent is also locked, therefore different logic should be applied
		final Semaphore write1Semaphore=new Semaphore(0);
		final Semaphore write2Semaphore=new Semaphore(0);
		final Semaphore waitSemaphore=new Semaphore(0);
		List<String> expectedKeys=new ArrayList<String>(); 
		List<Integer> expectedValues=new ArrayList<Integer>(); 
		for(int i=0;i<20;i++)
		{
			String key="testtrj_Key_" + (testDataStart + i+1);
			expectedKeys.add(key);
			expectedValues.add(new Integer(i+1));
		}
		
		StringTreeSegment firstSegment=new StringTreeSegment("a", null);
		ClusteredTreeCacheData<String, Integer> firstSub=new ClusteredTreeCacheData<String, Integer>(firstSegment, cluster);
		assertFalse(firstSub.exists());
		for(int i=0;i<20;i++)
		{
			String key="testtrj_Key_" + (testDataStart + i+1);
			ClusteredTreeCacheData<String, Integer> child=new ClusteredTreeCacheData<String, Integer>(new StringTreeSegment(key, firstSegment), cluster);
			assertNull(child.getTreeValue());
		}
		
		firstSub.create();
		
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
					
					try {
						transactionManager.begin();
					}
					catch(Exception ex) {						
					}
					
					for(int i=0;i<10;i++)
					{
						String key="testtrj_Key_" + (testDataStart + firstItem+i+1);
						ClusteredTreeCacheData<String, Integer> child=new ClusteredTreeCacheData<String, Integer>(new StringTreeSegment(key, firstSegment), localCluster);
						child.putValue(new Integer(firstItem+i+1));
					}
					
					waitSemaphore.release();
					
					try {
						writeSemaphore.acquire();
					}
					catch(InterruptedException ex) {
						
					}
					
					try {
						if(firstItem==10)
							transactionManager.rollback();
						else
							transactionManager.commit();
					}
					catch(Exception ex) {						
					}
					
					waitSemaphore.release();
					
					try {
						writeSemaphore.acquire();
					}
					catch(InterruptedException ex) {
						
					}
					
					try {
						transactionManager.begin();
					}
					catch(Exception ex) {
						
					}
					
					for(int i=0;i<10;i+=2)
					{
						String key="testtrj_Key_" + (testDataStart + firstItem+i+1);
						ClusteredTreeCacheData<String, Integer> child=new ClusteredTreeCacheData<String, Integer>(new StringTreeSegment(key, firstSegment), localCluster);
						child.removeValue();
					}
					
					waitSemaphore.release();
					
					try {
						writeSemaphore.acquire();
					}
					catch(InterruptedException ex) {
						
					}
					
					try {
						if(firstItem==10)
							transactionManager.commit();
						else
							transactionManager.rollback();
					}
					catch(Exception ex) {
					}
					
					waitSemaphore.release();										
				}
			});
		}
		
		//4 iterations overall -> wrote but did not commited , wrote and committed/rolledback , deleted and did not commited , deleted and committed/rolledback
		write1Semaphore.release(1);
		write2Semaphore.release(1);
		try {
			waitSemaphore.acquire(2);
		} 
		catch(InterruptedException ex) {
			
		}
		
		assertTrue(firstSub.exists());
		assertNull(firstSub.getTreeValue());
		
		for(int i=0;i<20;i++)
		{
			String key="testtrj_Key_" + (testDataStart + i+1);
			ClusteredTreeCacheData<String, Integer> child=new ClusteredTreeCacheData<String, Integer>(new StringTreeSegment(key, firstSegment), cluster);
			assertNull(child.getTreeValue());
		}
		
		List<TreeSegment<?>> keys=cluster.getChildren(null);
		assertEquals(keys.size(),1);
		assertEquals(keys.get(0).getSegment(),firstSegment.getSegment());
		
		Map<TreeSegment<?>, Object> data=cluster.getChildrenData(firstSegment);
		assertEquals(data.size(),0);
		for(int i=0;i<10;i++)
		{
			Boolean found=false;
			Object value=null;
			Iterator<Entry<TreeSegment<?>, Object>> iterator=data.entrySet().iterator();
			while(iterator.hasNext()) {
				Entry<TreeSegment<?>, Object> currEntry=iterator.next();
				if(currEntry.getKey().getSegment().equals(expectedKeys.get(i))) {
					found=true;
					value=currEntry.getValue();
					break;
				}
			}
			
			assertFalse(found);
			assertNull(value);
		}
		
		write1Semaphore.release(1);
		write2Semaphore.release(1);
		try {
			waitSemaphore.acquire(2);
		} 
		catch(InterruptedException ex) {
			
		}
		
		assertTrue(firstSub.exists());
		for(int i=0;i<10;i++)
		{
			String key="testtrj_Key_" + (testDataStart + i+1);
			ClusteredTreeCacheData<String, Integer> child=new ClusteredTreeCacheData<String, Integer>(new StringTreeSegment(key, firstSegment), cluster);
			assertEquals(child.getTreeValue(),new Integer(i+1));
		}
		
		for(int i=10;i<20;i++)
		{
			String key="testtrj_Key_" + (testDataStart + i+1);
			ClusteredTreeCacheData<String, Integer> child=new ClusteredTreeCacheData<String, Integer>(new StringTreeSegment(key, firstSegment), cluster);
			assertNull(child.getTreeValue());
		}
		
		keys=cluster.getChildren(null);
		assertEquals(keys.size(),1);
		assertEquals(keys.get(0).getSegment(),firstSegment.getSegment());
		
		data=cluster.getChildrenData(firstSegment);
		assertEquals(data.size(),10);
		for(int i=0;i<10;i++)
		{
			Boolean found=false;
			Object value=null;
			Iterator<Entry<TreeSegment<?>, Object>> iterator=data.entrySet().iterator();
			while(iterator.hasNext()) {
				Entry<TreeSegment<?>, Object> currEntry=iterator.next();
				if(currEntry.getKey().getSegment().equals(expectedKeys.get(i))) {
					found=true;
					value=currEntry.getValue();
					break;
				}
			}
			
			assertTrue(found);
			assertEquals(value,expectedValues.get(i));
		}
		
		for(int i=10;i<20;i++)
		{
			Boolean found=false;
			Object value=null;
			Iterator<Entry<TreeSegment<?>, Object>> iterator=data.entrySet().iterator();
			while(iterator.hasNext()) {
				Entry<TreeSegment<?>, Object> currEntry=iterator.next();
				if(currEntry.getKey().getSegment().equals(expectedKeys.get(i))) {
					found=true;
					value=currEntry.getValue();
					break;
				}
			}
			
			assertFalse(found);
			assertNull(value);
		}
		
		write1Semaphore.release(1);
		write2Semaphore.release(1);
		try {
			waitSemaphore.acquire(2);
		} 
		catch(InterruptedException ex) {
			
		}
		
		assertTrue(firstSub.exists());
		for(int i=0;i<10;i++)
		{
			String key="testtrj_Key_" + (testDataStart + i+1);
			ClusteredTreeCacheData<String, Integer> child=new ClusteredTreeCacheData<String, Integer>(new StringTreeSegment(key, firstSegment), cluster);
			assertEquals(child.getTreeValue(),new Integer(i+1));
		}
		
		for(int i=10;i<20;i++)
		{
			String key="testtrj_Key_" + (testDataStart + i+1);
			ClusteredTreeCacheData<String, Integer> child=new ClusteredTreeCacheData<String, Integer>(new StringTreeSegment(key, firstSegment), cluster);
			assertNull(child.getTreeValue());
		}
		
		keys=cluster.getChildren(null);
		assertEquals(keys.size(),1);
		assertEquals(keys.get(0).getSegment(),firstSegment.getSegment());
		
		data=cluster.getChildrenData(firstSegment);
		assertEquals(data.size(),10);
		for(int i=0;i<10;i++)
		{
			Boolean found=false;
			Object value=null;
			Iterator<Entry<TreeSegment<?>, Object>> iterator=data.entrySet().iterator();
			while(iterator.hasNext()) {
				Entry<TreeSegment<?>, Object> currEntry=iterator.next();
				if(currEntry.getKey().getSegment().equals(expectedKeys.get(i))) {
					found=true;
					value=currEntry.getValue();
					break;
				}
			}
			
			assertTrue(found);
			assertEquals(value,expectedValues.get(i));
		}
		
		for(int i=10;i<20;i++)
		{
			Boolean found=false;
			Object value=null;
			Iterator<Entry<TreeSegment<?>, Object>> iterator=data.entrySet().iterator();
			while(iterator.hasNext()) {
				Entry<TreeSegment<?>, Object> currEntry=iterator.next();
				if(currEntry.getKey().getSegment().equals(expectedKeys.get(i))) {
					found=true;
					value=currEntry.getValue();
					break;
				}
			}
			
			assertFalse(found);
			assertNull(value);
		}
		
		write1Semaphore.release(1);
		write2Semaphore.release(1);
		try {
			waitSemaphore.acquire(2);
		} 
		catch(InterruptedException ex) {
			
		}
		
		assertTrue(firstSub.exists());
		for(int i=0;i<10;i++)
		{
			String key="testtrj_Key_" + (testDataStart + i+1);
			ClusteredTreeCacheData<String, Integer> child=new ClusteredTreeCacheData<String, Integer>(new StringTreeSegment(key, firstSegment), cluster);
			assertEquals(child.getTreeValue(),new Integer(i+1));
		}
		
		for(int i=10;i<20;i++)
		{
			String key="testtrj_Key_" + (testDataStart + i+1);
			ClusteredTreeCacheData<String, Integer> child=new ClusteredTreeCacheData<String, Integer>(new StringTreeSegment(key, firstSegment), cluster);
			assertNull(child.getTreeValue());
		}
		
		keys=cluster.getChildren(null);
		assertEquals(keys.size(),1);
		assertEquals(keys.get(0).getSegment(),firstSegment.getSegment());
		
		data=cluster.getChildrenData(firstSegment);
		assertEquals(data.size(),10);
		for(int i=0;i<10;i++)
		{
			Boolean found=false;
			Object value=null;
			Iterator<Entry<TreeSegment<?>, Object>> iterator=data.entrySet().iterator();
			while(iterator.hasNext()) {
				Entry<TreeSegment<?>, Object> currEntry=iterator.next();
				if(currEntry.getKey().getSegment().equals(expectedKeys.get(i))) {
					found=true;
					value=currEntry.getValue();
					break;
				}
			}
			
			assertTrue(found);
			assertEquals(value,expectedValues.get(i));
		}
		
		for(int i=10;i<20;i++)
		{
			Boolean found=false;
			Object value=null;
			Iterator<Entry<TreeSegment<?>, Object>> iterator=data.entrySet().iterator();
			while(iterator.hasNext()) {
				Entry<TreeSegment<?>, Object> currEntry=iterator.next();
				if(currEntry.getKey().getSegment().equals(expectedKeys.get(i))) {
					found=true;
					value=currEntry.getValue();
					break;
				}
			}
			
			assertFalse(found);
			assertNull(value);
		}	
		
		assertEquals(removedCount.get(),0);
		cluster.removeDataRemovalListener(drl);
		cluster2.removeDataRemovalListener(drl);
	}
	
	@Test
	public void testRemoteNoTx() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
	{
		//without transactions tree is autocommited so the lock is auto released, no need of special handling
		Integer testDataStart=0;
		AtomicInteger removedCount=new AtomicInteger(0);
		DataListener drl=new DataListener() {
			
			@Override
			public void dataRemoved(Object key) {
				removedCount.incrementAndGet();
			}

			@Override
			public void dataCreated(Object key) {				
			}

			@Override
			public void dataModified(Object key) {				
			}
		};
		
		cluster.addDataListener(drl);
		cluster2.addDataListener(drl);
		
		ExecutorService executor=Executors.newFixedThreadPool(2);
		final Semaphore write1Semaphore=new Semaphore(0);
		final Semaphore write2Semaphore=new Semaphore(0);
		final Semaphore waitSemaphore=new Semaphore(0);
		
		List<String> expectedKeys=new ArrayList<String>(); 
		List<Integer> expectedValues=new ArrayList<Integer>(); 
		for(int i=0;i<20;i++)
		{
			String key="testtrj_Key_" + (testDataStart + i+1);
			expectedKeys.add(key);
			expectedValues.add(new Integer(i+1));
		}
		
		StringTreeSegment firstSegment=new StringTreeSegment("a", null);
		ClusteredTreeCacheData<String, Integer> firstSub=new ClusteredTreeCacheData<String, Integer>(firstSegment, cluster);
		assertFalse(firstSub.exists());
		for(int i=0;i<20;i++)
		{
			String key="testtrj_Key_" + (testDataStart + i+1);
			ClusteredTreeCacheData<String, Integer> child=new ClusteredTreeCacheData<String, Integer>(new StringTreeSegment(key, firstSegment), cluster);
			assertNull(child.getTreeValue());
		}
		
		firstSub.create();
		
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
					
					for(int i=0;i<10;i++)
					{
						String key="testtrj_Key_" + (testDataStart + firstItem+i+1);
						ClusteredTreeCacheData<String, Integer> child=new ClusteredTreeCacheData<String, Integer>(new StringTreeSegment(key, firstSegment), localCluster);
						child.putValue(new Integer(firstItem+i+1));
					}
					
					waitSemaphore.release();
					
					try {
						writeSemaphore.acquire();
					}
					catch(InterruptedException ex) {
						
					}
					
					for(int i=0;i<10;i+=2)
					{
						String key="testtrj_Key_" + (testDataStart + firstItem+i+1);
						ClusteredTreeCacheData<String, Integer> child=new ClusteredTreeCacheData<String, Integer>(new StringTreeSegment(key, firstSegment), localCluster);
						child.removeValue();
					}
					
					waitSemaphore.release();																								
				}
			});
		}
		
		//here only 2 iterations due to autocommit
		write1Semaphore.release(1);
		write2Semaphore.release(1);
		try {
			waitSemaphore.acquire(2);
		} 
		catch(InterruptedException ex) {
			
		}
		
		assertTrue(firstSub.exists());
		for(int i=0;i<20;i++)
		{
			String key="testtrj_Key_" + (testDataStart + i+1);
			ClusteredTreeCacheData<String, Integer> child=new ClusteredTreeCacheData<String, Integer>(new StringTreeSegment(key, firstSegment), cluster);
			assertEquals(child.getTreeValue(),new Integer(i+1));
		}
		
		List<TreeSegment<?>> keys=cluster.getChildren(null);
		assertEquals(keys.size(),1);
		assertEquals(keys.get(0).getSegment(),firstSegment.getSegment());
		
		Map<TreeSegment<?>, Object> data=cluster.getChildrenData(firstSegment);
		assertEquals(data.size(),20);
		for(int i=0;i<20;i++)
		{
			Boolean found=false;
			Object value=null;
			Iterator<Entry<TreeSegment<?>, Object>> iterator=data.entrySet().iterator();
			while(iterator.hasNext()) {
				Entry<TreeSegment<?>, Object> currEntry=iterator.next();
				if(currEntry.getKey().getSegment().equals(expectedKeys.get(i))) {
					found=true;
					value=currEntry.getValue();
					break;
				}
			}
			
			assertTrue(found);
			assertEquals(value,expectedValues.get(i));
		}
		
		write1Semaphore.release(1);
		write2Semaphore.release(1);
		try {
			waitSemaphore.acquire(2);
		} 
		catch(InterruptedException ex) {
			
		}
		
		assertTrue(firstSub.exists());
		for(int i=0;i<20;i+=2)
		{
			String key="testtrj_Key_" + (testDataStart + i+1);
			ClusteredTreeCacheData<String, Integer> child=new ClusteredTreeCacheData<String, Integer>(new StringTreeSegment(key, firstSegment), cluster);
			assertNull(child.getTreeValue());
		}
		
		for(int i=1;i<20;i+=2)
		{
			String key="testtrj_Key_" + (testDataStart + i+1);
			ClusteredTreeCacheData<String, Integer> child=new ClusteredTreeCacheData<String, Integer>(new StringTreeSegment(key, firstSegment), cluster);
			assertEquals(child.getTreeValue(),new Integer(i+1));
		}
		
		keys=cluster.getChildren(null);
		assertEquals(keys.size(),1);
		assertEquals(keys.get(0).getSegment(),firstSegment.getSegment());
		
		data=cluster.getChildrenData(firstSegment);
		assertEquals(data.size(),10);
		for(int i=1;i<20;i+=2)
		{
			Boolean found=false;
			Object value=null;
			Iterator<Entry<TreeSegment<?>, Object>> iterator=data.entrySet().iterator();
			while(iterator.hasNext()) {
				Entry<TreeSegment<?>, Object> currEntry=iterator.next();
				if(currEntry.getKey().getSegment().equals(expectedKeys.get(i))) {
					found=true;
					value=currEntry.getValue();
					break;
				}
			}
			
			assertTrue(found);
			assertEquals(value,expectedValues.get(i));
		}
		
		//there is no notifications for data removal
		assertEquals(removedCount.get(),0);			
		cluster.removeDataListener(drl);
		cluster2.removeDataListener(drl);
	}
}
