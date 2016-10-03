/**
 * 
 */
package fr.lelouet.stresscloud.control;

/*
 * #%L
 * StressCloud-API
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

import java.util.ArrayList;
import java.util.Map.Entry;

/**
 * @author Guillaume Le Louët
 * 
 */
public class LoadList extends TemplateWorkList<Load> {

	private static final long serialVersionUID = 1L;

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(LoadList.class);

	public LoadList() {
		super();
	}

	/**
	 * @see TemplateWorkList.#TemplateWorkList(ArrayList, RegisteredWorker...)
	 */
	public LoadList(ArrayList<Load> elems,
			RegisteredWorker... registeredStressers) {
		super(elems, registeredStressers);
	}

	@Override
	public StringBuilder toCSV(Load elem) {
		StringBuilder sb = null;
		long time = elem.startTime;
		double lasterror = 0;
		double load = elem.getRequestedLoad();
		for (Entry<Long, Double> e : elem.skippedVals.entrySet()) {
			if (sb == null) {
				sb = new StringBuilder();
			} else {
				sb.append('\n');
			}
			try {
				sb.append(time / 1000)
						.append(", ")
						.append(e.getKey() / 1000)
						.append(", ")
						.append(load)
						.append(", ")
						.append((e.getValue() - lasterror)
								/ (e.getKey() - time));
				time = e.getKey();
				lasterror = e.getValue();
			} catch (Exception ex) {
				logger.warn("", ex);
			}
		}
		if (sb == null) {
			sb = new StringBuilder();
		} else {
			sb.append('\n');
		}
		if (time / 1000 != elem.endTime / 1000) {
			sb.append(time / 1000).append(", ").append(elem.endTime / 1000)
					.append(", ").append(load).append(", ").append(0);
		}
		return sb != null ? sb : new StringBuilder();
	}
}
