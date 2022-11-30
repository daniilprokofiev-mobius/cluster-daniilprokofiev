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

package org.restcomm.cache.infinispan.tree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.commons.CacheException;
import org.infinispan.context.Flag;
import org.restcomm.cluster.IDGenerator;
import org.restcomm.cluster.data.RootTreeSegment;
import org.restcomm.cluster.data.TreeSegment;
/*
 * This is modified copy of infinispan implementation for tree
 * following changes are done
 * minimzed support for operations -> some are not required for the project
 * number of root nodes is configurable. This is because the root node becomes
 * main botleneck for the perfomance
 * The number of nodes per tree has been decreased from 2 to 1 
 * This implementation supports either data nodes or tree nodes while infinispan implementation
 * supported mixed mode
 */
/**
 * @author yulian.oifa
 */
public class TreeCacheImpl extends TreeStructureSupport implements TreeCache {
	private List<TreeSegment<?>> rootSegments;
	private Integer segments;
	private ConcurrentHashMap<Integer,AtomicInteger> hitCount=new ConcurrentHashMap<Integer,AtomicInteger>(); 
	private IDGenerator<?> idGenerator;
	private Boolean logStats;
	private static Logger logger = LogManager.getLogger(TreeCacheImpl.class);

	public TreeCacheImpl(AdvancedCache<TreeSegment<?>, AtomicMap<Object,Object>> cache,Integer segments,IDGenerator<?> idGenerator,Boolean logStats) {
		super(cache);
		if (cache.getCacheConfiguration().indexing().index().isEnabled())
			throw new CacheConfigurationException(
					"TreeCache cannot be used with a Cache instance configured to use indexing!");
		
		this.segments=segments;
		this.idGenerator=idGenerator;
		this.logStats=logStats;
		createRoot();
	}

	@Override
	public Node getNode(TreeSegment<?> fqn) {
		return getNode(cache, fqn);
	}

	@Override
	public Node getNode(TreeSegment<?> fqn, Flag... flags) {
		return getNode(cache.withFlags(flags), fqn);
	}

	private Node getNode(AdvancedCache<TreeSegment<?>, AtomicMap<Object,Object>> cache, TreeSegment<?> fqn) {
		if(fqn.isRoot())
			return new NodeImpl(this, fqn, cache);
		
		if (exists(cache, fqn))
			return new NodeImpl(this, fqn, cache);
		else
			return null;
	}

	@Override
	public boolean exists(TreeSegment<?> fqn, Flag... flags) {
		if(fqn.isRoot())
			return true;
		
		return exists(cache.withFlags(flags), fqn);
	}

	@Override
	public Cache<TreeSegment<?>, AtomicMap<Object,Object>> getCache() {
		// Retrieve the advanced cache as a way to retrieve
		// the cache behind the cache adapter.
		return cache.getAdvancedCache();
	}

	// ------------------ nothing different; just delegate to the cache
	@Override
	public void start() throws CacheException {
		cache.start();
		createRoot();
	}

	@Override
	public void stop() {
		if(logStats!=null && logStats) {
			for(int i=0;i<segments;i++) {
				logger.info("Hit count for " + cache.getName() + " bucket [" + (i+1) + "] is " + hitCount.get(i).get());
			}
		}
		
		cache.stop();		
	}
	
	private void createRoot() {
		if (!exists(RootTreeSegment.INSTANCE))
			createNodeInCache(RootTreeSegment.INSTANCE);
			
		for(int i=0;i<segments;i++)
			hitCount.put(i, new AtomicInteger(0));
		
		AtomicMap<Object, Object> rootSegmentsMap=getStructure(cache,RootTreeSegment.INSTANCE);
		Iterator<Object> iterator=rootSegmentsMap.keySet().iterator();
		rootSegments=new ArrayList<TreeSegment<?>>(segments);
		while(iterator.hasNext())
			rootSegments.add(RootTreeSegment.INSTANCE.getChild(iterator.next()));
		
		while(rootSegments.size()<segments) {
			TreeSegment<?> f=RootTreeSegment.INSTANCE.getChild(idGenerator.generateID());
			rootSegmentsMap.put(f.getSegment(), BOOLEAN_ELEMENT);
			createNodeInCache(cache, f);
			rootSegments.add(f);
			
			//warmup the segment to be capable of handling the load
			Node currNode=getNode(f);
			List<TreeSegment<?>> childs=new ArrayList<TreeSegment<?>>(1000);
			for(int i=0;i<1000;i++) {
				TreeSegment<?> currChild=f.getChild(idGenerator.generateID());
				currNode.addChild(currChild, true);
				childs.add(currChild);				
			}
			
			for(TreeSegment<?> currSegment:childs) {
				currNode.removeChild(currSegment);
			}
		}
	}

	public String toString() {
		return cache.toString();
	}

	@Override
	public TreeSegment<?> getRootSegment(int hashCode) {
		int segmentID=(hashCode & Integer.MAX_VALUE)%rootSegments.size();
		hitCount.get(segmentID).incrementAndGet();		
		return rootSegments.get(segmentID);		
	}

	@Override
	public List<TreeSegment<?>> getAllRootSegment() {
		return rootSegments;
	}
}