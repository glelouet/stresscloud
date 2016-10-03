package fr.lelouet.stresscloud.control;

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

import org.testng.Assert;
import org.testng.annotations.Test;

import fr.lelouet.stresscloud.BurstStress;
import fr.lelouet.stresscloud.Stresser.TYPES;
import fr.lelouet.stresscloud.local.EmptyStresser;
import fr.lelouet.stresscloud.local.LocalEntryPoint;
import fr.lelouet.stresscloud.local.LocalRegisteredVM;
import groovy.lang.Closure;

/**
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 * 
 */
public class RegisteredStresserImplTest {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(RegisteredStresserImplTest.class);

	@Test()
	public void testRecursiveset() {
		LocalEntryPoint ep = new LocalEntryPoint();
		LocalRegisteredVM vm = ep.addVM(new EmptyStresser());
		RegisteredStresserImpl test = vm.getStresser("cpu");
		Assert.assertNotNull(test);
		Closure<?> cl = GroovyTooling.makeClosure("it.load=it.load;");
		test.onLoadChange(cl);
		test.setLoad(0.0);
	}

	@Test()
	public void testNullReturn() {
		LocalEntryPoint ep = new LocalEntryPoint();
		LocalRegisteredVM vm = ep.addVM(new EmptyStresser() {
			@Override
			public String get(String key) {
				return null;
			}
		}, null, null);
		BasicVMRegistar reg = new BasicVMRegistar();
		ep.setVMListener(reg);
		reg.need(1);
		RegisteredStresserImpl test = vm.getStresser("cpu");
		vm.getExporter().getStresser(TYPES.CPU.toString()).setLoad(0);
		Assert.assertEquals(test.getLoad(), 0.0);
		((BurstStress) vm.getExporter().getStresser(TYPES.CPU.toString()))
				.addWork(50);
		Assert.assertEquals(test.getWork(), 0.0);
	}
}
