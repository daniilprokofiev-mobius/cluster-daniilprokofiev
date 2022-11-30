package org.restcomm.cluster.serializers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.cluster.CacheDataExecutorService;
import org.restcomm.cluster.CacheExecutorConfiguration;
import org.restcomm.cluster.ClusteredID;
import org.restcomm.cluster.UUIDGenerator;

public class SerializationTest {
	
	private static final Logger logger = LogManager.getLogger(SerializationTest.class);

	static JavaSerializer javaSerializer=new JavaSerializer(Thread.currentThread().getContextClassLoader());
	static UUIDGenerator generator;
	static KryoSerializer kryoSerializer;
	static JbossSerializer jbossSerializer=new JbossSerializer(Thread.currentThread().getContextClassLoader());
	
	@BeforeClass
	public static void setUp() throws Exception {
		Configurator.initialize(new DefaultConfiguration());
	    Configurator.setRootLevel(Level.INFO);
	    generator=new UUIDGenerator(new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06} );
	    kryoSerializer=new KryoSerializer(Thread.currentThread().getContextClassLoader(), new CacheDataExecutorService(new CacheExecutorConfiguration(1, 1000L, 1000L),generator, Thread.currentThread().getContextClassLoader()),true);
	}
	
	@Test
	public void testSimpleClasses() throws Exception {
		Boolean bValue=new Boolean(true);
		byte[] data1=javaSerializer.serialize(bValue);
		byte[] data2=kryoSerializer.serialize(bValue);
		byte[] data3=jbossSerializer.serialize(bValue);
		
		assertEquals(data1.length, 47);
		assertEquals(data2.length, 2);
		assertEquals(data3.length, 2);
		
		assertEquals(bValue,javaSerializer.deserialize(data1));
		assertEquals(bValue,kryoSerializer.deserialize(data2));
		assertEquals(bValue,jbossSerializer.deserialize(data3));
		
		Integer iValue=new Integer(18);
		data1=javaSerializer.serialize(iValue);
		data2=kryoSerializer.serialize(iValue);
		data3=jbossSerializer.serialize(iValue);
		
		assertEquals(data1.length, 81);
		assertEquals(data2.length, 2);
		assertEquals(data3.length, 6);
		
		assertEquals(iValue,javaSerializer.deserialize(data1));
		assertEquals(iValue,kryoSerializer.deserialize(data2));
		assertEquals(iValue,jbossSerializer.deserialize(data3));
		
		Long lValue=new Long(32L);
		data1=javaSerializer.serialize(lValue);
		data2=kryoSerializer.serialize(lValue);
		data3=jbossSerializer.serialize(lValue);
		
		assertEquals(data1.length, 82);
		assertEquals(data2.length, 2);
		assertEquals(data3.length, 10);
		
		assertEquals(lValue,javaSerializer.deserialize(data1));
		assertEquals(lValue,kryoSerializer.deserialize(data2));
		assertEquals(lValue,jbossSerializer.deserialize(data3));
		
		String sValue=new String("Hello World");
		data1=javaSerializer.serialize(sValue);
		data2=kryoSerializer.serialize(sValue);
		data3=jbossSerializer.serialize(sValue);
		
		assertEquals(data1.length, 18);
		assertEquals(data2.length, 13);
		assertEquals(data3.length, 14);
		
		assertEquals(sValue,javaSerializer.deserialize(data1));
		assertEquals(sValue,kryoSerializer.deserialize(data2));
		assertEquals(sValue,jbossSerializer.deserialize(data3));				
	}
	
	@Test
	public void testWithIDRegistration() throws Exception {
		ClusteredID<?> id=generator.generateID();
		byte[] data1=javaSerializer.serialize(id);
		byte[] data2=kryoSerializer.serialize(id);
		byte[] data3=jbossSerializer.serialize(id);
		
		byte[] realBytes=id.getBytes();
		assertEquals(data1.length, 74);
		assertEquals(data2.length, 18);
		assertEquals(data3.length, 22);
		
		assertEquals(id,javaSerializer.deserialize(data1));
		assertEquals(id,kryoSerializer.deserialize(data2));		
		assertEquals(id,jbossSerializer.deserialize(data3));		
		
		assertTrue(isSubset(data1, realBytes));
		assertTrue(isSubset(data2, realBytes));
		assertTrue(isSubset(data3, realBytes));
	}
	
	@Test
	public void testObjects() throws Exception {
		ClusteredID<?> id=generator.generateID();
		
		ExternalizableSample sample1=new ExternalizableSample(true, 18, 32L, "Hello wolrd", id);
		SerializableSample sample2=new SerializableSample(true, 18, 32L, "Hello wolrd", id);
		
		byte[] data1=javaSerializer.serialize(sample1);
		byte[] data2=kryoSerializer.serialize(sample1);
		byte[] data3=jbossSerializer.serialize(sample1);
		
		assertEquals(data1.length,173);
		assertEquals(data2.length,98);
		assertEquals(data3.length,120);
		assertEquals(sample1,javaSerializer.deserialize(data1));
		assertEquals(sample1,kryoSerializer.deserialize(data2));	
		assertEquals(sample1,jbossSerializer.deserialize(data3));	
		
		data1=javaSerializer.serialize(sample2);
		data2=kryoSerializer.serialize(sample2);
		data3=jbossSerializer.serialize(sample2);
		
		assertEquals(data1.length,439);
		assertEquals(data2.length,89);
		assertEquals(data3.length,168);
		
		assertEquals(sample2,javaSerializer.deserialize(data1));
		assertEquals(sample2,kryoSerializer.deserialize(data2));
		assertEquals(sample2,jbossSerializer.deserialize(data3));			
	}
	
	@Test
	public void testSimplePerfomance() throws Exception {
		Boolean bValue=new Boolean(true);
		Integer iValue=new Integer(18);
		Long lValue=new Long(32L);
		String sValue=new String("Hello World");
		
		Long startTime=System.currentTimeMillis();
		for(int i=0;i<100000;i++) {
			byte[] data=javaSerializer.serialize(bValue);
			assertEquals(bValue,javaSerializer.deserialize(data));
			data=javaSerializer.serialize(iValue);
			assertEquals(iValue,javaSerializer.deserialize(data));
			data=javaSerializer.serialize(lValue);
			assertEquals(lValue,javaSerializer.deserialize(data));
			data=javaSerializer.serialize(sValue);
			assertEquals(sValue,javaSerializer.deserialize(data));
		}
		
		Long endTime1=System.currentTimeMillis();
		
		for(int i=0;i<100000;i++) {
			byte[] data=kryoSerializer.serialize(bValue);
			assertEquals(bValue,kryoSerializer.deserialize(data));
			data=kryoSerializer.serialize(iValue);
			assertEquals(iValue,kryoSerializer.deserialize(data));
			data=kryoSerializer.serialize(lValue);
			assertEquals(lValue,kryoSerializer.deserialize(data));
			data=kryoSerializer.serialize(sValue);
			assertEquals(sValue,kryoSerializer.deserialize(data));
		}
		
		Long endTime2=System.currentTimeMillis();
		

		for(int i=0;i<100000;i++) {
			byte[] data=jbossSerializer.serialize(bValue);
			assertEquals(bValue,jbossSerializer.deserialize(data));
			data=jbossSerializer.serialize(iValue);
			assertEquals(iValue,jbossSerializer.deserialize(data));
			data=jbossSerializer.serialize(lValue);
			assertEquals(lValue,jbossSerializer.deserialize(data));
			data=jbossSerializer.serialize(sValue);
			assertEquals(sValue,jbossSerializer.deserialize(data));
		}
		
		Long endTime3=System.currentTimeMillis();
		
		logger.info("Simple test JAVA TIME:" + (endTime1-startTime) + ",KRYO TIME:" + (endTime2-endTime1) + ",JBOSS TIME:" + (endTime3-endTime2));
		assertTrue((endTime2-endTime1)<(endTime1-startTime));
		assertTrue((endTime3-endTime2)<(endTime1-startTime));
		assertTrue((endTime3-endTime2)<(endTime2-endTime1));
	}
		
	@Test
	public void testIDPerfomance() throws Exception {
		ClusteredID<?> id=generator.generateID();
		
		Long startTime=System.currentTimeMillis();
		for(int i=0;i<100000;i++) {
			byte[] data=javaSerializer.serialize(id);
			assertEquals(id,javaSerializer.deserialize(data));			
		}
		
		Long endTime1=System.currentTimeMillis();
		
		for(int i=0;i<100000;i++) {
			byte[] data=kryoSerializer.serialize(id);
			assertEquals(id,kryoSerializer.deserialize(data));			
		}
		
		Long endTime2=System.currentTimeMillis();
		
		for(int i=0;i<100000;i++) {
			byte[] data=jbossSerializer.serialize(id);
			assertEquals(id,jbossSerializer.deserialize(data));			
		}
		
		Long endTime3=System.currentTimeMillis();
		
		logger.info("id test JAVA TIME:" + (endTime1-startTime) + ",KRYO TIME:" + (endTime2-endTime1) + ",JBOSS TIME:" + (endTime3-endTime2));
		assertTrue((endTime2-endTime1)<(endTime1-startTime));
		assertTrue((endTime3-endTime2)<(endTime1-startTime));
		//no winner here
		//assertTrue((endTime3-endTime2)<(endTime2-endTime1));
	}
		
	@Test
	public void testSerializedPerfomance() throws Exception {
		ClusteredID<?> id=generator.generateID();
		
		SerializableSample sample2=new SerializableSample(true, 18, 32L, "Hello world", id);
		
		Long startTime=System.currentTimeMillis();
		for(int i=0;i<100000;i++) {
			byte[] data=javaSerializer.serialize(sample2);
			assertEquals(sample2,javaSerializer.deserialize(data));
		}
		
		Long endTime1=System.currentTimeMillis();
		
		for(int i=0;i<100000;i++) {
			byte[] data=kryoSerializer.serialize(sample2);
			assertEquals(sample2,kryoSerializer.deserialize(data));
		}
		
		Long endTime2=System.currentTimeMillis();
		
		for(int i=0;i<100000;i++) {
			byte[] data=jbossSerializer.serialize(sample2);
			assertEquals(sample2,jbossSerializer.deserialize(data));
		}
		
		Long endTime3=System.currentTimeMillis();
		
		logger.info("Serializable test JAVA TIME:" + (endTime1-startTime) + ",KRYO TIME:" + (endTime2-endTime1) + ",JBOSS TIME:" + (endTime3-endTime2));
		assertTrue((endTime2-endTime1)<(endTime1-startTime));
		assertTrue((endTime3-endTime2)<(endTime1-startTime));
		//no winner here
		//assertTrue((endTime3-endTime2)<(endTime2-endTime1));
	}
	
	@Test
	public void testExternalizablePerfomance() throws Exception {
		ClusteredID<?> id=generator.generateID();
		
		ExternalizableSample sample1=new ExternalizableSample(true, 18, 32L, "Hello world", id);
		
		Long startTime=System.currentTimeMillis();
		for(int i=0;i<100000;i++) {
			byte[] data=javaSerializer.serialize(sample1);
			assertEquals(sample1,javaSerializer.deserialize(data));			
		}
		
		Long endTime1=System.currentTimeMillis();
		
		for(int i=0;i<100000;i++) {
			byte[] data=kryoSerializer.serialize(sample1);
			assertEquals(sample1,kryoSerializer.deserialize(data));			
		}
		
		Long endTime2=System.currentTimeMillis();
		
		for(int i=0;i<100000;i++) {
			byte[] data=jbossSerializer.serialize(sample1);
			assertEquals(sample1,jbossSerializer.deserialize(data));			
		}
		
		Long endTime3=System.currentTimeMillis();
		
		logger.info("Externalizable test JAVA TIME:" + (endTime1-startTime) + ",KRYO TIME:" + (endTime2-endTime1) + ",JBOSS TIME:" + (endTime3-endTime2));
		assertTrue((endTime2-endTime1)<(endTime1-startTime));
		assertTrue((endTime3-endTime2)<(endTime1-startTime));
		//no winner here
		//assertTrue((endTime3-endTime2)<(endTime2-endTime1));
	}
	
	static final String HEXES = "0123456789ABCDEF";
	public static String getHex( byte [] raw ) {
	    if ( raw == null ) {
	        return null;
	    }
	    final StringBuilder hex = new StringBuilder( 2 * raw.length );
	    for ( final byte b : raw ) {
	        hex.append(HEXES.charAt((b & 0xF0) >> 4))
	            .append(HEXES.charAt((b & 0x0F)));
	    }
	    return hex.toString();
	}
	
	boolean isSubset(byte arr1[], byte arr2[])
	{
		int i = 0;
		int j = 0;
		for (i = 0; i < arr2.length; i++) {
			for (j = 0; j < arr1.length; j++) {
				if (arr2[i] == arr1[j])
					break;
			}
		
			if (j == arr1.length)
				return false;
		}
		
		return true;
	}
}