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

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.SerializerFactory.BaseSerializerFactory;
import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer;
import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.FieldSerializerConfig;
/**
 * @author yulian.oifa
 *
 */
public class KryoSerializerFactory extends BaseSerializerFactory<FieldSerializer<?>> {
	private final FieldSerializerConfig config;
	private CacheDataExecutorService service;
	
	private static final Logger logger = LogManager.getLogger(CacheDataExecutorService.class);

	public KryoSerializerFactory (CacheDataExecutorService service) {
		this.service=service;
		this.config = new FieldSerializerConfig();
	}

	public KryoSerializerFactory(FieldSerializerConfig config,CacheDataExecutorService service) {
		this.config = config;
		this.service = service;
	}

	public FieldSerializerConfig getConfig () {
		return config;
	}
	
	@SuppressWarnings("rawtypes")
	public FieldSerializer<?> newSerializer (Kryo kryo, Class type) {
		FactoryCallback callback=new FactoryCallback();
		FactoryCommand command=new FactoryCommand(kryo, callback, type);
		service.executeCommand(command);
		return callback.awaitResult();
	}
	
	public class FactoryCallback {

        private Semaphore semaphore = new Semaphore(0);

        private FieldSerializer<?> result;
        
        public void receiveResult(FieldSerializer<?> result) {
        	this.result = result;
            semaphore.release();            
        }
        
        public FieldSerializer<?> awaitResult() {
            try {
            	semaphore.acquire();            	
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
            
            return this.result;
        }
    }
	
	public class FactoryCommand implements CacheDataExecutorService.CommonCommand {
        protected FactoryCallback callback;
        private Class<?> type;
        private Boolean isCanceled = false;
        private Kryo kryo;
        
        public FactoryCommand(Kryo kryo,FactoryCallback callback,Class<?> type) {
            this.callback = callback;
            this.type=type;
            this.kryo=kryo;
        }
        
        @SuppressWarnings("rawtypes")
		public void execute() {
        	if(!isCanceled) {
        		try {
        			callback.receiveResult(new FieldSerializer(kryo, type, config.clone()));           			
        		}
        		catch(Exception ex) {
        			logger.error(ex.getMessage(), ex);
        			callback.receiveResult(null);
        		}
        	}
        	else
        		callback.receiveResult(null);
        }

        public void cancel() {
            this.isCanceled = true;
        }
    }
}