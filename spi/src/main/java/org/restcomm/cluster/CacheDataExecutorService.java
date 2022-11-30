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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.cluster.data.TreePutIfAbsentResult;
import org.restcomm.cluster.data.TreeSegment;

/**
 * 
 * @author yulian.oifa
 * 
 */
public class CacheDataExecutorService {

    private static final Logger logger = LogManager.getLogger(CacheDataExecutorService.class);

    private AtomicBoolean isRunning = new AtomicBoolean(true);
    
    //private long commandTimeout;
    private int threads;
    
    private ThreadLocal<MirrorThread> mirrorThread = new ThreadLocal<MirrorThread>();
    private ConcurrentLinkedQueue<MirrorThread> allThread = new ConcurrentLinkedQueue<MirrorThread>();
    private ConcurrentLinkedQueue<MirrorThread> availableThread = new ConcurrentLinkedQueue<MirrorThread>();
    private ClassLoader classLoader;
    private IDGenerator<?> idGenerator;
    
    public CacheDataExecutorService(CacheExecutorConfiguration config,IDGenerator<?> generator,ClassLoader classLoader) {
    	//this.commandTimeout = config.getCommandTimeout(); 
    	this.threads=config.getNumberOfThreads();
    	this.idGenerator=generator;
    	this.classLoader=classLoader;
    	for(int i=0;i<threads*10;i++) {
    		MirrorThread currThread=new MirrorThread(idGenerator.generateID());
    		currThread.setContextClassLoader(classLoader);
    		availableThread.offer(currThread);
    	}
    }

    public int getThreads() {
    	return threads;
    }
    
    public void terminate() {
        isRunning.set(false);
        MirrorThread thread=allThread.poll();
        while(thread!=null) {
        	thread.terminate();
        	thread.setCommand(null);
        	thread=allThread.poll();        	
        }                
    }
    
    public void executeCommand(CommonCommand command) {
    	MirrorThread thread = mirrorThread.get();
    	if(thread==null) {
    		thread=availableThread.poll();
    		if(thread==null) {
	    		thread=new MirrorThread(idGenerator.generateID());
	    		thread.setContextClassLoader(classLoader);	    
	    		allThread.add(thread);
    		}
    		
    		if(Thread.currentThread().getName()!=null)
    			thread.setName(Thread.currentThread().getName() + "_mirror");
    		
    		mirrorThread.set(thread);    		
    		thread.start();
    	}
    	
    	thread.setCommand(command);
    	//queue.offer(command);
    }
    
    public void initMirror() {
    	MirrorThread thread = mirrorThread.get();
    	if(thread==null) {
    		thread=new MirrorThread(idGenerator.generateID());
    		thread.setContextClassLoader(classLoader);	    
    		allThread.add(thread);
    		
    		if(Thread.currentThread().getName()!=null)
    			thread.setName(Thread.currentThread().getName() + "_mirror");
    		
    		mirrorThread.set(thread);    		
    		thread.start();
    	}
    }
    
    public void removeMirror() {
    	MirrorThread thread = mirrorThread.get();
    	if(thread!=null) {
    		thread.terminate();
        	thread.setCommand(null);
        	mirrorThread.set(null);
        	allThread.remove(thread);
    	}
    }
        
    public <T> T get(RestcommCluster cluster, Object key) {
        BlockingCallback<T> callback = new BlockingCallback<T>();
        Command<T> command = new GetCommand<T>(cluster, key, callback);
        callback.setCommand(command);
        executeCommand(command);
        return callback.awaitResult();
    }

    public <T> T treeGet(RestcommCluster cluster, TreeSegment<?> key) {
    	BlockingTreeCallback<T> callback = new BlockingTreeCallback<T>();
        TreeCommand<T> command = new TreeGetCommand<T>(cluster, key, callback);
        callback.setCommand(command);
        executeCommand(command);
        return callback.awaitResult();
    }
    
    public Boolean treePut(RestcommCluster cluster, TreeSegment<?> key, Object value) {
    	BlockingTreeCallback<Boolean> callback = new BlockingTreeCallback<Boolean>();
        TreeCommand<Boolean> command = new TreePutCommand(cluster, key, value, callback);
        callback.setCommand(command);
        executeCommand(command);
        return callback.awaitResult();
    }
    
    public TreePutIfAbsentResult treePutIfAbsent(RestcommCluster cluster, TreeSegment<?> key, Object value) {
    	BlockingTreeCallback<TreePutIfAbsentResult> callback = new BlockingTreeCallback<TreePutIfAbsentResult>();
        TreeCommand<TreePutIfAbsentResult> command = new TreePutIfAbsentCommand(cluster, key, value, callback);
        callback.setCommand(command);
        executeCommand(command);
        return callback.awaitResult();
    }
    
    public Boolean treeCreate(RestcommCluster cluster, TreeSegment<?> key) {
    	BlockingTreeCallback<Boolean> callback = new BlockingTreeCallback<Boolean>();
        TreeCommand<Boolean> command = new TreeCreateCommand(cluster, key, callback);
        callback.setCommand(command);
        executeCommand(command);
        return callback.awaitResult();
    }

    public boolean exists(RestcommCluster cluster, Object key) {
        BlockingCallback<Boolean> callback = new BlockingCallback<Boolean>();
        Command<Boolean> command = new ExistsCommand(cluster, key, callback);
        callback.setCommand(command);
        executeCommand(command);
        return callback.awaitResult();
    }

    public boolean treeExists(RestcommCluster cluster, TreeSegment<?> key) {
        BlockingTreeCallback<Boolean> callback = new BlockingTreeCallback<Boolean>();
        TreeCommand<Boolean> command = new TreeExistsCommand(cluster, key, callback);
        callback.setCommand(command);
        executeCommand(command);
        return callback.awaitResult();
    }

    public void treeRemove(RestcommCluster cluster, TreeSegment<?> key) {
        BlockingTreeCallback<Void> callback = new BlockingTreeCallback<Void>();
        TreeCommand<Void> command = new TreeRemoveCommand(cluster, key, callback);
        callback.setCommand(command);
        executeCommand(command);
        callback.awaitResult();
    }

    public void treeRemoveValue(RestcommCluster cluster, TreeSegment<?> key) {
        BlockingTreeCallback<Void> callback = new BlockingTreeCallback<Void>();
        TreeCommand<Void> command = new TreeRemoveValueCommand(cluster, key, callback);
        callback.setCommand(command);
        executeCommand(command);
        callback.awaitResult();
    }

    public <T> void put(RestcommCluster cluster, Object key, T value) {
        BlockingCallback<Void> callback = new BlockingCallback<Void>();
        Command<Void> command = new PutCommand<T>(cluster, key, value, callback);
        callback.setCommand(command);
        executeCommand(command);
        callback.awaitResult();
    }

    public <T> Boolean putIfAbsent(RestcommCluster cluster, Object key, T value) {
        BlockingCallback<Boolean> callback = new BlockingCallback<Boolean>();
        Command<Boolean> command = new PutIfAbsentCommand<T>(cluster, key, value, callback);
        callback.setCommand(command);
        executeCommand(command);
        return callback.awaitResult();
    }

    public <T> T remove(RestcommCluster cluster, Object key, Boolean returnValue) {
        BlockingCallback<T> callback = new BlockingCallback<T>();
        Command<T> command = new RemoveCommand<T>(cluster, key, returnValue, callback);
        callback.setCommand(command);
        executeCommand(command);
        return callback.awaitResult();
    }

    public List<TreeSegment<?>> treeGetChilds(RestcommCluster cluster, TreeSegment<?> key) {
    	BlockingTreeCallback<List<TreeSegment<?>>> callback = new BlockingTreeCallback<List<TreeSegment<?>>>();
        TreeCommand<List<TreeSegment<?>>> command = new TreeGetChildsCommand(cluster, key, callback);
        callback.setCommand(command);
        executeCommand(command);
        return callback.awaitResult();
    }

    public Boolean treeHasChilds(RestcommCluster cluster, TreeSegment<?> key) {
    	BlockingTreeCallback<Boolean> callback = new BlockingTreeCallback<Boolean>();
        TreeCommand<Boolean> command = new TreeHasChildsCommand(cluster, key, callback);
        callback.setCommand(command);
        executeCommand(command);
        return callback.awaitResult();
    }

    public Map<TreeSegment<?>,Object> treeGetChildrensData(RestcommCluster cluster, TreeSegment<?> key) {
    	BlockingTreeCallback<Map<TreeSegment<?>,Object>> callback = new BlockingTreeCallback<Map<TreeSegment<?>,Object>>();
        TreeCommand<Map<TreeSegment<?>,Object>> command = new TreeGetChildrenDataCommand(cluster, key, callback);
        callback.setCommand(command);
        executeCommand(command);
        return callback.awaitResult();
    }
    
    public interface CommonCommand {

        public void execute();

        public void cancel();
    }
    
    private abstract class Command<V> implements CommonCommand {

        protected RestcommCluster cluster;
        protected Object key;
        protected BlockingCallback<V> callback;
        protected boolean isCanceled;

        public Command(RestcommCluster cluster, Object key, BlockingCallback<V> callback) {
            this.cluster = cluster;
            this.key = key;
            this.callback = callback;
        }

        public abstract void execute();

        public void cancel() {
            this.isCanceled = true;
        }
    }
    
    private abstract class TreeCommand<V> implements CommonCommand {

        protected RestcommCluster cluster;
        protected TreeSegment<?> key;
        protected BlockingTreeCallback<V> callback;
        protected boolean isCanceled;

        public TreeCommand(RestcommCluster cluster, TreeSegment<?> key, BlockingTreeCallback<V> callback) {
            this.cluster = cluster;
            this.key = key;
            this.callback = callback;
        }

        public abstract void execute();

        public void cancel() {
            this.isCanceled = true;
        }
    }

    private class GetCommand<V> extends Command<V> {

        GetCommand(RestcommCluster cluster,Object key, BlockingCallback<V> callback) {
            super(cluster, key, callback);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute() {
            if (!isCanceled) {
            	V result = null;
                try {
                	result = (V) cluster.get(key,true);
                }
            	catch(Exception e) {
            		logger.error("An error occured while getting item," + e.getMessage(),e);
            	}
                
                callback.receiveResult(result);
            }
        }
    }

    private class TreeGetCommand<V> extends TreeCommand<V> {

    	TreeGetCommand(RestcommCluster cluster,TreeSegment<?> key, BlockingTreeCallback<V> callback) {
            super(cluster, key, callback);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute() {
            if (!isCanceled) {
            	V result = null;
            	try {
            		result = (V) cluster.treeGet(key, true); 
            	}
            	catch(Exception e) {
            		logger.error("An error occured while getting from tree," + e.getMessage(),e);
            	}
            	
                callback.receiveResult(result);
            }
        }
    }

    private class TreePutCommand extends TreeCommand<Boolean> {
    	Object value;
    	TreePutCommand(RestcommCluster cluster, TreeSegment<?> key, Object value, BlockingTreeCallback<Boolean> callback) {
            super(cluster, key, callback);
            this.value=value;
        }

        @Override
        public void execute() {
            if (!isCanceled) {            	
            	Boolean result = false;
            	try {
            		result = cluster.treePut(key, value, true);
            	}
            	catch(Exception e) {
            		logger.error("An error occured while putting into tree," + e.getMessage(),e);
            	}
            	
            	callback.receiveResult(result);
            }
        }
    }

    private class TreePutIfAbsentCommand extends TreeCommand<TreePutIfAbsentResult> {
    	Object value;
    	TreePutIfAbsentCommand(RestcommCluster cluster, TreeSegment<?> key, Object value, BlockingTreeCallback<TreePutIfAbsentResult> callback) {
            super(cluster, key, callback);
            this.value=value;
        }

        @Override
        public void execute() {
            if (!isCanceled) {            	
            	TreePutIfAbsentResult result = TreePutIfAbsentResult.ALREADY_EXISTS;
            	try {
            		result = cluster.treePutIfAbsent(key, value, true);
            	}
            	catch(Exception e) {
            		logger.error("An error occured while putting into tree," + e.getMessage(),e);
            	}
            	
            	callback.receiveResult(result);
            }
        }
    }
    
    private class TreeCreateCommand extends TreeCommand<Boolean> {
    	TreeCreateCommand(RestcommCluster cluster, TreeSegment<?> key, BlockingTreeCallback<Boolean> callback) {
            super(cluster, key, callback);
        }

        @Override
        public void execute() {
            if (!isCanceled) {            	
            	Boolean result = false;
            	try {
            		result = cluster.treeCreate(key, true);
            	}
            	catch(Exception e) {
            		logger.error("An error occured while putting into tree," + e.getMessage(),e);
            	}
            	
            	callback.receiveResult(result);
            }
        }
    }

    private class TreeGetChildsCommand extends TreeCommand<List<TreeSegment<?>>> {

    	TreeGetChildsCommand(RestcommCluster cluster, TreeSegment<?> key, BlockingTreeCallback<List<TreeSegment<?>>> callback) {
            super(cluster, key, callback);
        }

        @Override
        public void execute() {
            if (!isCanceled) {
            	List<TreeSegment<?>> result = null;
            	try {
            		result = cluster.getAllChilds(key, true);
            	}
            	catch(Exception e) {
            		logger.error("An error occured while getting tree childs," + e.getMessage(),e);
            	}
            	
            	if(result==null)
            		result=new ArrayList<TreeSegment<?>>();
            	
            	callback.receiveResult(result);            	
            }
        }
    }

    private class TreeHasChildsCommand extends TreeCommand<Boolean> {

    	TreeHasChildsCommand(RestcommCluster cluster, TreeSegment<?> key, BlockingTreeCallback<Boolean> callback) {
            super(cluster, key, callback);
        }

        @Override
        public void execute() {
            if (!isCanceled) {
            	Boolean result = null;
            	try {
            		result = cluster.hasChildrenData(key);
            	}
            	catch(Exception e) {
            		logger.error("An error occured while getting tree childs," + e.getMessage(),e);
            	}
            	
            	if(result==null)
            		result=false;
            	
            	callback.receiveResult(result);            	
            }
        }
    }

    private class TreeGetChildrenDataCommand extends TreeCommand<Map<TreeSegment<?>,Object>> {

    	TreeGetChildrenDataCommand(RestcommCluster cluster, TreeSegment<?> key, BlockingTreeCallback<Map<TreeSegment<?>,Object>> callback) {
            super(cluster, key, callback);
        }

        @Override
        public void execute() {
            if (!isCanceled) {
            	Map<TreeSegment<?>,Object> result = null;
            	try {
            		result = cluster.getAllChildrenData(key, true);
            	}
            	catch(Exception e) {
            		logger.error("An error occured while getting tree childs data," + e.getMessage(),e);
            	}
            	
            	if(result==null)
            		result = new HashMap<TreeSegment<?>,Object>();
            	
                callback.receiveResult(result);
            }
        }
    }

    private class ExistsCommand extends Command<Boolean> {

        ExistsCommand(RestcommCluster cluster, Object key, BlockingCallback<Boolean> callback) {
            super(cluster, key, callback);
        }

        @Override
        public void execute() {
            if (!isCanceled) {
            	boolean result = false;
                try {
                	result = cluster.exists(key,true);
                }
            	catch(Exception e) {
            		logger.error("An error occured while checking if item exists," + e.getMessage(),e);
            	}
                
                callback.receiveResult(result);
            }
        }
    }

    private class TreeExistsCommand extends TreeCommand<Boolean> {

    	TreeExistsCommand(RestcommCluster cluster, TreeSegment<?> key, BlockingTreeCallback<Boolean> callback) {
            super(cluster, key, callback);
        }

        @Override
        public void execute() {
            if (!isCanceled) {
            	boolean result = false;
            	try {
            		result = cluster.treeExists(key,true);
            	}
            	catch(Exception e) {
            		logger.error("An error occured while checking if tree item exists," + e.getMessage(),e);
            	}
            	
                callback.receiveResult(result);
            }
        }
    }

    private class TreeRemoveCommand extends TreeCommand<Void> {

    	TreeRemoveCommand(RestcommCluster cluster, TreeSegment<?> key, BlockingTreeCallback<Void> callback) {
            super(cluster, key, callback);
        }

        @Override
        public void execute() {
            if (!isCanceled) {
            	try {
            		cluster.treeRemove(key,true);
                }
            	catch(Exception e) {
            		logger.error("An error occured while removing from tree," + e.getMessage(),e);
            	}
            	
            	callback.receiveResult(null);
            }
        }
    }

    private class TreeRemoveValueCommand extends TreeCommand<Void> {

    	TreeRemoveValueCommand(RestcommCluster cluster, TreeSegment<?> key, BlockingTreeCallback<Void> callback) {
            super(cluster, key, callback);
        }

        @Override
        public void execute() {
            if (!isCanceled) {
            	try {
            		cluster.treeRemoveValue(key,true);
                }
            	catch(Exception e) {
            		logger.error("An error occured while removing from tree," + e.getMessage(),e);
            	}
            	
            	callback.receiveResult(null);
            }
        }
    }

    private class PutCommand<V> extends Command<Void> {
        private V value;

        PutCommand(RestcommCluster cluster, Object key, V value, BlockingCallback<Void> callback) {
            super(cluster, key, callback);
            this.value = value;
        }

        @Override
        public void execute() {
        	if(!isCanceled) {
        		try {
        			cluster.put(key, value, true);
        			callback.receiveResult(null);
	        	}
	        	catch(Exception e) {
	        		logger.error("An error occured while putting item," + e.getMessage(),e);
	        	}
        		
        		callback.receiveResult(null);
        	}
        }
    }
    
    private class PutIfAbsentCommand<V> extends Command<Boolean> {
        private V value;

        PutIfAbsentCommand(RestcommCluster cluster, Object key, V value, BlockingCallback<Boolean> callback) {
            super(cluster, key, callback);
            this.value = value;
        }

        @Override
        public void execute() {
        	if(!isCanceled) {
        		Boolean result=false;
        		try {
        			result=cluster.putIfAbsent(key, value, true);        			
	        	}
	        	catch(Exception e) {
	        		logger.error("An error occured while putting item," + e.getMessage(),e);
	        	}
        		
        		callback.receiveResult(result);
        	}
        }
    }

    private class RemoveCommand<V> extends Command<V> {
    	private Boolean returnValue;
    	
        RemoveCommand(RestcommCluster cluster, Object key,Boolean returnValue, BlockingCallback<V> callback) {
            super(cluster, key, callback);
            this.returnValue=returnValue;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute() {
        	if(!isCanceled) {
        		V result = null;
        		try {
        			result=(V) cluster.remove(key,true,returnValue);
        		}
	        	catch(Exception e) {
	        		logger.error("An error occured while removing item," + e.getMessage(),e);
	        	}
	        	
	        	callback.receiveResult(result);	        	
        	}
        }
    }

    private class BlockingCallback<R> {

        private CountDownLatch latch = new CountDownLatch(1);

        private R result;
        private Command<R> command;

        public void receiveResult(R result) {
        	this.result = result;
            latch.countDown();
        }

        public R awaitResult() {
            try {
                /*boolean wasReleased = latch.await(commandTimeout, TimeUnit.MILLISECONDS);
                if (!wasReleased) {
                    logger.warn(command.getClass().getSimpleName() + " didn't execute due to timeout");
                }*/
            	latch.await();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            } finally {
                command.cancel();
            }
            return this.result;
        }

        public void setCommand(Command<R> command) {
            this.command = command;
        }
    }
    
    private class BlockingTreeCallback<R> {

        private CountDownLatch latch = new CountDownLatch(1);

        private R result;
        private TreeCommand<R> command;

        public void receiveResult(R result) {
            this.result = result;
            latch.countDown();
        }

        public R awaitResult() {
            try {
                /*boolean wasReleased = latch.await(commandTimeout, TimeUnit.MILLISECONDS);
                if (!wasReleased) {
                    logger.warn(command.getClass().getSimpleName() + " didn't execute due to timeout");
                }*/
            	latch.await();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            } finally {
                command.cancel();
            }
            return this.result;
        }

        public void setCommand(TreeCommand<R> command) {
            this.command = command;
        }
    }
    
    private class MirrorThread extends Thread {
    	private Semaphore lockSemaphore=new Semaphore(0);
    	private AtomicReference<CommonCommand> command=new AtomicReference<CommonCommand>();
    	private AtomicBoolean isLocalRunning=new AtomicBoolean(true);
    	private ClusteredID<?> localID;
    	
    	public MirrorThread(ClusteredID<?> localID) {
    		this.localID=localID;
    	}
    	    	
    	public void setCommand(CommonCommand command) {
    		this.command.set(command);
    		lockSemaphore.release();
    	}
    	
    	public void terminate() {
    		isLocalRunning.set(false);
    	}
    	
    	public void run() {
    		while(isLocalRunning.get()) {
    			try {
    				lockSemaphore.acquire();
    			}
    			catch(InterruptedException ex) {
    				
    			}
    			
    			CommonCommand toExecute=command.getAndSet(null);
    			if(toExecute!=null) {
    				try {
    					toExecute.execute();
    				}
    				catch(Exception ex) {
    					logger.error("An error occured while processing cache command," + ex.getMessage(),ex);
    				}
    			}
    		}
    	}

    	@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((localID == null) ? 0 : localID.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			
			MirrorThread other = (MirrorThread) obj;
			if (localID == null) {
				if (other.localID != null)
					return false;
			} 
			else if (!localID.equals(other.localID))
				return false;
			
			return true;
		}		   	    	
    }
}