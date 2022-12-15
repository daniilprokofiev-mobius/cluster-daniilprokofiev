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