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

import java.io.IOException;
import java.util.ArrayList;

import fr.lelouet.stress.RunLocalStresser;
import fr.lelouet.stresscloud.StressMultiplexer;

/**
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 * 
 */
public class StressLocalCPU
		extends
			RunLocalStresser<StressMultiplexer<CPUMIPSStress>> {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(StressLocalCPU.class);

	public static final String THREADSKEY = "threads=";

	{
		stress = new StressMultiplexer<CPUMIPSStress>();
		stress.getStressers().add(new CPUMIPSStress());
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		new StressLocalCPU().run(args);
	}

	@Override
	public void applyToken(String token) {
		if (token.startsWith(THREADSKEY)) {
			setThreads(Integer.parseInt(token.substring(THREADSKEY.length())));
		} else {
			super.applyToken(token);
		}
	}

	protected void setThreads(int nbThreads) {
		System.out.println("using " + nbThreads + " thread(s)");
		int toAdd = nbThreads - stress.getStressers().size();
		ArrayList<CPUMIPSStress> mstressers = stress.getStressers();
		for (int i = toAdd; i < 0; i++) {
			CPUMIPSStress rm = mstressers.remove(mstressers.size() - 1);
			rm.setLoopMS(stress.getLoopMS());
			rm.stop();
		}
		for (int i = 0; i < toAdd; i++) {
			CPUMIPSStress added = new CPUMIPSStress();
			added.start();
			mstressers.add(added);
		}
		stress.setLoad(stress.getLoad());
	}

	@Override
	public String getHelp() {
		return super.getHelp() + "\n " + THREADSKEY + "nb of threads to use";
	}

	@Override
	public void export(String fileName) {
		res.setProperty("threads", "" + stress.getStressers().size());
		super.export(fileName);
	}
}
