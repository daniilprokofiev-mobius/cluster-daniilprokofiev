package org.restcomm.cluster.serializers;
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
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.HashMap;
/**
 * @author yulian.oifa
 *
 */
public class CustomObjectInputStream extends ObjectInputStream {
	private static final HashMap<String, Class<?>> primClasses = new HashMap<>(8, 1.0F);
	static {
		primClasses.put("boolean", boolean.class);
		primClasses.put("byte", byte.class);
		primClasses.put("char", char.class);
		primClasses.put("short", short.class);
		primClasses.put("int", int.class);
		primClasses.put("long", long.class);
		primClasses.put("float", float.class);
		primClasses.put("double", double.class);
		primClasses.put("void", void.class);
	}
	
	private ClassLoader classLoader;

	public CustomObjectInputStream(ClassLoader classLoader, InputStream is) throws IOException {
		super(is);
		this.classLoader = classLoader;
	}

	protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
		String name = desc.getName();
		try {
			return Class.forName(name, false, classLoader);
		} catch (ClassNotFoundException ex) {
			Class<?> cl = primClasses.get(name);
			if (cl != null) {
				return cl;
			} else {
				throw ex;
			}
		}
	}
}
