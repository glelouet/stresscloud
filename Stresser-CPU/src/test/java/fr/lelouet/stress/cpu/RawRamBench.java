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

import fr.lelouet.stress.StressBencher;

/**
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public class RawRamBench {

	@SuppressWarnings("unused")
	private static org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(RawRamBench.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		for (boolean enMasse : new Boolean[]{true, false}) {
			for (int loopms : new Integer[]{5, 50, 200}) {
				UnsafeRamWriterStress stress = new UnsafeRamWriterStress();
				stress.setLoopMS(loopms);
				stress.setMassMode(enMasse);
				StressBencher bench = new StressBencher();
				bench.target = stress;
				// bench.setFile();
				bench.run();
			}
		}
	}
}
