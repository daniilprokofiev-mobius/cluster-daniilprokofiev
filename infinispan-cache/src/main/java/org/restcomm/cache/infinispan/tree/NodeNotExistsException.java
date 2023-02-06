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

package org.restcomm.cache.infinispan.tree;

import org.infinispan.commons.CacheException;
/*
 * This is modified copy of infinispan implementation for tree
 * following changes are done
 * minimzed support for operations -> some are not required for the project
 * number of root nodes is configurable. This is because the root node becomes
 * main botleneck for the perfomance
 * The number of nodes per tree has been decreased from 2 to 1 
 * This implementation supports either data nodes or tree nodes while infinispan implementation
 * supported mixed mode
 */
/**
 * @author yulian.oifa
 */
public class NodeNotExistsException extends CacheException {

	private static final long serialVersionUID = 779376138690777440L;

	public NodeNotExistsException() {
		super();
	}

	public NodeNotExistsException(String msg) {
		super(msg);
	}

	public NodeNotExistsException(String msg, Throwable cause) {
		super(msg, cause);
	}
}