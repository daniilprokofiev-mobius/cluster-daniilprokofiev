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

import org.infinispan.AdvancedCache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.commons.CacheConfigurationException;
import org.restcomm.cluster.IDGenerator;
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
public class TreeCacheFactory {

	/**
	 * Creates a TreeCache instance by taking in a {@link org.infinispan.Cache} as a
	 * parameter
	 *
	 * @param cache
	 * @return instance of a {@link TreeCache}
	 * @throws NullPointerException        if the cache parameter is null
	 * @throws CacheConfigurationException if the invocation batching configuration
	 *                                     is not enabled.
	 */

	public TreeCache createTreeCache(AdvancedCache<TreeSegment<?>, AtomicMap<Object,Object>> cache,Integer segments,IDGenerator<?> generator,Boolean logStats) {

		// Validation to make sure that the cache is not null.

		if (cache == null) {
			throw new NullPointerException("The cache parameter passed in is null");
		}

		// If invocationBatching is not enabled, throw a new configuration exception.
		if (!cache.getCacheConfiguration().invocationBatching().enabled()) {
			throw new CacheConfigurationException("invocationBatching is not enabled for cache '" + cache.getName()
					+ "'. Make sure this is enabled by"
					+ " calling configurationBuilder.invocationBatching().enable()");
		}

		return new TreeCacheImpl(cache,segments,generator,logStats);
	}
}