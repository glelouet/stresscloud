package fr.lelouet.stress.cpu;

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

import fr.lelouet.stresscloud.BurstStress;

/**
 * stress a CPU using average usage.
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public class CPUAverageStress extends BurstStress {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(CPUAverageStress.class);

	Burner burner = null;

	class Burner implements Runnable {

		boolean burn = false;

		boolean stop = false;

		void burn() {
			burn = true;
			synchronized (this) {
				notify();
			}
		}

		void pause() {
			burn = false;
		}

		@Override
		public void run() {
			while (!stop) {
				synchronized (this) {
					try {
						wait();
					} catch (InterruptedException e) {
						logger.trace("", e);
					}
				}
				while (burn) {
					Math.sqrt(Math.random());
				}
			}
		}

		public void stop() {
			stop = true;
			burn = false;
			synchronized (this) {
				notify();
			}
		}
	};

	@Override
	protected void burnLoop() {
		if (burner == null) {
			burner = new Burner();
			new Thread(burner).start();
		}
		try {
			long burnTime = (long) (getLoopMS() * getLoad() / 100);
			long sleepTime = (long) (getLoopMS() * (100 - getLoad()) / 100);
			burner.burn();
			Thread.sleep(burnTime);
			burner.pause();
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			logger.trace("", e);
		}
	}

	@Override
	public String getType() {
		return TYPES.CPU.toString();
	}

	@Override
	protected double makeAtomicStress(double iterations) {
		return iterations;

	}

	@Override
	public String getLoadUnit() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("implement this !");
	}
}
