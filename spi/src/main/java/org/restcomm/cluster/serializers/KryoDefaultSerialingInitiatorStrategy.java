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

package org.restcomm.cluster.serializers;

import java.util.concurrent.Semaphore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.cluster.CacheDataExecutorService;

import com.esotericsoftware.kryo.kryo5.objenesis.instantiator.ObjectInstantiator;
import com.esotericsoftware.kryo.kryo5.objenesis.instantiator.sun.SunReflectionFactorySerializationInstantiator;
import com.esotericsoftware.kryo.kryo5.objenesis.strategy.BaseInstantiatorStrategy;
/**
 * @author yulian.oifa
 *
 */
public class KryoDefaultSerialingInitiatorStrategy extends BaseInstantiatorStrategy {
	private static final Logger logger = LogManager.getLogger(CacheDataExecutorService.class);

	private CacheDataExecutorService service;
	
	public KryoDefaultSerialingInitiatorStrategy(CacheDataExecutorService service) {
		this.service=service;
	}
	
	public <T> ObjectInstantiator<T> newInstantiatorOf(Class<T> type) {
		InitiatorCallback<T> callback=new InitiatorCallback<T>();
		InitiatorCommand<T> command=new InitiatorCommand<T>(callback, type);
		service.executeCommand(command);
		ObjectInstantiator<T> initiator = callback.awaitResult();
		if(initiator == null && command.getException()!=null)
			throw command.getException();
		
		return initiator;
	}
	
	public class InitiatorCallback<T> {

        private Semaphore semaphore = new Semaphore(0);

        private ObjectInstantiator<T> result;
        
        public void receiveResult(ObjectInstantiator<T> result) {
        	this.result = result;
            semaphore.release();            
        }
        
        public ObjectInstantiator<T> awaitResult() {
            try {
            	semaphore.acquire();            	
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
            
            return this.result;
        }
    }
	
	public class InitiatorCommand<T> implements CacheDataExecutorService.CommonCommand {
        protected InitiatorCallback<T> callback;
        private Class<T> type;
        private Boolean isCanceled = false;
        private RuntimeException exception;
        
        public InitiatorCommand(InitiatorCallback<T> callback,Class<T> type) {
            this.callback = callback;
            this.type=type;
        }
        
        public void execute() {
        	if(!isCanceled) {
        		try {
        			callback.receiveResult(new SunReflectionFactorySerializationInstantiator<>(type));           			
        		}
        		catch(RuntimeException ex) {
        			this.exception=ex;
        			callback.receiveResult(null);
        		}
        		catch(Exception ex) {
        			logger.error("An error occured while marshalling," + ex.getMessage(),ex);
        			callback.receiveResult(null);
        		}
        	}
        	else
        		callback.receiveResult(null);
        }

        public RuntimeException getException() {
        	return exception;
        }
        
        public void cancel() {
            this.isCanceled = true;
        }
    }
}