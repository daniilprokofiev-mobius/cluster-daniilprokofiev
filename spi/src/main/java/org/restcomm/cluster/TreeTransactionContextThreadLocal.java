package org.restcomm.cluster;
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

import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.Synchronization;
import javax.transaction.Transaction;

import org.restcomm.cluster.data.TreeSegment;

/**
 * @author yulian.oifa
 *
 */
public class TreeTransactionContextThreadLocal {
	/**
	 * 
	 */
	private static final ThreadLocal<ConcurrentHashMap<Transaction, ConcurrentHashMap<String, ConcurrentHashMap<TreeSegment<?>, TreeTxState>>>> transactionContext = new ThreadLocal<>();
	private static final ThreadLocal<ConcurrentHashMap<Transaction, ConcurrentHashMap<String, Synchronization>>> syncContext = new ThreadLocal<>();

	/**
	 * 
	 * @param name
	 * @param tx
	 * @param value
	 */
	public static void setTransactionContext(String name, Transaction tx,ConcurrentHashMap<TreeSegment<?>, TreeTxState> value) {
		ConcurrentHashMap<Transaction, ConcurrentHashMap<String, ConcurrentHashMap<TreeSegment<?>, TreeTxState>>> currMap = transactionContext.get();
		if (currMap == null) {
			currMap = new ConcurrentHashMap<>();
			transactionContext.set(currMap);
		}
		
		ConcurrentHashMap<String, ConcurrentHashMap<TreeSegment<?>, TreeTxState>> txMap = currMap.get(tx);
		if (value != null) {
			if (txMap == null) {
				txMap = new ConcurrentHashMap<>();
				ConcurrentHashMap<String, ConcurrentHashMap<TreeSegment<?>, TreeTxState>> oldMap = currMap.putIfAbsent(tx, txMap);
				if (oldMap != null)
					txMap = oldMap;
			}
			
			txMap.put(name, value);					
		} else {
			if (txMap == null)
				return;
			
			txMap.remove(name);
			if(txMap.size()==0)
				currMap.remove(tx);
		}
	}

	public static ConcurrentHashMap<TreeSegment<?>, TreeTxState> getTransactionContext(String name, Transaction tx) {
		ConcurrentHashMap<Transaction, ConcurrentHashMap<String, ConcurrentHashMap<TreeSegment<?>, TreeTxState>>> currMap = transactionContext.get();
		if (currMap == null)
			return null;

		ConcurrentHashMap<String, ConcurrentHashMap<TreeSegment<?>, TreeTxState>> innerMap = currMap.get(tx);
		if (innerMap == null)
			return null;
		
		return innerMap.get(name);
	}
	
	public static ConcurrentHashMap<String, ConcurrentHashMap<TreeSegment<?>, TreeTxState>> getCompleteMap(Transaction tx) {
		ConcurrentHashMap<Transaction, ConcurrentHashMap<String, ConcurrentHashMap<TreeSegment<?>, TreeTxState>>> currMap = transactionContext.get();
		if (currMap == null)
			return null;
		
		return currMap.get(tx);
	}
	
	public static Synchronization getSync(String name, Transaction tx) {
		ConcurrentHashMap<Transaction,ConcurrentHashMap<String, Synchronization>> currMap=syncContext.get();
		if(currMap==null)
			return null;
		
		ConcurrentHashMap<String, Synchronization> innerMap=currMap.get(tx);
		if(innerMap==null)
			return null;
		
		return innerMap.get(name);
	}
	
	public static void setSync(String name,Synchronization sync, Transaction tx) {
		ConcurrentHashMap<Transaction, ConcurrentHashMap<String, Synchronization>> currMap = syncContext.get();
		if (currMap == null) {
			currMap = new ConcurrentHashMap<>();
			syncContext.set(currMap);
		}
		
		ConcurrentHashMap<String, Synchronization> txMap = currMap.get(tx);
		if (sync != null) {
			if (txMap == null) {
				txMap = new ConcurrentHashMap<>();
				ConcurrentHashMap<String, Synchronization> oldMap = currMap.putIfAbsent(tx, txMap);
				if (oldMap != null)
					txMap = oldMap;
			}
			
			txMap.put(name, sync);					
		} else {
			if (txMap == null)
				return;
			
			txMap.remove(name);
			if(txMap.size()==0)
				currMap.remove(tx);
		}		
	}
}
