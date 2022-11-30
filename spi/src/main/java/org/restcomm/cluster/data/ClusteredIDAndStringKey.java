package org.restcomm.cluster.data;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.restcomm.cluster.ClusteredID;

/**
 * @author yulian.oifa
 */
public class ClusteredIDAndStringKey implements Externalizable {
	public static final Integer EXTERNALIZER_ID=2004;
	
	private ClusteredID<?> id;
	private String str;
	
	public ClusteredIDAndStringKey() {
		
	}
	
	public ClusteredIDAndStringKey(ClusteredID<?> id,String str) {
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
		return id.toString() + ":" + str;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		id=(ClusteredID<?>)in.readObject();
		str=in.readUTF();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(id);
		out.writeUTF(str);
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
		ClusteredIDAndStringKey other = (ClusteredIDAndStringKey) obj;
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