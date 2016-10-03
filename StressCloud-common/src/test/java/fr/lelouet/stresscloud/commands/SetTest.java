package fr.lelouet.stresscloud.commands;

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

import fr.lelouet.stresscloud.Stresser.TYPES;

/**
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 * 
 */
public class SetTest {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(SetTest.class);

	@Test
	public void testParsing() {
		Set set = new Set();
		set.id = 100l;
		set.key = "test";
		set.type = TYPES.DISK.toString();
		set.value = "42";
		String str = set.toString();
		Assert.assertEquals(str, "100=SET disk.test=42");
		Set parsed = Set.parseSet(str);
		Assert.assertEquals(parsed.id, set.id);
		Assert.assertEquals(parsed.key, set.key);
		Assert.assertEquals(parsed.type, set.type);
		Assert.assertEquals(parsed.value, set.value);
	}
}
