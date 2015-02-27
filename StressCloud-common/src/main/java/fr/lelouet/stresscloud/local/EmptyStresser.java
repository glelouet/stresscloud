package fr.lelouet.stresscloud.local;

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

import fr.lelouet.stresscloud.BurstStress;

/**
 * An empty {@link BurstStress} that does nothing.<br />
 * set its internal data : {@link #type}, {@link #loadUnit} , and
 * {@link #acceptStresses} to set whether it should return 0 or the number of
 * required executions on each burn.
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public class EmptyStresser extends BurstStress {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(EmptyStresser.class);

	public String type = TYPES.CPU.toString();

	public String loadUnit = "void";

	public EmptyStresser withType(String type) {
		this.type = type;
		return this;
	}

	/**
	 * set to false to return 0 on atomic stress demands, true to return the
	 * required work.<br />
	 * false means this can not produce work.
	 */
	public boolean acceptStresses = true;

	@Override
	protected double makeAtomicStress(double iterations) {
		return acceptStresses ? iterations : 0;
	}

	@Override
	public String getLoadUnit() {
		return loadUnit;
	}

	@Override
	public String getType() {
		return type;
	}

	/**
	 * set the type
	 * 
	 * @param type
	 *            the internal type to be set and then returned using
	 *            {@link #getType()}
	 * @return this, to use on constructor
	 */
	public EmptyStresser constructType(String type) {
		this.type = type;
		return this;
	}

	/**
	 * set the load unit
	 * 
	 * @param loadUnit
	 *            the internal unit to be set and then returned using
	 *            {@link #getLoadUnit()}
	 * @return this, to use on constructor
	 */
	public EmptyStresser constructLoadUnit(String loadUnit) {
		this.loadUnit = loadUnit;
		return this;
	}

	/**
	 * set the acceptstresses boolean
	 * 
	 * @param accept
	 *            the internal boolean to set to produce or not work on
	 *            {@link #makeAtomicStress(double)}
	 * @return this, to use on constructor
	 */
	public EmptyStresser constructAcceptStresses(boolean accept) {
		acceptStresses = accept;
		return this;
	}
}
