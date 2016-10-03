package fr.lelouet.stresscloud.commands;

/*
 * #%L
 * stresscloud
 * %%
 * Copyright (C) 2012 - 2016 Mines de Nantes
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

import fr.lelouet.stresscloud.control.RegisteredVM;

/**
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public abstract class ACommand implements Command {

	private static final long serialVersionUID = 1L;

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(ACommand.class);

	public String type = null;

	public ACommand() {
	}

	public ACommand(String type) {
		this.type = type;
	}

	@Override
	public String getTypeTarget() {
		return type;
	}

	@Override
	public void setTypeTarget(String t) {
		type = t;
	}

	public long id = 0;

	@Override
	public long getId() {
		return id;
	}

	@Override
	public void setId(long id) {
		this.id = id;
	}

	@Override
	public abstract String toString();

	/**
	 * parse an String to a Command.
	 * 
	 * @param exp
	 *            the expression to parse to a command
	 * @return the command resulting from the parsing of exp, or null if
	 *         exp==null, or a {@link Ping} if unrecognized Command
	 */
	public static Command parse(String exp) {
		if (exp == null) {
			return null;
		}
		Command ret = Get.parseGet(exp);
		if (ret != null) {
			return ret;
		}
		ret = Set.parseSet(exp);
		if (ret != null) {
			return ret;
		}
		ret = Sync.parseSync(exp);
		if (ret != null) {
			return ret;
		}
		ret = Ping.parsePing(exp);
		if (ret != null) {
			return ret;
		}
		logger.warn("cannot parse Command " + exp + " , returning a ping");
		return new Ping();
	}

	protected boolean synced = true;

	@Override
	public boolean synced() {
		return synced;
	}

	/**
	 * set the synced state of this command. Default should be true. false means
	 * the {@link RegisteredVM} that sends this will not wait for its response
	 * to send next command.
	 */
	public void setSynced(boolean synced) {
		this.synced = synced;
	}

	protected boolean blocking = true;

	@Override
	public boolean blocking() {
		return blocking;
	}

	public void setBlocking(boolean blocking) {
		this.blocking = blocking;
	}
}
