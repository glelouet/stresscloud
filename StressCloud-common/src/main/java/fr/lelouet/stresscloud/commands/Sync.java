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

import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.lelouet.stresscloud.Stresser;
import fr.lelouet.stresscloud.control.RegisteredVM;
import fr.lelouet.stresscloud.export.VMExporter;

/**
 * wait for the work of a {@link RegisteredVM}'s stressers to reach 0. If one
 * type is specified, only the stresser of this type work will be required to
 * reach 0. If null is specified, all stressers have to reach 0 work remaining.
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public class Sync extends ACommand {

	private static final long serialVersionUID = 1L;

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(Sync.class);

	public Sync() {
		this(null);
	}

	public Sync(String type) {
		super(type);
		synced = true;
	}

	/** calls parallel {@link Stresser.#after()} on each stresser. */
	@Override
	public Object apply(VMExporter target) {
		if (type == null) {
			final Semaphore required = new Semaphore(0);
			for (final String t : target.getStressersTypes()) {
				final Stresser s = target.getStresser(t);
				new Thread(new Runnable() {
					@Override
					public void run() {
						// System.err.println("getting after for type " + t +
						// ", stresser : "
						// + s);
						s.after();
						// System.err.println("got after for type " + t);
						required.release();
					}
				}).start();
			}
			try {
				required.acquire(target.getStressersTypes().size());
			} catch (InterruptedException e) {
				logger.warn("", e);
			}
		} else {
			target.getStresser(type).after();
		}
		return "0";
	}

	public static final String PREFIX = "SYN";

	@Override
	public String toString() {
		return getId() + "=" + PREFIX + " " + getTypeTarget();
	}

	protected static final Pattern p = Pattern.compile("(\\d+)=" + PREFIX
			+ " ([A-Za-z0-9]+)");

	/**
	 * try to parse a string to a sync command
	 * 
	 * @param exp
	 *            the expression to parse
	 * @return a sync command corresponding to that expression, or null.
	 */
	public static Sync parseSync(String exp) {
		Matcher m = p.matcher(exp);
		if (!m.matches()) {
			return null;
		}
		Sync ret = new Sync();
		ret.id = Long.parseLong(m.group(1));
		if ("null".equals(m.group(2))) {
			ret.type = null;
		} else {
			ret.type = m.group(2);
		}
		return ret;
	}
}
