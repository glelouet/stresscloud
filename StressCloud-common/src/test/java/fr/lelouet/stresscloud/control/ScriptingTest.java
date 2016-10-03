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

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import fr.lelouet.stresscloud.control.BasicVMRegistar;
import fr.lelouet.stresscloud.local.LocalEntryPoint;
import fr.lelouet.stresscloud.local.LocalRegisteredVM;

/**
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 * 
 */
public class ScriptingTest {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(ScriptingTest.class);

	@Test
	public void hpc() {
		LocalEntryPoint lep = new LocalEntryPoint();
		int nbVMs = 3;
		LocalRegisteredVM[] vms = new LocalRegisteredVM[nbVMs];
		for (int i = 0; i < nbVMs; i++) {
			vms[i] = lep.addVM(null, 1, -1);
		}
		BasicVMRegistar registar = new BasicVMRegistar();
		registar.setRequireTimeout(200);
		lep.setVMListener(registar);
		String script1 = "require(\"cores==2\", 5)";
		Object ret1 = registar.evaluate(script1);
		Assert.assertEquals(null, ret1);
		// System.err.println("script1 passed");
		String script2 = "need(" + nbVMs
				+ ");stresses = require(\"cores==1\", " + nbVMs + ");";
		List<?> ret2 = (List<?>) registar.evaluate(script2);
		// System.err.println("script2 passed");
		Assert.assertEquals(nbVMs, ret2.size());
		String script3 = "stresses.each({it.cpu=10});stresses";
		List<?> ret3 = (List<?>) registar.evaluate(script3);
		// System.err.println("script3 passed");
		Assert.assertEquals(ret2, ret3);
		// for (RegisteredVM vm : vms) {
		// Assert.assertEquals(vm.getAt(TYPES.CPU).getLoad(), 10.0);
		// }
		// String script4 = "stresses.each({it.cpu+100});sync(stresses)";
		// registar.evaluate(script4);
		// System.err.println("script4 passed");

		// for (LocalRegisteredVM vm : vms) {
		// Assert.assertEquals(vm.getAt(TYPES.CPU).getLoad(), 0.0, "on vm "
		// + vm);
		// }
	}
}
