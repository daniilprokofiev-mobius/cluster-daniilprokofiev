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
package org.restcomm.cache.infinispan;

import java.io.IOException;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.marshall.BufferSizePredictor;
import org.infinispan.commons.marshall.Marshaller;
import org.restcomm.cluster.serializers.Serializer;
/**
 * @author yulian.oifa
 */
public class InfinispanMarshaller implements Marshaller {
	private Serializer serializer;
	
	public InfinispanMarshaller(Serializer serializer) {		
		this.serializer=serializer;
	}
	
	@Override
	public byte[] objectToByteBuffer(Object obj, int estimatedSize) throws IOException, InterruptedException {
		return serializer.serialize(obj);
	}

	@Override
	public byte[] objectToByteBuffer(Object obj) throws IOException, InterruptedException {
		return serializer.serialize(obj);
	}

	@Override
	public Object objectFromByteBuffer(byte[] buf) throws IOException, ClassNotFoundException {
		return serializer.deserialize(buf);
	}

	@Override
	public Object objectFromByteBuffer(byte[] buf, int offset, int length) throws IOException, ClassNotFoundException {
		return serializer.deserialize(buf,offset,length);
	}

	@Override
	public ByteBuffer objectToBuffer(Object o) throws IOException, InterruptedException {
		byte[] data=objectToByteBuffer(o);
		return new ByteBufferImpl(data);
	}

	@Override
	public boolean isMarshallable(Object o) throws Exception {
		return true;
	}

	@Override
	public BufferSizePredictor getBufferSizePredictor(Object o) {
		return null;
	}

	@Override
	public MediaType mediaType() {
		return MediaType.APPLICATION_JBOSS_MARSHALLING;
	}
}