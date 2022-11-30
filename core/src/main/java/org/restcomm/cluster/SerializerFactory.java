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
package org.restcomm.cluster;
/**
 * 
 * @author yulian.oifa
 * 
 */

import org.restcomm.cluster.serializers.JavaSerializer;
import org.restcomm.cluster.serializers.JbossSerializer;
import org.restcomm.cluster.serializers.KryoSerializer;
import org.restcomm.cluster.serializers.Serializer;

public class SerializerFactory {
	public static Serializer createSerializer(SerializationType serializationType,ClassLoader classLoader,CacheDataExecutorService service) {
		switch (serializationType) {
			case JAVA:
				return new JavaSerializer(classLoader);				
			case KRYO:
				return new KryoSerializer(classLoader, service, true);
			case JBOSS:
				return new JbossSerializer(classLoader);
			case NATIVE:
			default:
				return null;
		}
	}
}