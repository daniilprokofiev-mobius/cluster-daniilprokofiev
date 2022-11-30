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

package org.restcomm.cluster;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.transaction.TransactionManager;

import org.restcomm.cluster.data.ClusterOperation;
import org.restcomm.cluster.data.TreePutIfAbsentResult;
import org.restcomm.cluster.data.TreeSegment;

/**
 * 
 * @author martins
 * @author András Kőkuti
 * @author yulian.oifa
 *
 */
public interface RestcommCluster {
	public static final String CONNECTED_CLIENT="--m--";
	
	/**
	 * Adds the specified fail over listener.
	 * @param listener
	 */
	public boolean addFailOverListener(FailOverListener listener);
	
	/**
	 * Removes the specified fail over listener.
	 * @param listener
	 * @return
	 */
	public boolean removeFailOverListener(FailOverListener listener);
	
	/**
	 * Adds the specified data removal listener.
	 * @param listener
	 */
	public boolean addDataRemovalListener(DataRemovalListener listener);
	
	/**
	 * Removes the specified data removal listener.
	 * @param listener
	 * @return
	 */
	public boolean removeDataRemovalListener(DataRemovalListener listener);
	
	/**
	 * Adds the specified data listener.
	 * @param listener
	 */
	public boolean addDataListener(DataListener listener);
	
	/**
	 * Removes the specified data listener.
	 * @param listener
	 * @return
	 */
	public boolean removeDataListener(DataListener listener);
	
	/**
	 * Retrieves the local address of the cluster node.
	 * @return
	 */
	public String getLocalAddress();
	
	
	/**
     * Indicates if the cache is not in a cluster environment.
     *
     * @return the localMode
     */
    public boolean isLocalMode();
    
	/**
	 * Method to determine if this node is single node in the cluster.
	 * 
	 * @return <ul>
	 *         <li><b>true</b> - cache mode is local || clusterMembers == 1
	 *         <li>
	 *         <li><b>false</b> - otherwise
	 *         <li>
	 *         </ul>
	 */
	public boolean isSingleMember();
	
	/**
	 * Starts the cluster. This should only be invoked when all listeners are
	 * added, and when all classes needed to deserialize data in a running
	 * cluster are visible (somehow).
	 */
	public void startCluster(Boolean useRemovalOnlyListener);
	
	/**
	 * Indicates if the cluster is running or not.
	 * @return
	 */
	public boolean isStarted();
	
	/**
	 * Stops the cluster.
	 */
	public void stopCluster();
	
	/**
	 * Retreived cached value from cache.
	 * @return
	 */
	public Object treeGet(TreeSegment<?> key,Boolean ignoreRollbackState);
	
	/**
	 * Validates if element exists in cache.
	 * @return
	 */
	public Boolean treeExists(TreeSegment<?> key,Boolean ignoreRollbackState);
	
	/**
	 * Removes the element from cache.
	 * @return
	 */
	public void treeRemove(TreeSegment<?> key,Boolean ignoreRollbackState);
	
	/**
	 * Removes the value from cache.
	 * @return
	 */
	public void treeRemoveValue(TreeSegment<?> key,Boolean ignoreRollbackState);
	
	/**
	 * Stores value to cache.
	 * @return
	 */
	public Boolean treePut(TreeSegment<?> key,Object value,Boolean ignoreRollbackState);
		
	/**
	 * Stores value to cache if the parent exists and child doesnt.
	 * @return
	 */
	public TreePutIfAbsentResult treePutIfAbsent(TreeSegment<?> key,Object value,Boolean ignoreRollbackState);
		
	/**
	 * Create a tree element.
	 * @return
	 */
	public Boolean treeCreate(TreeSegment<?> key,Boolean ignoreRollbackState);
		
	/**
	 * Multi operation to put multiple childs into tree in one op.
	 * @return
	 */
	public Boolean treeMulti(Map<TreeSegment<?>,Object> putItems,Boolean createParent,Boolean ignoreRollbackState);
	
	/**
	 * When created out of op and then op resumed/created we may use this method to preload the data without going to cache
	 * @return
	 */
	public void treeMarkAsPreloaded(Map<TreeSegment<?>,Object> putItems);
	
	/**
	 * Returns all child element from cache.
	 * @return
	 */
	public List<TreeSegment<?>> getAllChilds(TreeSegment<?> key,Boolean ignoreRollbackState);
	
	/**
	 * Returns all child element from cache.
	 * @return
	 */
	public List<TreeSegment<?>> getChildren(TreeSegment<?> key);
	
	/**
	 * Returns all values assigned for current element from cache.
	 * @return
	 */
	public Map<TreeSegment<?>,Object> getAllChildrenData(TreeSegment<?> key,Boolean ignoreRollbackState);
	
	/**
	 * Returns all values assigned for current element from cache.
	 * @return
	 */
	public Map<TreeSegment<?>,Object> getChildrenData(TreeSegment<?> key);
	
	/**
	 * Returns whether element has values assigned in cache.
	 * @return
	 */
	public Boolean hasChildrenData(TreeSegment<?> key);
	
	/**
	 * Preloads the element children into tx cache.
	 * @return
	 */
	public void treePreload(TreeSegment<?> key);
	
	/**
	 * Returns whether children has been preloaded already into tx cache.
	 * @return
	 */
	public Boolean treeIsPreloaded(TreeSegment<?> key);
	
	/**
	 * Retreived cached value from cache.
	 * @return
	 */
	public Object get(Object key,Boolean ignoreRollbackState);
	
	/**
	 * Validates if key exists in cache.
	 * @return
	 */
	public Boolean exists(Object key,Boolean ignoreRollbackState);
	
	/**
	 * Removes the value from cache.
	 * @return
	 */
	public Object remove(Object key,Boolean ignoreRollbackState,Boolean returnValue);
	
	/**
	 * Stores value to cache.
	 * @return
	 */
	public void put(Object key,Object value,Boolean ignoreRollbackState);
		
	/**
	 * Stores value to cache if key not present.
	 * @return whether the operation succeeded
	 */
	public Boolean putIfAbsent(Object key,Object value,Boolean ignoreRollbackState);
		
	/**
	 * Returns all keys from cache.
	 * @return
	 */
	public Set<?> getAllKeys();
	
	/**
	 * Returns all elements from cache.
	 * @return
	 */
	public Map<?,?> getAllElements();
	
	/**
	 * Returns transaction manager used with this cache.
	 * @return
	 */
	public TransactionManager getTransactionManager();
	
	/**
	 * update the operation statistics
	 */
	void updateStats(ClusterOperation operation,long time);
}