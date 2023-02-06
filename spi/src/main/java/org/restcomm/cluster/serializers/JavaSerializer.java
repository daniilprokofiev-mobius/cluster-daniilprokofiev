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
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
/**
 * @author yulian.oifa
 *
 */
public class JavaSerializer implements Serializer 
{
	private transient ThreadLocal<ByteBuf> buffers = new ThreadLocal<ByteBuf>() {
        @Override protected ByteBuf initialValue() {
        	ByteBuf localValue=Unpooled.buffer();  
        	return localValue;
        }
    };
    
    private ClassLoader classLoader;
	
	public JavaSerializer(ClassLoader classLoader) {
		this.classLoader=classLoader;
    }
    
	public void init() {
		
	}
	
    public Object deserialize(byte[] data) {
    	return deserialize(data,0,data.length);
    }
    
	public Object deserialize(byte[] data,int offset,int length) {
		ByteBufInputStream bis=new ByteBufInputStream(Unpooled.wrappedBuffer(data,offset,length));
		return deserialize(bis);		
	}
	
	public Object deserialize(InputStream is) {
		try {
			CustomObjectInputStream ins=new CustomObjectInputStream(classLoader,is);		
			Object value = ins.readObject();
			ins.close();
			return value;
		}
		catch(IOException | ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public void serialize(Object value,OutputStream os) {
		ObjectOutputStream out = null;
		
		try {	    	
			out = new ObjectOutputStream(os);
	        out.writeObject(value);
	    }
		catch(IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public byte[] serialize(Object value) {
		ByteBuf buffer=buffers.get();
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

	@Override
	public void registerKnownClass(int id, Class<? extends Externalizable> knownClass) {	
		//not used here
	}
}