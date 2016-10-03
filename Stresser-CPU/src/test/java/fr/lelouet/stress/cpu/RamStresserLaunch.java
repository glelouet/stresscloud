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

import java.io.BufferedReader;
import java.io.InputStreamReader;

import fr.lelouet.stress.cpu.UnsafeRamWriterStress;

/**
 * run a RAMStresser and set the load to the values given in stdin.
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public class RamStresserLaunch {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(RamStresserLaunch.class);

	public static void main(String[] args) {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		UnsafeRamWriterStress stress = new UnsafeRamWriterStress();
		stress.setLoopMS(1);
		stress.start();
		String res = null;
		do {
			try {
				res = br.readLine();
				stress.setLoad(Double.parseDouble(res));
			} catch (Exception e) {
				logger.warn("", e);
			}
		} while (res != null);
	}
}
