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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 
 * @author yulian.oifa
 * 
 */
public class UUIDGenerator implements IDGenerator<UUID> 
{
	private static byte[] DefaultNodeBytes=new byte[6];
    private static long gregorianCalendarStart=-12219292800000L;
    private Random random=new Random();    
    private AtomicLong currValue=new AtomicLong(0);
    
    public static transient ThreadLocal<byte[]> buffers = new ThreadLocal<byte[]>() {
        @Override protected byte[] initialValue() {
        	byte[] localValue=new byte[16];  
        	return localValue;
        }
    };
    
    public UUIDGenerator() {
    	InetAddress localAddress=null;
    	InetAddress publicAddress=null;
    	    	
    	Enumeration<NetworkInterface> enumNetworkInterfaces = null;
    	try {
    		enumNetworkInterfaces=NetworkInterface.getNetworkInterfaces();
    	}
    	catch(SocketException ex) {
    		
    	}
    	
    	if(enumNetworkInterfaces!=null) {
	        while (enumNetworkInterfaces.hasMoreElements()) {
	
	            NetworkInterface networkInterface = enumNetworkInterfaces.nextElement();
	            Enumeration<InetAddress> enumInetAddress = networkInterface.getInetAddresses();
	
	            while (enumInetAddress.hasMoreElements()) {
	                InetAddress inetAddress = enumInetAddress.nextElement();
	
	                if (!inetAddress.isAnyLocalAddress() && !inetAddress.isMulticastAddress()) {
	                    if(inetAddress.isLoopbackAddress())
	                    	localAddress=inetAddress;
	                    else
	                    	publicAddress=inetAddress;
	                }
	            }
	        }
    	}
        
        InetAddress realAddress=null;
        if(publicAddress!=null)
        	realAddress=publicAddress;
        else if(localAddress!=null)
        	realAddress=localAddress;
        else
        	realAddress=InetAddress.getLoopbackAddress();
                
        currValue.set(random.nextInt());
    	if(realAddress instanceof Inet4Address)
        {
        	byte[] realNodeBytes=new byte[6];
        	System.arraycopy(realAddress.getAddress(),0,realNodeBytes,0,4);
        	byte[] randomSeed=new byte[2];
        	random.nextBytes(randomSeed);
        	System.arraycopy(randomSeed, 0, realNodeBytes, 4, 2);
        	DefaultNodeBytes=realNodeBytes;
        }
        else
        {
        	byte[] realNodeBytes=new byte[6];
        	System.arraycopy(realAddress.getAddress(),0,realNodeBytes,0,6); 
        	DefaultNodeBytes=realNodeBytes;
        }            	
    }
    
    public UUIDGenerator(byte[] serverKey) throws Exception
    {
    	currValue.set(random.nextInt());
        
        if(serverKey.length==6)
        	DefaultNodeBytes=serverKey;
        else
        	throw new Exception("can not initiate node bytes");
    }
    
    public void DefaultClockSequence(byte[] data,int index)
    {
    	long value=currValue.incrementAndGet();    	
    	data[index]=(byte)((value>>8) & 0x00FF);
    	data[index+1]=(byte)(value & 0x00FF);    	
    }
    
    public static byte[] uuidToBytes(UUID value)
    {
    	byte[] data=UUIDGenerator.buffers.get();
    	ByteBuf bb = Unpooled.wrappedBuffer(data);
    	bb.resetWriterIndex();
    	bb.writeLong(value.getMostSignificantBits());
    	bb.writeLong(value.getLeastSignificantBits());
    	return data;
    }
	
    public static UUID bytesToUUID(ByteBuf value)
    {
    	return new UUID(value.readLong(),value.readLong());    	
    }
	
    public static long getTimestamp(UUID uuid)
    {
    	return uuid.timestamp()/10000L + gregorianCalendarStart;
    }  
    
    public static short getClockSequence(UUID uuid)
    {
    	byte[] data=uuidToBytes(uuid);
    	short result=getClockSequence(data);
    	return result;
    }
    
    public static short getClockSequence(byte[] uuidBytes)
    {
    	return (short)(((uuidBytes[8] & 0x7F)<<8) | (uuidBytes[9]&0x0FF));
    }
    
	public byte[] GenerateTimeBasedGuidBytes(long time)
    {
    	long ticks = (time - gregorianCalendarStart)*10000L;
        
        byte[] guid = UUIDGenerator.buffers.get();
        ByteBuf buffer=Unpooled.wrappedBuffer(guid);
        buffer.resetWriterIndex();
        buffer.writeLong(ticks);
        
        byte tempBuffer;
        for(int i=0;i<4;i++)
        {
        	tempBuffer=guid[i];
        	guid[i]=guid[7-i];
        	guid[7-i]=tempBuffer;
        }
        
        // copy node
        System.arraycopy(DefaultNodeBytes, 0, guid, 10, 6);
        
        // copy clock sequence
        DefaultClockSequence(guid,8);
        
        guid[8] &= (byte)0x3f;
        guid[8] |= (byte)0x80;

        // set the version
        guid[7] &= (byte)0x0f;
        guid[7] |= (byte)((byte)0x01 << 4);
        
        //now lets reverse data
        tempBuffer=guid[0];
        guid[0]=guid[3];
        guid[3]=tempBuffer;
        
        tempBuffer=guid[1];
        guid[1]=guid[2];
        guid[2]=tempBuffer;
        
        tempBuffer=guid[4];
        guid[4]=guid[5];
        guid[5]=tempBuffer;
        
        tempBuffer=guid[6];
        guid[6]=guid[7];
        guid[7]=tempBuffer;
        
        return guid;
    }
	
	@Override
	public ClusteredID<UUID> generateID() 
	{
		byte[] data=GenerateTimeBasedGuidBytes(System.currentTimeMillis());
		ClusteredUUID result = new ClusteredUUID(bytesToUUID(Unpooled.wrappedBuffer(data)));
		return result;
	}

	@Override
	public ClusteredID<UUID> fromString(String value) 
	{		
		return new ClusteredUUID(UUID.fromString(value));
	}

	@Override
	public ClusteredID<UUID> fromBytes(byte[] data) 
	{
		return new ClusteredUUID(bytesToUUID(Unpooled.wrappedBuffer(data)));
	}
}