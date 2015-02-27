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

import java.util.HashMap;

import fr.lelouet.stress.Calibration;
import fr.lelouet.stresscloud.BurstStress;

/**
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 * 
 */
public class CalibrateStressers {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(CalibrateStressers.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BurstStress[] stresses = new BurstStress[]{new CPUMIPSStress(), /**/
		/* new UnsafeRamWriterStress() /* */};
		HashMap<BurstStress, Double> calibrations = new HashMap<BurstStress, Double>();
		for (BurstStress run : stresses) {
			run.setLoopMS(5);
			run.start();
			run.waitForRunning();
			calibrations.put(run, Calibration.calibrate(run, 0.10));
			run.stop();
		}
		System.out.println(calibrations);
	}
}
