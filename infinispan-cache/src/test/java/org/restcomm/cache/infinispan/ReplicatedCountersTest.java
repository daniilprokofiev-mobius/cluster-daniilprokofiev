package org.restcomm.cache.infinispan;
/*
 * Copyright 2022-2023, Mobius Software LTD. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.UUID;

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
import org.restcomm.cluster.IDGenerator;
import org.restcomm.cluster.RestcommCluster;
import org.restcomm.cluster.UUIDGenerator;

public class ReplicatedCountersTest 
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
		factory=new InfinispanCacheFactory("mobius", transactionManager, null, generator, Thread.currentThread().getContextClassLoader(), new CacheDataExecutorService(configuration, generator, Thread.currentThread().getContextClassLoader()), 1000, false, true, false, 1, true, null);
		cluster=factory.getCluster("testr", false);
		cluster.startCluster(true);		
		
		factory2=new InfinispanCacheFactory("mobius", transactionManager, null, generator, Thread.currentThread().getContextClassLoader(), new CacheDataExecutorService(configuration, generator, Thread.currentThread().getContextClassLoader()), 1000, false, true, false, 1, true, null);
		cluster2=factory2.getCluster("testr", false);
		cluster2.startCluster(true);
	}
	
	@After
	public void clearData() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
	{
		transactionManager.begin();
		
		Set<?> keys=cluster.getAllKeys();
		for(Object curr:keys)
			cluster.remove(curr, false, false);
		
		transactionManager.commit();
	}
	
	@AfterClass
	public static void stopCluster() throws SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException, SystemException
	{
		factory.stop();		
		factory2.stop();
	}
	
	@Test
	public void testCounters() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
	{
		assertEquals(cluster.getAtomicValue("testkey"),new Long(0L));
		assertEquals(cluster2.addAndGetAtomicValue("testkey",2L),new Long(2L));
		assertTrue(cluster.compareAndSetAtomicValue("testkey",2L,5L));
		assertEquals(cluster2.getAtomicValue("testkey"),new Long(5L));		
	}
}