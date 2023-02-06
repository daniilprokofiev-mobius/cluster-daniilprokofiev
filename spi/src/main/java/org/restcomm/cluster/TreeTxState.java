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

package org.restcomm.cluster;

import java.util.concurrent.ConcurrentHashMap;

import org.restcomm.cluster.data.TreeSegment;
import org.restcomm.cluster.data.WriteOperation;

/**
 * Holds last operation for specific tree path and its data
 * @author yulian.oifa
 *
 */
public class TreeTxState {
	private Boolean createdInThisTX;
	private Boolean isPreloaded=false;
	private WriteOperation operation;
	private Object data;
	private ConcurrentHashMap<TreeSegment<?>, TreeTxState> childs=null;
	
	public TreeTxState(Boolean createdInThisTX, WriteOperation operation, Object data) {
		this.createdInThisTX=createdInThisTX;
		this.operation = operation;
		this.data = data;
	}

	public WriteOperation getOperation() {
		return operation;
	}

	public Object getData() {
		return data;
	}
	
	public Boolean isPreloaded() {
		return isPreloaded;
	}
	
	public void setIsPreloaded(Boolean value) {
		this.isPreloaded=value;
	}
	
	public Boolean createdInThisTX() {
		return createdInThisTX;
	}
	
	public void updateOperation(Boolean createdInThisTX, WriteOperation operation,Object data) {
		this.createdInThisTX=createdInThisTX;
		this.operation=operation;
		this.data=data;
	}
	
	public ConcurrentHashMap<TreeSegment<?>, TreeTxState> getAllChilds() {
		return childs;
	}
	
	public void clearChilds() {
		childs=null;
	}
	
	public void setChilds(ConcurrentHashMap<TreeSegment<?>, TreeTxState> map) {
		this.childs=map;
	}
	
	public void addChild(TreeSegment<?> key,TreeTxState state) {
		if(childs==null)
			childs=new ConcurrentHashMap<TreeSegment<?>, TreeTxState>();
		
		childs.put(key, state);
	}
	
	public TreeTxState getChild(TreeSegment<?> segment) {
		if(childs==null)
			return null;
		
		return childs.get(segment);
	}
}