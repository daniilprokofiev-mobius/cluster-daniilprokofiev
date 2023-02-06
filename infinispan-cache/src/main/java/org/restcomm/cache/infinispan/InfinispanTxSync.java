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
import java.util.concurrent.Semaphore;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.cluster.RestcommCluster;
import org.restcomm.cluster.TreeTransactionContextThreadLocal;
import org.restcomm.cluster.TreeTxState;
import org.restcomm.cache.infinispan.tree.Node;
import org.restcomm.cluster.CacheDataExecutorService.CommonCommand;
import org.restcomm.cluster.data.ClusterOperation;
import org.restcomm.cluster.data.TreeSegment;

/**
 * @author yulian.oifa
 *
 */
public class InfinispanTxSync implements Synchronization {
	private static Logger logger = LogManager.getLogger(InfinispanCache.class);   
    
	private ConcurrentHashMap<TreeSegment<?>, TreeTxState> txChangedEntities;
	private RestcommCluster cluster;
	private InfinispanCache treeCache;
	private Transaction transaction;
	
	public InfinispanTxSync(Transaction transaction,RestcommCluster cluster, InfinispanCache treeCache,ConcurrentHashMap<TreeSegment<?>, TreeTxState> txChangedEntities) {
		this.transaction=transaction;
		this.cluster=cluster;
		this.treeCache=treeCache;
		this.txChangedEntities=txChangedEntities;
	}
	
	@Override
	public void beforeCompletion() {
		long startTime=System.currentTimeMillis();
		TreeTransactionContextThreadLocal.setTransactionContext(treeCache.getName(),transaction,null);
		Semaphore txSemaphore=new Semaphore(0);		
		CommitTXCommand commitTxCommand=new CommitTXCommand(cluster.getTransactionManager(),txSemaphore,txChangedEntities);
		treeCache.getCacheDataExecutorService().executeCommand(commitTxCommand);
		try {
			txSemaphore.acquire();
		}
		catch(InterruptedException ex) {
			
		}
		
		txChangedEntities.clear();
		cluster.updateStats(ClusterOperation.COMMIT, System.currentTimeMillis()-startTime);
	}

	@Override
	public void afterCompletion(int status) {
		switch (status) {
			case Status.STATUS_COMMITTED:
				break;
			case Status.STATUS_ROLLEDBACK:
				TreeTransactionContextThreadLocal.setTransactionContext(treeCache.getName(),transaction,null);
				txChangedEntities.clear();
				break;
			default:				
				throw new IllegalStateException("Unexpected transaction state " + status);
		}	
	}
	
	private void processChilds(Node parent,ConcurrentHashMap<TreeSegment<?>, TreeTxState> currLevel,TransactionManager txManager) {		
		Iterator<Entry<TreeSegment<?>, TreeTxState>> iterator=currLevel.entrySet().iterator();
		while(iterator.hasNext()) {
			if(parent==null) {
				//the transaction is created for each root node separately since each parent node may fall into
				//different tree bucket. as result when thread A and thread B both has root entities for buckets 1 and 2 , but
				//will process them in reversed order , then the deadlock may occur. Also this way only single root segment
				//may be locked for tree at each moment by single thread
				
				try {
					txManager.begin();
				}
				catch(Exception ex) {
					logger.error("Can not start transaction on mirror thread," + ex.getMessage(),ex);
				}
			}
			
			Entry<TreeSegment<?>, TreeTxState> currEntry=iterator.next();
			try {
				Boolean shouldContinue=true;	
				Node currObject = null;
				switch(currEntry.getValue().getOperation()) {
					case CREATE:
						currObject = treeCache.treeCreateInternal(parent, currEntry.getKey());
						break;
					case SET:
						currObject = treeCache.treePutInternal(parent, currEntry.getKey(), currEntry.getValue().getData());
						break;
					case NOOP:
						if(currEntry.getValue().getAllChilds()!=null && currEntry.getValue().getAllChilds().size()>0)
							currObject = treeCache.getNodeInternal(parent, currEntry.getKey());
						break;						
					case REMOVE_VALUE:
						if(!currEntry.getValue().createdInThisTX()) {							
							treeCache.treeRemoveValueInternal(parent, currEntry.getKey());
						}
						
						shouldContinue=false;
						break;
					case REMOVE:
					default:
						if(!currEntry.getValue().createdInThisTX()) {							
							treeCache.treeRemoveInternal(parent, currEntry.getKey());
						}
						
						shouldContinue=false;
						break;
				}
				
				if(shouldContinue) {	
					//first we need to create all the elements on level and only after that process with childs
					ConcurrentHashMap<TreeSegment<?>, TreeTxState> currMap=currEntry.getValue().getAllChilds();
					if(currMap!=null && currMap.size()>0) {
						processChilds(currObject, currMap,txManager);
					}
				}
			} catch(Exception ex) {
				logger.error("An error occured while commiting infinispan changes," + ex.getMessage(),ex);
			}
			
			if(parent==null) {
				try {
					txManager.commit();
				}
				catch(Exception ex) {
					logger.error("Can not commit transaction on mirror thread," + ex.getMessage(),ex);
					try {
						//trying to rollback so tx would not get stucked
						txManager.rollback();
					}
					catch(Exception ex1) {
						
					}
				}
			}
		}
	}
	
	private class CommitTXCommand implements CommonCommand {
		private TransactionManager txManager;
		private Semaphore releaseSemaphore;
		private ConcurrentHashMap<TreeSegment<?>, TreeTxState> txChangedEntities;
		public CommitTXCommand(TransactionManager txManager,Semaphore releaseSemaphore,ConcurrentHashMap<TreeSegment<?>, TreeTxState> txChangedEntities) {
			this.txManager=txManager;
			this.releaseSemaphore=releaseSemaphore;
			this.txChangedEntities=txChangedEntities;
		}
		
		@Override
		public void execute() {
			if(txChangedEntities.size()>0) {							
				processChilds(null, txChangedEntities, txManager);									
			}
			releaseSemaphore.release();
		}

		@Override
		public void cancel() {
			
		}
		
	}	
}