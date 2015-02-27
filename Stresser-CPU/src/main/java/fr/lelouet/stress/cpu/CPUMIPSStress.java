package fr.lelouet.stress.cpu;

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
import java.util.concurrent.Semaphore;

import fr.lelouet.stresscloud.BurstStress;

/**
 * in this stresser, the load refers to the number of Million Instruction per
 * Seconds to try to execute. Basically, with a {@link #getLoopSeconds() loop
 * seconds} of 0.1, it will try to execute 0.1* {@link #getLoad()}*
 * {@link #ITERATIONPERUNIT} iteration per loop.
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public class CPUMIPSStress extends BurstStress {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(CPUMIPSStress.class);

	protected class DirectBurner extends Semaphore implements Runnable {

		private static final long serialVersionUID = 1L;

		public DirectBurner() {
			super(0);
		}

		public Thread mThread;

		Semaphore onEnd = null;

		/**
		 * start the thread to execute given number of instructions, then
		 * release one to the semaphore
		 */
		public void startBurn(Semaphore onEnd) {
			this.onEnd = onEnd;
			if (mThread == null || !mThread.isAlive()) {
				mThread = new Thread(this);
				mThread.start();
			}
			release();
		}

		@Override
		public void acquire() {
			try {
				super.acquire();
			} catch (InterruptedException e) {
				throw new UnsupportedOperationException(e);
			}
		}

		@Override
		public void run() {
			while (keepsRunning()) {
				acquire();
				// System.err.println("acquired " + iterationsPerThread
				// + " to burn");
				makeiterations(iterationsPerThread);
				onEnd.release();
			}
		}
	}

	ArrayList<DirectBurner> burnThreads = new ArrayList<CPUMIPSStress.DirectBurner>();

	protected int nbThreads = 1;

	protected long iterationsPerThread = 0;

	/**
	 * set the number of threads to use for the stresser
	 * 
	 * @param nbThreads
	 *            number of stress threads
	 */
	public void setNbThreads(int nbThreads) {
		for (int i = burnThreads.size(); i < nbThreads; i++) {
			DirectBurner thread = new DirectBurner();
			burnThreads.add(thread);
		}
		this.nbThreads = nbThreads;
	}

	/**
	 * resulted from a burn on a computer : getting the effective consumed CPU
	 * of the host for a load of 1000. this resulted in 1GHz consumed.
	 */
	protected static final long ITERATIONPERUNIT = 287000;

	protected double iterationsmultiplier = 1;

	/**
	 * 
	 * @return the internal multiplier used to know how many operations we do
	 *         per loop. Allows to take in consideration architecture
	 *         modifications.
	 */
	public double getIterationsmultiplier() {
		return iterationsmultiplier;
	}

	/**
	 * set the weight of instruction (default 1). Use it when you want ie double
	 * all the load
	 */
	public void setIterationsmultiplier(double iterationsmultiplier) {
		this.iterationsmultiplier = iterationsmultiplier;
	}

	/**
	 * a static value used by anyone to prevent the compiler from skipping the
	 * comparison phase. A for(int i=0 to MAX) has no memory impact and thus
	 * would be skipped by the compiler. this should not be modified, but the
	 * compiler does not know it.
	 */
	public static long sharedVariable = 0;

	protected static void makeiterations(long actualBurns) {
		for (long remaining = actualBurns; remaining > 0; sharedVariable = remaining--) {
		}
	}

	@Override
	protected double makeAtomicStress(final double bogoMi) {
		long actualBurns = (long) (bogoMi * ITERATIONPERUNIT * iterationsmultiplier);
		if (nbThreads == 1) {
			makeiterations(actualBurns);
			return bogoMi;
		} else {
			Semaphore sem = new Semaphore(0);
			int nbthreads = nbThreads;
			iterationsPerThread = actualBurns / nbthreads;
			// System.err.println("it per thread : " + iterationsPerThread);
			if (iterationsPerThread < 1) {
				return 0;
			}
			for (int i = 0; i < nbthreads; i++) {
				DirectBurner db = burnThreads.get(i);
				db.startBurn(sem);
			}
			try {
				// System.err.println("acquiring " + nbthreads
				// + " running thread end");
				sem.acquire(nbthreads);
				// System.err.println("acquired " + nbthreads);
			} catch (InterruptedException e) {
				throw new UnsupportedOperationException(e);
			}
			return iterationsPerThread * nbthreads / iterationsmultiplier
					/ ITERATIONPERUNIT;
		}
	}

	@Override
	public String getType() {
		return TYPES.CPU.toString();
	}

	@Override
	public String getLoadUnit() {
		return "BOGOMips to execute on the core";
	}

	@Override
	public void stop() {
		super.stop();
		for (DirectBurner db : burnThreads) {
			db.release();
		}
	}
}
