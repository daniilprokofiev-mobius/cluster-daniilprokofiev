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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.util.Util;
/**
 * 
 * ID based externalizer for Infinispan
 * 
 * @author yulian.oifa
 *
 */
public class ExternalizableExternalizer extends AbstractExternalizer<Externalizable> {
	private static final long serialVersionUID = 1L;
	
	private Class<? extends Externalizable> realClass;
	public Integer realID;
	
	public ExternalizableExternalizer(Class<? extends Externalizable> realClass,Integer realID) {
		this.realClass=realClass;
		this.realID=realID;
	}
	
	@Override
	public void writeObject(ObjectOutput output, Externalizable object) throws IOException {
		object.writeExternal(output);			
	}

	@Override
	public Externalizable readObject(ObjectInput input) throws IOException, ClassNotFoundException {
		Externalizable result;
		try {
			result = realClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new IOException(e);
		}
		
		result.readExternal(input);
		return result;		
	}

	@Override
	public Set<Class<? extends Externalizable>> getTypeClasses() {
		return Util.<Class<? extends Externalizable>>asSet(realClass);			
	}
	
	@Override
	public Integer getId() {
		return realID;
	}
}