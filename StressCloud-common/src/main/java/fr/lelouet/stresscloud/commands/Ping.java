package fr.lelouet.stresscloud.commands;

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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.lelouet.stresscloud.export.VMExporter;

/**
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public class Ping extends ACommand {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(Ping.class);

	int num = 0;

	public Ping() {
		type = null;
	}

	public Ping(int num) {
		this.num = num;
		type = null;
	}

	@Override
	public Object apply(VMExporter target) {
		return num + 1;
	}

	public static final String PREFIX = "PNG";

	@Override
	public String toString() {
		return getId() + "=" + PREFIX + " " + num;
	}

	protected static final Pattern p = Pattern.compile("(\\d+)=" + PREFIX
			+ " ([0-9]+)");

	/**
	 * try to parse a string to a ping command
	 * 
	 * @param exp
	 *            the expression to parse
	 * @return a ping command corresponding to that expression, or null.
	 */
	public static Ping parsePing(String exp) {
		Matcher m = p.matcher(exp);
		if (!m.matches()) {
			return null;
		}
		Ping ret = new Ping();
		ret.id = Long.parseLong(m.group(1));
		ret.num = Integer.parseInt(m.group(2));
		return ret;
	}
}
