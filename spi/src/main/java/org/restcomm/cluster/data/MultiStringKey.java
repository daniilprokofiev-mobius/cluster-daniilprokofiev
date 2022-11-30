package org.restcomm.cluster.data;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * @author yulian.oifa
 */
public class MultiStringKey implements Externalizable {
	public static final Integer EXTERNALIZER_ID=2002;
	
	private String str1;
	private String str2;
	
	public MultiStringKey() {
		
	}
	
	public MultiStringKey(String str1,String str2) {
		this.str1=str1;
		this.str2=str2;
	}

	public String getStr1() {
		return str1;
	}

	public void setId(String str) {
		this.str1 = str;
	}

	public String getStr2() {
		return str2;
	}

	public void setStr2(String str) {
		this.str2 = str;
	}
	
	public String toString() {
		return str1 + ":" + str2;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		str1=in.readUTF();
		str2=in.readUTF();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeUTF(str1);
		out.writeUTF(str2);		
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((str1 == null) ? 0 : str1.hashCode());
		result = prime * result + ((str2 == null) ? 0 : str2.hashCode());
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
		MultiStringKey other = (MultiStringKey) obj;
		if (str1 == null) {
			if (other.str1 != null)
				return false;
		} else if (!str1.equals(other.str1))
			return false;
		if (str2 == null) {
			if (other.str2 != null)
				return false;
		} else if (!str2.equals(other.str2))
			return false;
		return true;
	}		
}