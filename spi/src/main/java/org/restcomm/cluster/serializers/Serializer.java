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
package org.restcomm.cluster.serializers;

import java.io.Externalizable;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author yulian.oifa
 *
 */
public interface Serializer {
	public Object deserialize(InputStream is);
	
	public Object deserialize(byte[] data);
	
	public Object deserialize(byte[] data,int offset,int length);
	
	public byte[] serialize(Object value);		
	
	public void serialize(Object value,OutputStream os);
	
	public void init();
	
	public void registerKnownClass(int id,Class<? extends Externalizable> knownClass);		
}