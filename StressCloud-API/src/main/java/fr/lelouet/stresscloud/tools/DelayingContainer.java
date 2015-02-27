/**
 * 
 */
package fr.lelouet.stresscloud.tools;

/*
 * #%L
 * StressCloud-API
 * %%
 * Copyright (C) 2012 - 2015 Mines de Nantes
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import fr.lelouet.tools.containers.Container;

/**
 * container that waits till one item has been set. all calls to get() will be
 * blocked untill this moment.
 * 
 * @author Guillaume Le Louët
 * 
 */
public class DelayingContainer<E> extends Container<E> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(DelayingContainer.class);

	/** set to true when receiving an element */
	boolean received = false;

	@Override
	public void onReplace(E before, E after) {
		super.onReplace(before, after);
		synchronized (this) {
			received = true;
			this.notifyAll();
		}
	}

	@Override
	public void beforeGet(E accessed) {
		synchronized (this) {
			while (!received) {
				try {
					wait();
				} catch (InterruptedException e) {
					logger.warn("while waiting for item", e);
				}
			}
		}
		super.beforeGet(accessed);
	}

	public boolean contains() {
		return received;
	}

	public void reset() {
		synchronized (this) {
			received = false;
		}
	}
}
