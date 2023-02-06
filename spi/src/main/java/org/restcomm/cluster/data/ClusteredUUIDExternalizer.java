package org.restcomm.cluster.data;
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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;
import java.util.UUID;

import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.util.Util;
import org.restcomm.cluster.ClusteredUUID;
import org.restcomm.cluster.UUIDGenerator;

import io.netty.buffer.Unpooled;
/**
 * 
 * Clustered UUID externalizer for Infinispan
 * 
 * @author yulian.oifa
 *
 */
public class ClusteredUUIDExternalizer extends AbstractExternalizer<ClusteredUUID> {
	private static final long serialVersionUID = 1L;

	public static final Integer EXTERNALIZER_ID=2001;
	
	@Override
	public void writeObject(ObjectOutput output, ClusteredUUID id) throws IOException {
		output.write(id.getBytes());
	}

	@Override
	public ClusteredUUID readObject(ObjectInput input) throws IOException, ClassNotFoundException {
		byte[] data=UUIDGenerator.buffers.get();
		input.read(data);
		UUID uuid=UUIDGenerator.bytesToUUID(Unpooled.wrappedBuffer(data));
		return new ClusteredUUID(uuid);		
	}

	@Override
	public Set<Class<? extends ClusteredUUID>> getTypeClasses() {
		return Util.<Class<? extends ClusteredUUID>>asSet(ClusteredUUID.class);			
	}
	
	@Override
	public Integer getId() {
		return EXTERNALIZER_ID;
	}
}