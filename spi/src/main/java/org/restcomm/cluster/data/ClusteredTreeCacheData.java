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

package org.restcomm.cluster.data;


import org.restcomm.cluster.AsyncCacheCallback;
import org.restcomm.cluster.RestcommCluster;

/**
 * 
 * Abstract class for a clustered tree data.
 * 
 * @author yulian.oifa
 *
 */
public class ClusteredTreeCacheData<T,V> {
	private TreeSegment<T> key;
    private RestcommCluster cluster;
    
	/**
	 * @param key
	 * @param cluster
	 */
	public ClusteredTreeCacheData(TreeSegment<T> key, RestcommCluster cluster) {
		this.key=key;
		this.cluster=cluster;
	}
	
	@SuppressWarnings("unchecked")
	public V getTreeValue() {
		return (V)cluster.treeGet(key,false);		
	}
	
	@SuppressWarnings("unchecked")
	public void getTreeValueAsync(AsyncCacheCallback<V> callback) {
		cluster.treeGetAsync(key,(AsyncCacheCallback<Object>) callback);		
	}
	
	public Boolean putValue(V value) {
		return cluster.treePut(key, value,false);		
	}
	
	public void putValueAsync(V value,AsyncCacheCallback<Boolean> callback) {
		cluster.treePutAsync(key, value, callback);		
	}
	
	public TreePutIfAbsentResult putValueIfAbsent(V value) {
		return cluster.treePutIfAbsent(key, value,false);		
	}
	
	public void putValueIfAbsentAsync(V value, AsyncCacheCallback<TreePutIfAbsentResult> callback) {
		cluster.treePutIfAbsentAsync(key, value, callback);		
	}
	
	public Boolean create() {
		return cluster.treeCreate(key, false);		
	}
	
	public void createAsync(AsyncCacheCallback<Boolean> callback) {
		cluster.treeCreateAsync(key, callback);		
	}
	
	public void removeElement() {
		cluster.treeRemove(key,false);		
	}
	
	public void removeElementAsync(AsyncCacheCallback<Void> callback) {
		cluster.treeRemoveAsync(key,callback);
	}
	
	public void removeValue() {
		cluster.treeRemoveValue(key,false);		
	}
	
	public void removeValueAsync(AsyncCacheCallback<Void> callback) {
		cluster.treeRemoveValueAsync(key, callback);
	}
	
	public TreeSegment<T> getKey() {
        return this.key;
    }

    public Boolean exists() {
        return cluster.treeExists(key,false);
    }

    public void existsAsync(AsyncCacheCallback<Boolean> callback) {
        cluster.treeExistsAsync(key, callback);
    }
    
    public void preload() {
    	cluster.treePreload(key);
    }
    
    public Boolean isPreloaded() {
    	return cluster.treeIsPreloaded(key);
    }
}