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
import org.infinispan.atomic.AtomicMapLookup;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
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
public class TreeStructureSupport {
	private static final Log log = LogFactory.getLog(TreeStructureSupport.class);
	private static final boolean trace = log.isTraceEnabled();

	public static final Boolean BOOLEAN_ELEMENT = new Boolean(true);

	protected final AdvancedCache<TreeSegment<?>, AtomicMap<Object, Object>> cache;

	public TreeStructureSupport(AdvancedCache<TreeSegment<?>, AtomicMap<Object, Object>> cache) {
		this.cache = cache;
	}

	public boolean exists(TreeSegment<?> f) {
		return exists(cache, f);
	}

	protected boolean exists(AdvancedCache<TreeSegment<?>, AtomicMap<Object, Object>> cache, TreeSegment<?> fqn) {
		return cache.containsKey(fqn);
	}

	/**
	 * @return true if created, false if this was not necessary.
	 */
	boolean createNodeInCache(TreeSegment<?> fqn) {
		return createNodeInCache(cache, fqn);
	}

	protected boolean createNodeInCache(AdvancedCache<TreeSegment<?>, AtomicMap<Object, Object>> cache, TreeSegment<?> fqn) {
		if (cache.containsKey(fqn))
			return false;
		
		getAtomicMap(cache, fqn);
		
		if (trace)
			log.tracef("Created node %s", fqn);
		
		return true;
	}

	protected AtomicMap<Object, Object> getStructure(AdvancedCache<TreeSegment<?>, AtomicMap<Object, Object>> cache, TreeSegment<?> fqn) {
		return getAtomicMap(cache, fqn);
	}

	protected final <K, V> AtomicMap<K, V> getAtomicMap(TreeSegment<?> key) {
		return AtomicMapLookup.getAtomicMap(cache, key);
	}

	protected final AtomicMap<Object, Object> getAtomicMap(AdvancedCache<TreeSegment<?>, AtomicMap<Object, Object>> cache,
			TreeSegment<?> key) {		
		return AtomicMapLookup.getAtomicMap(cache, key);
	}
}