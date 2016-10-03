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

import fr.lelouet.stresscloud.commands.Ping;

/**
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 * 
 */
public class PingTest {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(PingTest.class);

	@Test
	public void testParsing() {
		Ping test = new Ping();
		test.id = 25l;
		test.num = 55;
		String str = test.toString();
		Assert.assertEquals(str, "25=PNG 55");
		Ping parsed = Ping.parsePing(str);
		Assert.assertEquals(parsed.id, test.id);
		Assert.assertEquals(parsed.num, test.num);
		Assert.assertEquals(parsed.type, test.type);
	}
}
