package org.restcomm.cluster.data;
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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Set;

import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.util.Util;
import org.restcomm.cluster.ClusteredID;

import net.jcip.annotations.Immutable;

/**
 * 
 * abstract tree data key segment
 * 
 * @author yulian.oifa
 *
 */
@Immutable
public abstract class AbstractTreeSegment<T> implements TreeSegment<T> {
	private static final long serialVersionUID = 1L;
	
	private Object[] list;
	private Integer hashCode;
	
	public AbstractTreeSegment(T value, TreeSegment<?> parent) {
		this.list = asList(parent,value);
		hashCode();
	}
	
	protected AbstractTreeSegment(Object[] list) {
		this.list = list;
		hashCode();
	}
	
	protected AbstractTreeSegment() {
		this.list = RootTreeSegment.EMPTY_ARRAY;
		hashCode();
	}

	@Override
	public TreeSegment<?> getParent() {
		if (list.length==0)
			return null;
		else if(list.length==1)
			return RootTreeSegment.INSTANCE;

		Object[] list=new Object[this.list.length-1];
		System.arraycopy(this.list, 0, list, 0, this.list.length-1);
		
		if (list[list.length-1] instanceof String)
			return new StringTreeSegment(list);
		else if (list[list.length-1] instanceof ClusteredID<?>)
			return new ClusteredIDTreeSegment(list);
		else if (list[list.length-1] instanceof ClusteredIDAndStringKey)
			return new ClusteredIDAndStringTreeSegment(list);
		else if (list[list.length-1] instanceof StringAndClusteredIDKey)
			return new StringAndClusteredIDTreeSegment(list);
		else if (list[list.length-1] instanceof MultiStringKey)
			return new MultiStringTreeSegment(list);

		throw new RuntimeException("Unsupported object has been found for tree segment" + list[list.length-1].getClass().getCanonicalName());		
	}

	@SuppressWarnings("unchecked")
	@Override
	public T getSegment() {
		if(list.length==0)
			return null;
		
		return (T)list[list.length-1];
	}

	@Override
	public int hashCode() {
		if(hashCode!=null)
			return hashCode;
		
		int result = 19;
		if(list!=null)
			for (Object o : list) result = 31 * result + o.hashCode();
		
		hashCode=result;
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
		
		AbstractTreeSegment<?> other = (AbstractTreeSegment<?>) obj;
		if(this.hashCode!=null && other.hashCode!=null)
			if(!this.hashCode.equals(other.hashCode))
				return false;
		
		Object[] segments1=list;
		Object[] segments2=other.list;
		
		if(segments1==null)
			segments1=RootTreeSegment.EMPTY_ARRAY;
		
		if(segments2==null)
			segments2=RootTreeSegment.EMPTY_ARRAY;
		
		int s1 = segments1.length;
		int s2 = segments2.length;

		if (s1!=s2) {
			return false;
		}

		for (int i = 0; i < s1; i++) {
			if (!segments1[i].equals(segments2[i]))
				return false;			
		}

		return true;
	}

	/**
	 * Compares this Segment to another
	 */
	@Override
	public int compareTo(TreeSegment<?> segment) {
		return TreeSegmentComparator.INSTANCE.compare(this, segment);
	}

	private Object[] asList(TreeSegment<?> parent,T value) {		
		Object[] list;
		if(parent!=null && !parent.isRoot()) {
			list = new Object[parent.getList().length+1];
			System.arraycopy(parent.getList(), 0, list, 0, list.length-1);
		}
		else
			list = new Object[1];
		
		list[list.length-1]=value;
		return list;
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		Object[] list = getList();
		if(list==null)
			out.write(0);
		else {
			out.writeByte(list.length);
			for (Object curr : list)
				out.writeObject(curr);
		}
	}
		     
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {	
		int segments = in.readByte() & 0x0FF;
		list = new Object[segments];
		if(segments==0)
			return;
		
		for(int i=0;i<segments;i++)
			list[i]=in.readObject();
	}

	@Override
	public Object[] getList() {
		return this.list;
	}

	@Override
	public TreeSegment<?> getChild(Object child) {
		if (child instanceof String)
			return new StringTreeSegment((String) child, this);
		else if (child instanceof ClusteredID<?>)
			return new ClusteredIDTreeSegment((ClusteredID<?>) child, this);
		else if (child instanceof ClusteredIDAndStringKey)
			return new ClusteredIDAndStringTreeSegment((ClusteredIDAndStringKey)child, this);
		else if (child instanceof StringAndClusteredIDKey)
			return new StringAndClusteredIDTreeSegment((StringAndClusteredIDKey)child, this);
		else if (child instanceof MultiStringKey)
			return new MultiStringTreeSegment((MultiStringKey)child, this);
		
		throw new RuntimeException(
				"Unsupported object has been found for tree segment" + child.getClass().getCanonicalName());
	}

	public String toString() {
		return Arrays.toString(list);
	}	
	
	public static class Externalizer extends AbstractExternalizer<AbstractTreeSegment<?>> {
		private static final long serialVersionUID = 1L;

		public static final Integer EXTERNALIZER_ID=2000;
		
		@Override
		public void writeObject(ObjectOutput output, AbstractTreeSegment<?> segment) throws IOException {
			Object[] list = segment.getList();
			if(list==null)
				output.write(0);
			else {
				output.writeByte(list.length);
				for (Object curr : list)
					output.writeObject(curr);
			}
		}

		@Override
		public AbstractTreeSegment<?> readObject(ObjectInput input) throws IOException, ClassNotFoundException {
			int segments = input.readByte() & 0x0FF;
			if(segments==0)
				return RootTreeSegment.INSTANCE;
			
			Object[] list = new Object[segments];
			for(int i=0;i<segments;i++)
				list[i]=input.readObject();
			
			if (list[list.length-1] instanceof String)
				return new StringTreeSegment(list);
			else if (list[list.length-1] instanceof ClusteredID<?>)
				return new ClusteredIDTreeSegment(list);
			else if (list[list.length-1] instanceof ClusteredIDAndStringKey)
				return new ClusteredIDAndStringTreeSegment(list);
			else if (list[list.length-1] instanceof StringAndClusteredIDKey)
				return new StringAndClusteredIDTreeSegment(list);
			else if (list[list.length-1] instanceof MultiStringKey)
				return new MultiStringTreeSegment(list);
			
			throw new RuntimeException("Unsupported object has been found for tree segment" + list[list.length-1].getClass().getCanonicalName());
		}

		@Override
		public Set<Class<? extends AbstractTreeSegment<?>>> getTypeClasses() {
			return Util.<Class<? extends AbstractTreeSegment<?>>>asSet(RootTreeSegment.class, ClusteredIDTreeSegment.class, StringTreeSegment.class, ClusteredIDAndStringTreeSegment.class, StringAndClusteredIDTreeSegment.class, MultiStringTreeSegment.class);			
		}
		
		@Override
		public Integer getId() {
			return EXTERNALIZER_ID;
		}
	}
}