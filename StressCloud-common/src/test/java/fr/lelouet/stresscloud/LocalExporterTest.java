package fr.lelouet.stresscloud;

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

import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import fr.lelouet.stresscloud.control.BasicVMRegistar;
import fr.lelouet.stresscloud.local.EmptyStresser;
import fr.lelouet.stresscloud.local.LocalEntryPoint;

/**
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 * 
 */
public class LocalExporterTest {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(LocalExporterTest.class);

	@Test
	public void testExportDifferentTypes() {
		LocalEntryPoint ep = new LocalEntryPoint();
		BasicVMRegistar reg = new BasicVMRegistar();
		ep.setVMListener(reg);
		ep.addVM(
				new EmptyStresser().constructType("type1").constructLoadUnit(
						"unit1"), new EmptyStresser().constructType("type2")
						.constructLoadUnit("unit2"));
		reg.need(1);
		Assert.assertEquals(
				reg.evaluate("availableVMs.first().type1.loadunit"), "unit1");
		@SuppressWarnings("unchecked")
		Set<String> types = (Set<String>) reg
				.evaluate("availableVMs.first().types");
		Assert.assertTrue(types.contains("type1"), "" + types);
		Assert.assertTrue(types.contains("type2"), "" + types);
	}
}
