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

import java.util.List;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.commons.api.Lifecycle;
import org.infinispan.context.Flag;
import org.restcomm.cluster.AsyncCacheCallback;
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
public interface TreeCache extends Lifecycle {
	/**
	 * A convenience method to retrieve a node directly from the cache. Equivalent
	 * to calling cache.getRoot().getChild(fqn).
	 *
	 * @param fqn fqn of the node to retrieve
	 * @return a Node object, or a null if the node does not exist.
	 * @throws IllegalStateException if the cache is not in a started state
	 */
	Node getNode(TreeSegment<?> fqn);

	Node getNode(TreeSegment<?> fqn, Flag... flags);
	
	/**
	 * A convenience method to retrieve a node directly from the cache. Equivalent
	 * to calling cache.getRoot().getChild(fqn).
	 *
	 * @param fqn fqn of the node to retrieve
	 * @throws IllegalStateException if the cache is not in a started state
	 */
	void getNodeAsync(TreeSegment<?> fqn, AsyncCacheCallback<Node> callback);

	void getNodeAsync(TreeSegment<?> fqn, AsyncCacheCallback<Node> callback, Flag... flags);
	
	/**
	 * @return a reference to the underlying cache instance
	 */
	Cache<TreeSegment<?>, AtomicMap<Object,Object>> getCache();

	/**
	 * Tests if an Fqn exists.
	 *
	 * @param fqn Fqn to test
	 * @return true if the fqn exists, false otherwise
	 */
	boolean exists(TreeSegment<?> fqn);

	boolean exists(TreeSegment<?> fqn, Flag... flags);
	
	/**
	 * Tests if an Fqn exists.
	 *
	 * @param fqn Fqn to test
	 */
	void existsAsync(TreeSegment<?> fqn, AsyncCacheCallback<Boolean> callback);

	void existsAsync(TreeSegment<?> fqn, AsyncCacheCallback<Boolean> callback, Flag... flags);
	
	/**
	 * Root node is the main lock point, therefore it is recommended to segment it.This method returns hash code 
	 * based segment for root. 
	 *
	 * @param fqn Fqn to test
	 * @return true if the fqn exists, false otherwise
	 */
	TreeSegment<?> getRootSegment(int hashCode);
	
	List<TreeSegment<?>> getAllRootSegment();
}