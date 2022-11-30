package org.restcomm.cluster.serializers;
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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectOutput;
import java.util.concurrent.ConcurrentHashMap;

import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.marshall.AdvancedExternalizer;

import org.infinispan.commons.marshall.jboss.AbstractJBossMarshaller;
import org.infinispan.commons.marshall.jboss.DefaultContextClassResolver;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.ObjectTable.Writer;
import org.jboss.marshalling.Unmarshaller;
import org.restcomm.cluster.ClusteredUUID;
import org.restcomm.cluster.data.AbstractTreeSegment;
import org.restcomm.cluster.data.ClusteredIDAndStringKey;
import org.restcomm.cluster.data.ClusteredIDAndStringTreeSegment;
import org.restcomm.cluster.data.ClusteredIDTreeSegment;
import org.restcomm.cluster.data.ClusteredUUIDExternalizer;
import org.restcomm.cluster.data.ExternalizableExternalizer;
import org.restcomm.cluster.data.MultiStringKey;
import org.restcomm.cluster.data.MultiStringTreeSegment;
import org.restcomm.cluster.data.StringAndClusteredIDKey;
import org.restcomm.cluster.data.StringAndClusteredIDTreeSegment;
import org.restcomm.cluster.data.StringTreeSegment;

/**
 * @author yulian.oifa
 *
 */
public class GenericJbossMarshaller extends AbstractJBossMarshaller {
	public static Integer FIRST_ID=2010;
	
	private ConcurrentHashMap<String,KnownObjectsWriter<?>> writers=new ConcurrentHashMap<String,KnownObjectsWriter<?>>();
	private ConcurrentHashMap<Integer,AbstractExternalizer<?>> externalizers=new ConcurrentHashMap<Integer,AbstractExternalizer<?>>();
	
	public GenericJbossMarshaller(ClassLoader classLoader) {
		super();
		
		AbstractTreeSegment.Externalizer treeExternalizer = new AbstractTreeSegment.Externalizer();
		externalizers.put(AbstractTreeSegment.Externalizer.EXTERNALIZER_ID, treeExternalizer);
		ClusteredUUIDExternalizer idExternalizer = new ClusteredUUIDExternalizer();
		externalizers.put(ClusteredUUIDExternalizer.EXTERNALIZER_ID, idExternalizer);
		ExternalizableExternalizer stringAndClusteredIDExternalizer = new ExternalizableExternalizer(StringAndClusteredIDKey.class,StringAndClusteredIDKey.EXTERNALIZER_ID);
		externalizers.put(StringAndClusteredIDKey.EXTERNALIZER_ID, stringAndClusteredIDExternalizer);
		ExternalizableExternalizer clusteredIDAndStringExternalizer = new ExternalizableExternalizer(ClusteredIDAndStringKey.class,ClusteredIDAndStringKey.EXTERNALIZER_ID);
		externalizers.put(ClusteredIDAndStringKey.EXTERNALIZER_ID, clusteredIDAndStringExternalizer);
		ExternalizableExternalizer multiStringExternalizer = new ExternalizableExternalizer(MultiStringKey.class,MultiStringKey.EXTERNALIZER_ID);
		externalizers.put(MultiStringKey.EXTERNALIZER_ID, multiStringExternalizer);
		
		KnownObjectsWriter<?> writer = new KnownObjectsWriter<AbstractTreeSegment<?>>(treeExternalizer);
		writers.put(AbstractTreeSegment.class.getCanonicalName(), writer);
		writers.put(ClusteredIDTreeSegment.class.getCanonicalName(), writer);
		writers.put(StringTreeSegment.class.getCanonicalName(), writer);
		writers.put(StringAndClusteredIDTreeSegment.class.getCanonicalName(), writer);
		writers.put(ClusteredIDAndStringTreeSegment.class.getCanonicalName(), writer);
		writers.put(MultiStringTreeSegment.class.getCanonicalName(), writer);
		
		writer = new KnownObjectsWriter<ClusteredUUID>(idExternalizer);
		writers.put(ClusteredUUID.class.getCanonicalName(),writer);
		
		writer = new KnownObjectsWriter<Externalizable>(stringAndClusteredIDExternalizer);
		writers.put(StringAndClusteredIDKey.class.getCanonicalName(),writer);
		
		writer = new KnownObjectsWriter<Externalizable>(clusteredIDAndStringExternalizer);
		writers.put(ClusteredIDAndStringKey.class.getCanonicalName(),writer);
		
		writer = new KnownObjectsWriter<Externalizable>(multiStringExternalizer);
		writers.put(MultiStringKey.class.getCanonicalName(),writer);
		
		baseCfg.setClassResolver(new DefaultContextClassResolver(classLoader != null ? classLoader : this.getClass().getClassLoader()));
		baseCfg.setObjectTable(new ObjectTable() {
			@Override
			public Writer getObjectWriter(Object object) throws IOException {
				return writers.get(object.getClass().getCanonicalName());				
			}

			@Override
			public Object readObject(Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
				@SuppressWarnings("rawtypes")
				AdvancedExternalizer ext = null;

				ext=externalizers.get(unmarshaller.readInt());
				if (ext == null)
					return null;

				return ext.readObject(unmarshaller);
			}
		});
	}

	public void registerKnownClass(int id,Class<? extends Externalizable> knownClass) {
		ExternalizableExternalizer externalizer=new ExternalizableExternalizer(knownClass,FIRST_ID + id);
		externalizers.put(FIRST_ID + id, externalizer);		
		writers.put(knownClass.getCanonicalName(),new KnownObjectsWriter<Externalizable>(externalizer));		
	}
	
	private class KnownObjectsWriter<T> implements Writer {
		private AdvancedExternalizer<T> externalizer;

		public KnownObjectsWriter(AdvancedExternalizer<T> externalizer) {
			this.externalizer = externalizer;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void writeObject(Marshaller marshaller, Object object) throws IOException {
			marshaller.writeInt(externalizer.getId());
			externalizer.writeObject((ObjectOutput) marshaller, (T) object);
		}
	}
}