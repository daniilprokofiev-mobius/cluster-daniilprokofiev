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

import java.util.UUID;
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
import org.junit.Test;
import org.restcomm.cluster.CacheDataExecutorService;
import org.restcomm.cluster.CacheExecutorConfiguration;
import org.restcomm.cluster.FailOverListener;
import org.restcomm.cluster.IDGenerator;
import org.restcomm.cluster.RestcommCluster;
import org.restcomm.cluster.UUIDGenerator;
import org.restcomm.cluster.serializers.KryoSerializer;

public class FailoverCacheTest 
{
	@Test
	public void testFailover() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
	{
		Configurator.initialize(new DefaultConfiguration());
	    Configurator.setRootLevel(Level.INFO);
	    
	    EmbeddedBaseTransactionManager transactionManager=new EmbeddedBaseTransactionManager();
		
	    IDGenerator<UUID> generator=new UUIDGenerator();
		
	    CacheExecutorConfiguration configuration=new CacheExecutorConfiguration(16, 1000L, 1000L);
		InfinispanCacheFactory factory=new InfinispanCacheFactory(transactionManager, new KryoSerializer(Thread.currentThread().getContextClassLoader(),new CacheDataExecutorService(configuration, generator, Thread.currentThread().getContextClassLoader()),true),generator,Thread.currentThread().getContextClassLoader(), new CacheDataExecutorService(configuration, generator, Thread.currentThread().getContextClassLoader()), 1000, true, false, 1, true);
		RestcommCluster cluster=factory.getCluster("testf", false);
		cluster.startCluster(true);		
		
		InfinispanCacheFactory factory2=new InfinispanCacheFactory(transactionManager, new KryoSerializer(Thread.currentThread().getContextClassLoader(),new CacheDataExecutorService(configuration, generator, Thread.currentThread().getContextClassLoader()),true),generator,Thread.currentThread().getContextClassLoader(), new CacheDataExecutorService(configuration, generator, Thread.currentThread().getContextClassLoader()), 1000, true, false, 1, true);
		RestcommCluster cluster2=factory2.getCluster("testf", false);
		cluster2.startCluster(true);
		
		AtomicInteger removedCount=new AtomicInteger(0);
		FailOverListener fol=new FailOverListener() {
			
			@Override
			public void failOverClusterMember(String address) {
				removedCount.incrementAndGet();
			}
		};
		
		cluster.addFailOverListener(fol);
		
		try {
			Thread.sleep(5000);
		}		
		catch(InterruptedException ex) {
			
		}
		
		factory2.stop();
		
		try {
			Thread.sleep(10000);
		}		
		catch(InterruptedException ex) {
			
		}
		
		assertEquals(removedCount.get(),1);
		cluster.removeFailOverListener(fol);
		
		factory.stop();
	}
}