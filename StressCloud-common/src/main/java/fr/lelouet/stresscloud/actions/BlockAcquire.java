package fr.lelouet.stresscloud.actions;

/* #%L
 * stresscloud
 * %%
 * Copyright (C) 2012 - 2015 Mines de Nantes
 * %%
 * This program
 * is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Lesser Public License
 * for more details. You should have received a copy of the GNU General Lesser
 * Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L% */

import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.lelouet.stresscloud.control.BasicRegisteredVM;

/**
 * a command that blocks until somewhere else the semaphore is released or the
 * thread is interrupted
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public class BlockAcquire implements Action {

	private static final Logger logger = LoggerFactory
			.getLogger(BlockAcquire.class);

	final Semaphore wait;
	int needed = 1;

	public BlockAcquire() {
		this(new Semaphore(0));
	}

	public BlockAcquire(Semaphore wait) {
		this(wait, 1);
	}

	public BlockAcquire(Semaphore wait, int needed) {
		super();
		this.wait = wait;
		this.needed = needed;
	}

	@Override
	public void apply(BasicRegisteredVM target) {
		try {
			wait.acquire(needed);
		} catch (InterruptedException e) {
			logger.warn("", e);
		}
	}

	@Override
	public long syncedId() {
		return hashCode();
	}

	public Semaphore getSem() {
		return wait;
	}
}
