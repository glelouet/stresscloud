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

import fr.lelouet.stresscloud.Stresser;
import fr.lelouet.stresscloud.export.VMExporter;

/**
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 * 
 */
public class Set extends ACommand {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(Set.class);

	public Set() {
	}

	public Set(String key, String value, String type) {
		super(type);
		this.key = key;
		this.value = value;
		setSynced(true);
	}

	public String key, value;

	@Override
	public Object apply(VMExporter target) {
		if (type != null) {
			Stresser s = target.getStresser(type);
			// System.err.println("applying to stresser " + s);
			return s.set(key, value);
		}
		for (String type : target.getStressersTypes()) {
			target.getStresser(type).set(key, value);
		}
		return "ok";
	}

	public static final String PREFIX = "SET";

	@Override
	public String toString() {
		return "" + id + "=" + PREFIX + " " + getTypeTarget() + "." + key + "="
				+ value;
	}

	protected static final Pattern p = Pattern.compile("(\\d+)=" + PREFIX
			+ " ([A-Za-z0-9]+)\\.(.*)=(.*)");

	public static Set parseSet(String exp) {
		Matcher m = p.matcher(exp);
		if (!m.matches()) {
			return null;
		}
		Set ret = new Set();
		ret.id = Long.parseLong(m.group(1));
		ret.type = "null".equals(m.group(2)) ? null : m.group(2);
		ret.key = m.group(3);
		ret.value = m.group(4);
		return ret;
	}
}
