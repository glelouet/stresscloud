package fr.lelouet.stresscloud;

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

import java.util.ArrayList;

/**
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public class StressMultiplexer<T extends BurstStress> extends BurstStress {

	@SuppressWarnings("unused")
	private static org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(StressMultiplexer.class);

	protected ArrayList<T> stresses = new ArrayList<T>();

	public ArrayList<T> getStressers() {
		return stresses;
	}

	@Override
	public String getLoadUnit() {
		return stresses.get(0).getLoadUnit();
	}

	@Override
	public String getType() {
		return stresses.get(0).getType();
	}

	@Override
	protected double makeAtomicStress(double iterations) {
		throw new UnsupportedOperationException(
				"stressmultiplexer should not make atomic stresses");
	}

	@Override
	public void start() {
		for (T s : stresses) {
			s.start();
		}
	}

	@Override
	public void stop() {
		for (T s : stresses) {
			s.stop();
		}
	}

	@Override
	public void setLoad(double load) {
		super.setLoad(load);
		for (T s : stresses) {
			s.setLoad(load / stresses.size());
		}
	}

	@Override
	public long getSkipped() {
		long ret = 0;
		for (T s : stresses) {
			ret += s.getSkipped();
		}
		return ret;
	}

	@Override
	public void setLoopMS(long ms) {
		super.setLoopMS(ms);
		for (T s : stresses) {
			s.setLoopMS(ms);
		}
	}

	@Override
	public double getMaxUsagesPerSecond() {
		double ret = 0;
		for (T s : stresses) {
			ret += s.getMaxUsagesPerSecond();
		}
		return ret;
	}

	@Override
	public void setItPerLoop() {
	}
}
