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
public class GetTest {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(GetTest.class);

	@Test
	public void testGetParsing() {
		Get get = new Get();
		get.id = 25l;
		get.key = "test";
		get.type = TYPES.DISK.toString();
		parseAndUnparse(get, "25=GET disk.test");
		get.id = 32l;
		get.key = "LoadUnit";
		get.type = "type1";
		parseAndUnparse(get, "32=GET type1.LoadUnit");
	}

	public void parseAndUnparse(Get get, String val) {
		String str = get.toString();
		if (val != null) {
			Assert.assertEquals(str, val);
		}
		Get parsed = Get.parseGet(str);
		Assert.assertEquals(parsed.id, get.id);
		Assert.assertEquals(parsed.key, get.key);
		Assert.assertEquals(parsed.type, get.type);
	}
}
