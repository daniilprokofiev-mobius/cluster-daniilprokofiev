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

package org.restcomm.timers;

/**
 * 
 * A thread local used to store the a tx context.
 * 
 * @author martins
 * @author yulian.oifa
 * 
 */
public class TransactionContextThreadLocal {

	/**
	 * 
	 */
	private static final ThreadLocal<TransactionContext> transactionContext = new ThreadLocal<TransactionContext>();

	/**
	 * 
	 * @param txContext
	 */
	public static void setTransactionContext(TransactionContext txContext) {
		transactionContext.set(txContext);
	}

	/**
	 * 
	 * @return
	 */
	public static TransactionContext getTransactionContext() {
		return transactionContext.get();
	}

}
