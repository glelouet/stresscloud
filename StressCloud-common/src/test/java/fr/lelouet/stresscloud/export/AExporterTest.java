package fr.lelouet.stresscloud.export;

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

import static fr.lelouet.stresscloud.export.AExporter.getHostIpConnect;
import static fr.lelouet.stresscloud.export.AExporter.getHostIpEnumerate;
import static fr.lelouet.stresscloud.export.AExporter.getHostIpIfconfig;
import static fr.lelouet.stresscloud.export.AExporter.getInstalledMemorySize;
import static fr.lelouet.stresscloud.export.AExporter.getNbCores;
import static fr.lelouet.stresscloud.export.AExporter.makeIP;
import static fr.lelouet.stresscloud.export.AExporter.serializeAns;
import static fr.lelouet.stresscloud.export.AExporter.serializeComm;
import static fr.lelouet.stresscloud.export.AExporter.serializeStore;
import static fr.lelouet.stresscloud.export.AExporter.unserializeAns;
import static fr.lelouet.stresscloud.export.AExporter.unserializeComm;
import static fr.lelouet.stresscloud.export.AExporter.unserializeStore;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 * 
 */
public class AExporterTest {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(AExporterTest.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("cores:" + getNbCores());
		System.out.println("installedRAM:" + getInstalledMemorySize());
		System.out.println("ipconfig:" + getHostIpIfconfig());
		System.out.println("ipgoogle:" + getHostIpConnect("gogole.fr", 80));
		System.out.println("ipenum" + getHostIpEnumerate());
	}

	@Test
	public void testCOMParsing() {
		long id = 24;
		String serial = serializeComm(id);
		long parsed = unserializeComm(serial);
		Assert.assertEquals(parsed, id);
	}

	@Test
	public void testANSParsing() {
		long id = 78;
		String val = "lololol";
		String serial = serializeAns(id, val);
		String[] parsed = unserializeAns(serial);
		Assert.assertEquals(parsed, new String[]{"" + id, val});
	}

	@Test
	public void testSTROParsing() {
		String key = "mkey", value = "mval";
		String serial = serializeStore(key, value);
		String[] parsed = unserializeStore(serial);
		Assert.assertEquals(parsed, new String[]{key, value});
	}

	@Test
	public void testMakeIp() {
		Assert.assertEquals(makeIP(256 * 256 * 256 * 1 + 256 * 256 * 2 + 256
				* 3 + 4), "1.2.3.4");
	}
}
