package fr.lelouet.stresscloud.actions;

/*
 * #%L
 * stresscloud
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

import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.lelouet.stresscloud.commands.Command;
import fr.lelouet.stresscloud.control.BasicRegisteredVM;

/**
 * a Action executing a command.
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public class SendCommand implements Action {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory
			.getLogger(SendCommand.class);

	Command data = null;

	Semaphore locked = null;

	public SendCommand(Command send) {
		this(send, null);
	}

	/**
	 * @param send
	 *            the command to send
	 * @param locked
	 *            the Semaphore to {@link Semaphore.#release()} when the command
	 *            has returned. default is null, meaning "no callback". If the
	 *            return value is interesting, it will be anyhow accessed by the
	 */
	public SendCommand(Command send, Semaphore locked) {
		data = send;
		this.locked = locked;
	}

	@Override
	public void apply(BasicRegisteredVM target) {
		target.sendData(data.toString());
	}

	@Override
	public long syncedId() {
		return data.blocking() ? data.getId() : -1;
	}

	@Override
	public String toString() {
		return "send " + data;
	}

}
