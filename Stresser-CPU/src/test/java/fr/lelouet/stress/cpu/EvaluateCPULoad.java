package fr.lelouet.stress.cpu;

/*
 * #%L
 * Stresser-CPU
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

import fr.lelouet.stress.StressBencher;

/**
 * @author Guillaume Le Louët [guillaume.lelouet@gmail.com] 2013
 * 
 */
public class EvaluateCPULoad {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(EvaluateCPULoad.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		CPUMIPSStress stress = new CPUMIPSStress();
		stress.setNbThreads(4);
		stress.setLoopMS(200);
		StressBencher bench = new StressBencher();
		bench.target = stress;
		bench.makeLoads(stress.benchMaxLoad() * 1.5, 10);
		bench.setFile();
		bench.adaptationPeriod = 5 * 1000;
		bench.monitoringPeriod = 2 * 1000;
		bench.run();
		stress.stop();
	}
}
