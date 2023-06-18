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

package org.restcomm.timers.cache;

import org.restcomm.cluster.ClusteredID;
import org.restcomm.cluster.RestcommCluster;
import org.restcomm.cluster.data.ClusteredCacheData;
import org.restcomm.timers.TimerTask;
import org.restcomm.timers.TimerTaskData;

/**
 * 
 * Proxy object for timer task data management through Infinispan Cache
 * 
 * @author martins
 * @author András Kőkuti
 * @author yulian.oifa
 * 
 */

public class TimerTaskCacheData extends ClusteredCacheData<ClusteredID<?>,TimerTaskData> {	
	/**
	 * 
	 */
	//@SuppressWarnings("unchecked")
	public TimerTaskCacheData(ClusteredID<?> taskID, RestcommCluster cluster,Long maxIdleMs) {
		super(taskID,cluster,maxIdleMs);
	}

	/**
	 * Sets the task data.
	 * 
	 * @param taskData
	 */
	public void setTaskData(TimerTaskData taskData) {
		putValue(taskData);
	}

	/**
	 * Retrieves the task data
	 * @return
	 */
	public TimerTaskData getTaskData() {
		return getValue();		
	}

	/**
	 * Retrieves the {@link TimerTask} id from the specified {@link ClusteredCacheData}.
	 * 
	 * @param clusteredCacheData
	 * @return
	 *  
	 */
	public static ClusteredID<?> getTaskID(ClusteredCacheData<ClusteredID<?>,TimerTaskData> clusteredCacheData) throws IllegalArgumentException {
		return clusteredCacheData.getKey();
	}
}
