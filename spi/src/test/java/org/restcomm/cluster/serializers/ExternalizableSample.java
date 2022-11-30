package org.restcomm.cluster.serializers;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.restcomm.cluster.ClusteredID;

public class ExternalizableSample implements Externalizable {
	private Boolean bValue;
	private Integer iValue;
	private Long lValue;	
	private String sValue;
	private ClusteredID<?> id;
	
	public ExternalizableSample() {
		
	}
	
	public ExternalizableSample (Boolean bValue,Integer iValue,Long lValue,String sValue,ClusteredID<?> id) {
		this.bValue=bValue;
		this.iValue=iValue;
		this.lValue=lValue;
		this.sValue=sValue;
		this.id=id;		
	}

	public Boolean getbValue() {
		return bValue;
	}

	public Integer getiValue() {
		return iValue;
	}

	public Long getlValue() {
		return lValue;
	}

	public String getsValue() {
		return sValue;
	}

	public ClusteredID<?> getId() {
		return id;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		bValue=in.readBoolean();
		iValue=in.readInt();
		lValue=in.readLong();
		sValue=in.readUTF();
		id=(ClusteredID<?>)in.readObject();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeBoolean(bValue);
		out.writeInt(iValue);
		out.writeLong(lValue);
		out.writeUTF(sValue);
		out.writeObject(id);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bValue == null) ? 0 : bValue.hashCode());
		result = prime * result + ((iValue == null) ? 0 : iValue.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((lValue == null) ? 0 : lValue.hashCode());
		result = prime * result + ((sValue == null) ? 0 : sValue.hashCode());
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
		ExternalizableSample other = (ExternalizableSample) obj;
		if (bValue == null) {
			if (other.bValue != null)
				return false;
		} else if (!bValue.equals(other.bValue))
			return false;
		if (iValue == null) {
			if (other.iValue != null)
				return false;
		} else if (!iValue.equals(other.iValue))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (lValue == null) {
			if (other.lValue != null)
				return false;
		} else if (!lValue.equals(other.lValue))
			return false;
		if (sValue == null) {
			if (other.sValue != null)
				return false;
		} else if (!sValue.equals(other.sValue))
			return false;
		return true;
	}
}