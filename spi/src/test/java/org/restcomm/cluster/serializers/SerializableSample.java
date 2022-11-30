package org.restcomm.cluster.serializers;

import java.io.Serializable;

import org.restcomm.cluster.ClusteredID;

public class SerializableSample implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private Boolean bValue;
	private Integer iValue;
	private Long lValue;	
	private String sValue;
	private ClusteredID<?> id;
		
	public SerializableSample() {
		
	}
	
	public SerializableSample (Boolean bValue,Integer iValue,Long lValue,String sValue,ClusteredID<?> id) {
		this.bValue=bValue;
		this.bValue=bValue;
		this.iValue=iValue;
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
		SerializableSample other = (SerializableSample) obj;
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