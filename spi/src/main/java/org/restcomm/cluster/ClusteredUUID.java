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

package org.restcomm.cluster;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * @author yulian.oifa
 */
public class ClusteredUUID implements ClusteredID<UUID> 
{
	private UUID uuid;
	private int hashCode;
	
	public ClusteredUUID() {
	}
	
	public ClusteredUUID(UUID uuid) {
		this.uuid=uuid;
		calculateHash();
	}
	
	@Override
	public IDType getType() {
		return IDType.uuid;
	}

	@Override
	public UUID getValue() {
		return this.uuid;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.write(UUIDGenerator.uuidToBytes(uuid));
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		byte[] data=UUIDGenerator.buffers.get();
		in.read(data);
		uuid=UUIDGenerator.bytesToUUID(Unpooled.wrappedBuffer(data));
		calculateHash();	
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException
	{
		out.write(UUIDGenerator.uuidToBytes(uuid));
	}
		     
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		byte[] data=UUIDGenerator.buffers.get();
		in.read(data);
		uuid=UUIDGenerator.bytesToUUID(Unpooled.wrappedBuffer(data));	
		calculateHash();
	}
	
	private void calculateHash() {
		//2 LSB bytes of timestamp and 2 of clock sequence
		long timestamp=UUIDGenerator.getTimestamp(uuid);
		short seqBytes=UUIDGenerator.getClockSequence(uuid);
		ByteBuf buffer=Unpooled.buffer(4);
		buffer.writeShort(seqBytes);
		buffer.writeShort((int)(timestamp & 0x0FFFF));
		hashCode=buffer.readInt();
	}
	
	@Override
	public int hashCode() {
		if(uuid==null)
			return 0;
				
		return hashCode;    
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClusteredUUID other = (ClusteredUUID) obj;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		
		return true;
	}
	
	@Override
	public String toString() {
		return uuid.toString();
	}

	@Override
	public byte[] getBytes() 
	{
		return UUIDGenerator.uuidToBytes(uuid);
	}
}