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

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.concurrent.Semaphore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.cluster.CacheDataExecutorService;

import com.esotericsoftware.kryo.kryo5.KryoException;
import com.esotericsoftware.kryo.kryo5.objenesis.instantiator.ObjectInstantiator;
import com.esotericsoftware.kryo.kryo5.objenesis.strategy.InstantiatorStrategy;
import com.esotericsoftware.kryo.kryo5.reflectasm.ConstructorAccess;
import com.esotericsoftware.kryo.kryo5.util.Util;
/**
 * @author yulian.oifa
 *
 */
public class KryoDefaultInitiatorStrategy implements InstantiatorStrategy {
	private static final Logger logger = LogManager.getLogger(CacheDataExecutorService.class);
	private CacheDataExecutorService service;
	
	
	private InstantiatorStrategy fallbackStrategy;

	public KryoDefaultInitiatorStrategy (CacheDataExecutorService service) {
		this.service=service;
	}

	public KryoDefaultInitiatorStrategy (CacheDataExecutorService service,InstantiatorStrategy fallbackStrategy) {
		this.fallbackStrategy = fallbackStrategy;
		this.service=service;
	}

	public void setFallbackInstantiatorStrategy (final InstantiatorStrategy fallbackStrategy) {
		this.fallbackStrategy = fallbackStrategy;
	}

	public InstantiatorStrategy getFallbackInstantiatorStrategy () {
		return fallbackStrategy;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public ObjectInstantiator newInstantiatorOf (final Class type) {
		if (!Util.isAndroid) {
			// Use ReflectASM if the class is not a non-static member class.
			Class enclosingType = type.getEnclosingClass();
			boolean isNonStaticMemberClass = enclosingType != null && type.isMemberClass()
				&& !Modifier.isStatic(type.getModifiers());
			if (!isNonStaticMemberClass) {
				try {
					
					InitiatorCallback callback=new InitiatorCallback();
					InitiatorCommand command=new InitiatorCommand(callback, type);
					service.executeCommand(command);
					final ConstructorAccess access = callback.awaitResult();
					if(access==null && command.getException()!=null)
						throw command.getException();
					
					if(access!=null) {
						return new ObjectInstantiator() {
							@Override
							public Object newInstance () {
								try {
									return access.newInstance();
								} catch (Exception ex) {
									throw new KryoException("Error constructing instance of class: " + Util.className(type), ex);
								}
							}
						};
					}
				} catch (Exception ignored) {
				}
			}
		}

		// Reflection.
		try {
			Constructor ctor;
			try {
				ctor = type.getConstructor((Class[])null);
			} catch (Exception ex) {
				ctor = type.getDeclaredConstructor((Class[])null);
				ctor.setAccessible(true);
			}
			final Constructor constructor = ctor;
			return new ObjectInstantiator() {
				@Override
				public Object newInstance () {
					try {
						return constructor.newInstance();
					} catch (Exception ex) {
						throw new KryoException("Error constructing instance of class: " + Util.className(type), ex);
					}
				}
			};
		} catch (Exception ignored) {
		}

		if (fallbackStrategy == null) {
			if (type.isMemberClass() && !Modifier.isStatic(type.getModifiers()))
				throw new KryoException("Class cannot be created (non-static member class): " + Util.className(type));
			else {
				StringBuilder message = new StringBuilder("Class cannot be created (missing no-arg constructor): " + Util.className(type));
				if (type.getSimpleName().equals("")) {
					message
						.append(
							"\nNote: This is an anonymous class, which is not serializable by default in Kryo. Possible solutions:\n")
						.append("1. Remove uses of anonymous classes, including double brace initialization, from the containing\n")
						.append(
							"class. This is the safest solution, as anonymous classes don't have predictable names for serialization.\n")
						.append("2. Register a FieldSerializer for the containing class and call FieldSerializer\n")
						.append("setIgnoreSyntheticFields(false) on it. This is not safe but may be sufficient temporarily.");
				}
				throw new KryoException(message.toString());
			}
		}
		// InstantiatorStrategy.
		return fallbackStrategy.newInstantiatorOf(type);
	}
	
	public class InitiatorCallback {

        private Semaphore semaphore = new Semaphore(0);

        private ConstructorAccess<?> result;
        
        public void receiveResult(ConstructorAccess<?> result) {
        	this.result = result;
            semaphore.release();            
        }
        
        public ConstructorAccess<?> awaitResult() {
            try {
            	semaphore.acquire();            	
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
            
            return this.result;
        }
    }
	
	public class InitiatorCommand implements CacheDataExecutorService.CommonCommand {
        protected InitiatorCallback callback;
        private Class<?> type;
        private Boolean isCanceled = false;
        private RuntimeException exception;
        
        public InitiatorCommand(InitiatorCallback callback,Class<?> type) {
            this.callback = callback;
            this.type=type;
        }
        
        public void execute() {
        	if(!isCanceled) {
        		try {
        			callback.receiveResult(ConstructorAccess.get(type));           			
        		}
        		catch (RuntimeException e) {
					this.exception=e;
					callback.receiveResult(null);
				}
        		catch(Exception ex) {
        			logger.error(ex.getMessage(), ex);
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