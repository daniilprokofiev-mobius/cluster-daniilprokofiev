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
package org.restcomm.cluster.data;

import java.io.Serializable;
import java.util.Comparator;
/**
 * 
 * @author yulian.oifa
 * 
 */
public class TreeSegmentComparator implements Comparator<TreeSegment<?>>, Serializable {
	public static final TreeSegmentComparator INSTANCE = new TreeSegmentComparator();
	private static final long serialVersionUID = -1357631755443829281L;

	/**
	 * Returns -1 if the first comes before; 0 if they are the same; 1 if the second Fqn comes before.  <code>null</code>
	 * always comes first.
	 */
	@Override
	public int compare(TreeSegment<?> segment1, TreeSegment<?> segment2) {
		Object[] segments1=segment1.getList();
		Object[] segments2=segment2.getList();
		
		int s1 = segments1.length;
		int s2 = segments2.length;

		if (s1 == 0) {
			return (s2 == 0) ? 0 : -1;
		}

		if (s2 == 0) {
			return 1;
		}

		int size = Math.min(s1, s2);

		for (int i = 0; i < size; i++) {
			Object currSegment1=segments1[i];
			Object currSegment2=segments2[i];
			
			if (currSegment1 == currSegment2) {
				continue;
			}
			if (currSegment1 == null) {
				return 0;
			}
			if (currSegment2 == null) {
				return 1;
			}
			if (!currSegment1.equals(currSegment2)) {
				int c = compareElements(currSegment1, currSegment2);
	            if (c != 0) {
	               return c;
	            }
			}
		}

		return s1 - s2;
	}

	/**
	 * Compares two Fqn elements. If e1 and e2 are the same class and e1 implements Comparable, returns e1.compareTo(e2).
	 * Otherwise, returns e1.toString().compareTo(e2.toString()).
	 */
	@SuppressWarnings("unchecked")
	private int compareElements(Object e1, Object e2) {
		if (e1.getClass() == e2.getClass() && e1 instanceof Comparable) {
			return ((Comparable<Object>) e1).compareTo(e2);
		} else {
			return e1.toString().compareTo(e2.toString());
		}
	}
}