package org.restcomm.cluster;
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
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.Synchronization;
import javax.transaction.Transaction;

/**
 * @author yulian.oifa
 */
public class TransactionContextThreadLocal {
	private static final ThreadLocal<ConcurrentHashMap<Transaction, ConcurrentHashMap<String, ConcurrentHashMap<Object, TxState>>>> transactionContext = new ThreadLocal<>();
	private static final ThreadLocal<ConcurrentHashMap<Transaction, ConcurrentHashMap<String, Synchronization>>> syncContext = new ThreadLocal<>();

	public static void setTransactionContext(String name, Transaction tx,ConcurrentHashMap<Object, TxState> value) {
		ConcurrentHashMap<Transaction, ConcurrentHashMap<String, ConcurrentHashMap<Object, TxState>>> currMap = transactionContext.get();
		if (currMap == null) {
			currMap = new ConcurrentHashMap<>();
			transactionContext.set(currMap);
		}
		
		ConcurrentHashMap<String, ConcurrentHashMap<Object, TxState>> txMap = currMap.get(tx);
		if (value != null) {
			if (txMap == null) {
				txMap = new ConcurrentHashMap<>();
				ConcurrentHashMap<String, ConcurrentHashMap<Object, TxState>> oldMap = currMap.putIfAbsent(tx, txMap);
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

	public static ConcurrentHashMap<Object, TxState> getTransactionContext(String name, Transaction tx) {
		ConcurrentHashMap<Transaction, ConcurrentHashMap<String, ConcurrentHashMap<Object, TxState>>> currMap = transactionContext.get();
		if (currMap == null)
			return null;

		ConcurrentHashMap<String, ConcurrentHashMap<Object, TxState>> innerMap = currMap.get(tx);
		if (innerMap == null)
			return null;
		
		return innerMap.get(name);
	}
	
	public static ConcurrentHashMap<String, ConcurrentHashMap<Object, TxState>> getCompleteMap(Transaction tx) {
		ConcurrentHashMap<Transaction, ConcurrentHashMap<String, ConcurrentHashMap<Object, TxState>>> currMap = transactionContext.get();
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