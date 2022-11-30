package org.restcomm.cache.infinispan;
import java.io.Externalizable;

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
import javax.transaction.TransactionManager;

import org.infinispan.manager.DefaultCacheManager;
import org.restcomm.cluster.CacheDataExecutorService;
import org.restcomm.cluster.DefaultClusterElector;
import org.restcomm.cluster.IDGenerator;
import org.restcomm.cluster.RestcommCacheFactory;
import org.restcomm.cluster.RestcommCluster;
import org.restcomm.cluster.data.ExternalizableExternalizer;
import org.restcomm.cluster.serializers.GenericJbossMarshaller;
import org.restcomm.cluster.serializers.Serializer;

/**
 * @author yulian.oifa
 */
public class InfinispanCacheFactory implements RestcommCacheFactory 
{
	private DefaultCacheManager jBossCacheContainer;
	private CacheDataExecutorService cacheExecutorService;
    private ClassLoader classLoader;
    private TransactionManager transactionManager;
	private DefaultClusterElector elector;
	private ViewChangedListener viewChangedListener;
	private IDGenerator<?> idGenerator;
	private Boolean logStats;
	
	public InfinispanCacheFactory(TransactionManager transactionManager,Serializer serializer,IDGenerator<?> idGenerator, ClassLoader classLoader,CacheDataExecutorService service,Integer aquireTimeout,Boolean isReplicated, Boolean paritioned, Integer copies,Boolean logStats) {
		this.cacheExecutorService=service;
		this.classLoader=classLoader;
		
		this.transactionManager = transactionManager;
		this.elector = new DefaultClusterElector();
		this.idGenerator=idGenerator;
		this.logStats=logStats;
		
		final TransactionManager txMgr=transactionManager;
		if(this.jBossCacheContainer==null) {
			this.jBossCacheContainer=InfinispanCache.initContainer(txMgr,serializer, isReplicated, paritioned, copies, aquireTimeout);
			this.viewChangedListener=new ViewChangedListener();
			this.jBossCacheContainer.addListener(viewChangedListener);
		}
	}
	
	public void registerKnownClass(int id, Class<? extends Externalizable> knownClass) {
		this.jBossCacheContainer.getGlobalComponentRegistry().getGlobalConfiguration().serialization().advancedExternalizers().put(GenericJbossMarshaller.FIRST_ID+id,new ExternalizableExternalizer(knownClass,GenericJbossMarshaller.FIRST_ID + id));		    	
    }
	
	public void removeCluster(String name) {		
	}

	public RestcommCluster getCluster(String name,Boolean isTree) {
		if(name.equals(RestcommCluster.CONNECTED_CLIENT))
			throw new RuntimeException("The name is reserved");
		
		InfinispanCache localCache;
		if(isTree)
			localCache = new InfinispanCache(name, jBossCacheContainer, transactionManager, classLoader, cacheExecutorService, idGenerator, isTree, logStats);
		else
			localCache = new InfinispanCache(name, jBossCacheContainer, transactionManager, classLoader, cacheExecutorService, idGenerator, isTree, logStats);
		
		return new InfinispanCluster(localCache, viewChangedListener, transactionManager, elector, logStats);		
	}
	
    public void stop() {
    	if(this.jBossCacheContainer!=null) {
			this.jBossCacheContainer.stop();
			this.jBossCacheContainer=null;
		}
    	
		if(this.cacheExecutorService!=null) {
		    this.cacheExecutorService.terminate();
		    this.cacheExecutorService = null;
		}
    }		
}
