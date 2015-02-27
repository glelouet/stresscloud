/**
 * 
 */
package fr.lelouet.stresscloud.control;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * @author Guillaume Le Louët
 * 
 */
public class TemplateWorkList<T extends Work> extends ArrayList<T> {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(TemplateWorkList.class);

	/**
	 * create a new list with given elems, and removes all elems wich do not
	 * match the filter
	 * 
	 * @param elems
	 *            the elems to add
	 * @param registeredStressers
	 *            the workers to keeps the elems, or null to keep all elems; the
	 *            list is expanded to the stressers of the vms
	 */
	public TemplateWorkList(ArrayList<T> elems,
			RegisteredWorker... registeredStressers) {
		HashSet<RegisteredWorker> workers = null;
		if (registeredStressers != null && registeredStressers.length != 0) {
			workers = new HashSet<RegisteredWorker>();
			for (RegisteredWorker w : registeredStressers) {
				if (w instanceof RegisteredVM) {
					workers.addAll(((RegisteredVM) w).getStressers());
				} else {
					workers.add(w);
				}
			}
		}
		synchronized (elems) {
			for (T w : elems) {
				if (workers == null || workers.contains(w.getWorker())) {
					add(w);
				}
			}
		}
	}

	public TemplateWorkList() {
	}

	/**
	 * removes all elements started strictly before given time
	 * 
	 * @param beginning
	 *            the time all elements should have been started after
	 * @return this, after removal of bad elems.
	 */
	public TemplateWorkList<T> since(long beginning) {
		Iterator<T> it = iterator();
		while (it.hasNext()) {
			Work elem = it.next();
			if (elem.getStartTime() < beginning) {
				it.remove();
			}
		}
		return this;
	}

	public HashMap<RegisteredWorker, TemplateWorkList<T>> byWorker() {
		HashMap<RegisteredWorker, TemplateWorkList<T>> ret = new HashMap<RegisteredWorker, TemplateWorkList<T>>();
		for (T t : this) {
			TemplateWorkList<T> m = ret.get(t.getWorker());
			if (m == null) {
				m = new TemplateWorkList<T>();
				ret.put(t.getWorker(), m);
			}
			m.add(t);
		}
		return ret;
	}

	/**
	 * remove all the elements with a duration lower than given number of
	 * seconds
	 */
	public TemplateWorkList<T> minDuration(long seconds) {
		Iterator<T> it = iterator();
		while (it.hasNext()) {
			Work elem = it.next();
			if (elem.getDuration() < seconds * 1000) {
				it.remove();
			}
		}
		return this;
	}

	public String toCSV() {
		StringBuilder sb = new StringBuilder();
		HashMap<RegisteredWorker, TemplateWorkList<T>> m = this.byWorker();
		for (Entry<RegisteredWorker, TemplateWorkList<T>> e : m.entrySet()) {
			sb.append('\n').append(e.getKey().toString()).append('\n');
			for (T w : e.getValue()) {
				// System.err.println("work " + w + " associated");
				sb.append(toCSV(w)).append('\n');
			}
		}
		return sb.toString();
	}

	public StringBuilder toCSV(T elem) {
		return new StringBuilder("" + elem.startTime / 1000).append(", ")
				.append(elem.endTime / 1000).append(", ")
				.append(elem.requestedLoad);
	}

}
