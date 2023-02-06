/*
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * Copyright 2022-2023, Mobius Software LTD. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.cluster.data;


import org.restcomm.cluster.AsyncCacheCallback;
import org.restcomm.cluster.RestcommCluster;

/**
 * 
 * Abstract class for a clustered data.
 * 
 * @author martins
 * @author András Kőkuti
 * @author yulian.oifa
 *
 */
public class ClusteredCacheData<K,V> {
	private K key;
    private RestcommCluster cluster;
    
	/**
	 * @param key
	 * @param cluster
	 */
	public ClusteredCacheData(K key, RestcommCluster cluster) {
		this.key=key;
		this.cluster=cluster;
	}
	
	@SuppressWarnings("unchecked")
	public V getValue() {
		return (V)cluster.get(key,false);		
	}
	
	@SuppressWarnings("unchecked")
	public void getValueAsync(AsyncCacheCallback<V> callback) {
		cluster.getAsync(key,(AsyncCacheCallback<Object>) callback);		
	}
	
	public void putValue(V value) {
		cluster.put(key, value,false);		
	}
	
	public void putValueAsync(V value,AsyncCacheCallback<Void> callback) {
		cluster.putAsync(key, value, callback);	
	}
	
	public Boolean putIfAbsent(V value) {
		return cluster.putIfAbsent(key, value, false);		
	}
	
	public void putIfAbsentAsync(V value,AsyncCacheCallback<Boolean> callback) {
		cluster.putIfAbsentAsync(key, value, callback);		
	}
	
	@SuppressWarnings("unchecked")
	public V removeElement() {
	    return (V)cluster.remove(key,false,true);
	}
	
	@SuppressWarnings("unchecked")
	public void removeElementAsync(AsyncCacheCallback<V> callback) {
	    cluster.removeAsync(key,true,(AsyncCacheCallback<Object>) callback);
	}
	
	public void deleteElement() {
	    cluster.remove(key,false,false);
	}
	
	public void deleteElementAsync(AsyncCacheCallback<Object> callback) {
	    cluster.removeAsync(key,false,callback);
	}
	
	public K getKey() {
        return this.key;
    }

    public Boolean exists() {
        return cluster.exists(key,false);
    }

    public void existsAsync(AsyncCacheCallback<Boolean> callback) {
        cluster.existsAsync(key,callback);
    }
}