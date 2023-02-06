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
package org.restcomm.cluster.serializers;

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.KryoException;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.KryoObjectInput;
import com.esotericsoftware.kryo.kryo5.io.KryoObjectOutput;
import com.esotericsoftware.kryo.kryo5.io.Output;
/**
 * @author yulian.oifa
 *
 */
public class KryoExternalizableSerializer extends com.esotericsoftware.kryo.kryo5.Serializer<Externalizable> {
	private KryoObjectInput objectInput = null;
	private KryoObjectOutput objectOutput = null;

	public void write (Kryo kryo, Output output, Externalizable object) {
		writeExternal(kryo, output, object);		
	}

	@SuppressWarnings("rawtypes")
	public Externalizable read (Kryo kryo, Input input, Class type) {		
		return readExternal(kryo, input, type);		
	}

	private void writeExternal (Kryo kryo, Output output, Object object) {
		try {
			((Externalizable)object).writeExternal(getObjectOutput(kryo, output));
		} catch (Exception ex) {
			throw new KryoException(ex);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Externalizable readExternal (Kryo kryo, Input input, Class type) {
		try {
			Externalizable object = (Externalizable)kryo.newInstance(type);
			object.readExternal(getObjectInput(kryo, input));
			return object;
		} catch (Exception ex) {
			throw new KryoException(ex);
		}
	}

	private ObjectOutput getObjectOutput (Kryo kryo, Output output) {
		if (objectOutput == null)
			objectOutput = new KryoObjectOutput(kryo, output);
		else
			objectOutput.setOutput(output);
		return objectOutput;
	}

	private ObjectInput getObjectInput (Kryo kryo, Input input) {
		if (objectInput == null)
			objectInput = new KryoObjectInput(kryo, input);
		else
			objectInput.setInput(input);
		return objectInput;
	}
}