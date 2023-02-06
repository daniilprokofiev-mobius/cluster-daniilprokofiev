/*
 * Copyright 2022-2023, Mobius Software LTD. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.restcomm.cache.infinispan;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.restcomm.cluster.data.CacheListener;
import org.restcomm.cluster.data.TreeListener;
/**
 * @author yulian.oifa
 *
 */

@Listener
public class ViewChangedListener {
	private static final Logger logger = LogManager.getLogger(ViewChangedListener.class);

	private ConcurrentHashMap<String,CacheListener> listeners=new ConcurrentHashMap<String,CacheListener>();
	private ConcurrentHashMap<String,TreeListener> treeListeners=new ConcurrentHashMap<String,TreeListener>();
	
	public void registerListener(String name,CacheListener listener) {
		if(listener==null)
			listeners.remove(name);
		else
			listeners.put(name, listener);
	}
	
	public void registerTreeListener(String name,TreeListener listener) {
		if(listener==null)
			treeListeners.remove(name);
		else
			treeListeners.put(name, listener);
	}
	
	/**
	 * Method handle a change on the cluster members set
	 * @param event
	 */
	@ViewChanged
	public synchronized void viewChanged(ViewChangedEvent event) {	
		if (logger.isDebugEnabled()) {
			logger.debug("onViewChangeEvent : id[" + event.getViewId() + "] : event local address[" + event.getLocalAddress() + "]");
		}
		
		Iterator<Entry<String, CacheListener>> iterator=listeners.entrySet().iterator();
		while(iterator.hasNext())
			iterator.next().getValue().viewChanged();
		
		Iterator<Entry<String, TreeListener>> treeIterator=treeListeners.entrySet().iterator();
		while(treeIterator.hasNext())
			treeIterator.next().getValue().treeViewChanged();		
	}
}
