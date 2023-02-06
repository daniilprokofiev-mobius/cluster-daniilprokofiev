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

import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.SerializedLambda;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.restcomm.cluster.CacheDataExecutorService;
import org.restcomm.cluster.ClusteredUUID;
import org.restcomm.cluster.data.ClusteredIDAndStringKey;
import org.restcomm.cluster.data.ClusteredIDAndStringTreeSegment;
import org.restcomm.cluster.data.ClusteredIDTreeSegment;
import org.restcomm.cluster.data.MultiStringKey;
import org.restcomm.cluster.data.MultiStringTreeSegment;
import org.restcomm.cluster.data.StringAndClusteredIDKey;
import org.restcomm.cluster.data.StringAndClusteredIDTreeSegment;
import org.restcomm.cluster.data.StringTreeSegment;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.esotericsoftware.kryo.kryo5.serializers.ClosureSerializer;
import com.esotericsoftware.kryo.kryo5.serializers.ClosureSerializer.Closure;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
/**
 * @author yulian.oifa
 *
 */
public class KryoSerializer implements Serializer {
	private static Integer FIRST_ID=120;
	
	private ClassLoader classLoader;
    private Boolean registerClusteredID;
    private CacheDataExecutorService cacheDataExecutorService;
    
    private static final int CLUSTERED_UUID_ID=101;
    private static final int CLUSTERED_ID_SEGMENT_ID=102;
    private static final int STRING_SEGMENT_ID=103;
    private static final int MULTISTRING_ID=104;
    private static final int CLUSTERED_ID_AND_STRING_ID=105;
    private static final int STRING_AND_CLUSTERED_ID_ID=106;
    private static final int MULTISTRING_SEGMENT_ID=107;
    private static final int CLUSTERED_ID_AND_STRING_SEGMENT_ID=108;
    private static final int STRING_AND_CLUSTERED_ID_SEGMENT_ID=109;
    
    private static ConcurrentHashMap<Integer, Class<?>> registeredClasses=new ConcurrentHashMap<Integer, Class<?>>();
    private static ConcurrentLinkedQueue<Kryo> allInstances=new ConcurrentLinkedQueue<Kryo>();
    
    static {
    	registeredClasses.put(CLUSTERED_UUID_ID, ClusteredUUID.class);
    	registeredClasses.put(MULTISTRING_ID, MultiStringKey.class);
    	registeredClasses.put(CLUSTERED_ID_AND_STRING_ID, ClusteredIDAndStringKey.class);
    	registeredClasses.put(STRING_AND_CLUSTERED_ID_ID, StringAndClusteredIDKey.class);
    	registeredClasses.put(CLUSTERED_ID_SEGMENT_ID, ClusteredIDTreeSegment.class);
    	registeredClasses.put(STRING_SEGMENT_ID, StringTreeSegment.class);
    	registeredClasses.put(MULTISTRING_SEGMENT_ID, MultiStringTreeSegment.class);
    	registeredClasses.put(CLUSTERED_ID_AND_STRING_SEGMENT_ID, ClusteredIDAndStringTreeSegment.class);
    	registeredClasses.put(STRING_AND_CLUSTERED_ID_SEGMENT_ID, StringAndClusteredIDTreeSegment.class);
    }
    
	private transient ThreadLocal<Kryo> kryos = new ThreadLocal<Kryo>() {
        @Override protected Kryo initialValue() {
        	Kryo localValue=new Kryo();  
        	localValue.setReferences(true);
        	localValue.setDefaultSerializer(new KryoSerializerFactory(cacheDataExecutorService));
        	localValue.setInstantiatorStrategy(new KryoDefaultInitiatorStrategy(cacheDataExecutorService, new KryoDefaultSerialingInitiatorStrategy(cacheDataExecutorService)));
        	localValue.addDefaultSerializer(Externalizable.class, KryoExternalizableSerializer.class);
        	localValue.setClassLoader(classLoader);
        	localValue.register(SerializedLambda.class);
        	localValue.register(Closure.class, new ClosureSerializer()); 
        	if(registerClusteredID) {
        		Iterator<Entry<Integer, Class<?>>> iterator=registeredClasses.entrySet().iterator();
        		while(iterator.hasNext()) {
        			Entry<Integer, Class<?>> currEntry=iterator.next();
        			localValue.register(currEntry.getValue(),currEntry.getKey());
        		}
        	}
        	
        	localValue.setRegistrationRequired(false);
        	allInstances.add(localValue);
        	return localValue;
        }
    };
    
    private transient ThreadLocal<ByteBuf> buffers = new ThreadLocal<ByteBuf>() {
        @Override protected ByteBuf initialValue() {
        	ByteBuf localValue=Unpooled.buffer();  
        	return localValue;
        }
    };
    
    public KryoSerializer(ClassLoader classLoader,CacheDataExecutorService cacheDataExecutorService,Boolean registerClusteredID) {
    	this.classLoader=classLoader;
    	this.registerClusteredID=registerClusteredID;
    	this.cacheDataExecutorService=cacheDataExecutorService;
    }
    
    public void init() {
    	kryos.get();
    }
    
    public Object deserialize(byte[] data) {
    	return deserialize(data,0,data.length);
    }
    
    public Object deserialize(byte[] data,int offset,int length) {
    	ByteBufInputStream stream = null;
    	stream = new ByteBufInputStream(Unpooled.wrappedBuffer(data,offset,length));
    	
    	try {
    		return deserialize(stream);
    	}
    	finally {
	    	try {
				stream.close();
			}
			catch(IOException ex) {
				
			}
    	} 	
    }
    
    public void registerKnownClass(int id,Class<? extends Externalizable> knownClass) {
    	registeredClasses.put(FIRST_ID + id, knownClass);
    	Iterator<Kryo> iterator=allInstances.iterator();
    	while(iterator.hasNext())
    		iterator.next().register(knownClass, FIRST_ID + id);
    }
    
	public Object deserialize(InputStream is) {
		Input in = new Input(is);
		return kryos.get().readClassAndObject(in);
	}
	
	public byte[] serialize(Object value) {
		ByteBuf buffer = buffers.get();
		buffer.resetReaderIndex();
		buffer.resetWriterIndex();
		ByteBufOutputStream bos = new ByteBufOutputStream(buffer);
		serialize(value,bos);
		byte[] data=new byte[buffer.readableBytes()];
        buffer.readBytes(data);
		try {
			bos.close();
		}
		catch(IOException ex) {
			
		}
		
        return data;
	}
	
	public void serialize(Object value,OutputStream os) {
		Output out = new Output(os);
		kryos.get().writeClassAndObject(out, value);
        out.flush(); 
	}
}