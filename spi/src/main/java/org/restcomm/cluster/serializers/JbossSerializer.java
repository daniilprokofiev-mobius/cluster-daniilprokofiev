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

/**
 * @author yulian.oifa
 *
 */
public class JbossSerializer implements Serializer 
{
	private GenericJbossMarshaller globalMarshaller;
    
    public JbossSerializer(ClassLoader classLoader) {
    	this.globalMarshaller=new GenericJbossMarshaller(classLoader);
    	this.globalMarshaller.start();    	
    }
    
    public Object deserialize(byte[] data) {
    	return deserialize(data,0,data.length);
    }
    
    public void init() {
    	//should create the marshaller and unmarshaller
    	Boolean dummyData=new Boolean(true);
    	byte[] data=serialize(dummyData);
    	deserialize(data);
    }
    
	public Object deserialize(byte[] data,int offset,int length) {
		try {			
			return globalMarshaller.objectFromByteBuffer(data, offset, length);			
		}
		catch(IOException | ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public Object deserialize(InputStream is) {
		try {			
			return globalMarshaller.objectFromInputStream(is);
		}
		catch(IOException | ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		}
    }
	
	public byte[] serialize(Object value) {
		try {	    	
			return globalMarshaller.objectToByteBuffer(value);
		}
		catch(IOException | InterruptedException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public void serialize(Object value,OutputStream os) {
		try {	    	
			os.write(serialize(value));
		}
		catch(IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public void registerKnownClass(int id,Class<? extends Externalizable> knownClass) {
		globalMarshaller.registerKnownClass(id, knownClass);
	}
}