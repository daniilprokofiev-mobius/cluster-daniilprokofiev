package org.restcomm.cluster;
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
import org.restcomm.cluster.data.WriteOperation;
/**
 * @author yulian.oifa
 */
public class TxState {
	private Boolean createdInThisTX;
	private WriteOperation operation;
	private Object data;

	public TxState(Boolean createdInThisTX, WriteOperation operation, Object data) {
		this.createdInThisTX = createdInThisTX;
		this.operation = operation;
		this.data = data;
	}

	public WriteOperation getOperation() {
		return this.operation;
	}

	public Object getData() {
		return this.data;
	}

	public Boolean createdInThisTX() {
		return this.createdInThisTX;
	}

	public void updateOperation(Boolean createdInThisTX, WriteOperation operation, Object data) {
		this.createdInThisTX = createdInThisTX;
		this.operation = operation;
		this.data = data;
	}
}
