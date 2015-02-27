package fr.lelouet.stress.disk;

/*
 * #%L
 * Stresser-DISK
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
public class EvaluateDiskLoad {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(EvaluateDiskLoad.class);

	/** @param args */
	public static void main(String[] args) {
		DiskBitWritter stress = new DiskBitWritter();
		StressBencher bench = new StressBencher();
		bench.target = stress;
		bench.makeLoads(stress.benchMaxLoad() * 1.5, 11);
		bench.setFile();
		bench.adaptationPeriod = 10 * 1000;
		bench.monitoringPeriod = 5 * 1000;
		bench.run();
		stress.stop();
	}
}
