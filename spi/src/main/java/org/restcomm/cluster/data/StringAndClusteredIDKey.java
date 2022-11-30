package org.restcomm.cluster.data;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.restcomm.cluster.ClusteredID;

/**
 * @author yulian.oifa
 */
public class StringAndClusteredIDKey implements Externalizable {
	public static final Integer EXTERNALIZER_ID=2003;
	
	private String str;
	private ClusteredID<?> id;
	
	public StringAndClusteredIDKey() {
		
	}
	
	public StringAndClusteredIDKey(String str,ClusteredID<?> id) {
		this.id=id;
		this.str=str;
	}

	public ClusteredID<?> getId() {
		return id;
	}

	public void setId(ClusteredID<?> id) {
		this.id = id;
	}

	public String getStr() {
		return str;
	}

	public void setStr(String str) {
		this.str = str;
	}
	
	public String toString() {
		return str + ":" + id.toString();
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		str=in.readUTF();
		id=(ClusteredID<?>)in.readObject();		
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeUTF(str);
		out.writeObject(id);		
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((str == null) ? 0 : str.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StringAndClusteredIDKey other = (StringAndClusteredIDKey) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (str == null) {
			if (other.str != null)
				return false;
		} else if (!str.equals(other.str))
			return false;
		return true;
	}		
}