package fr.lelouet.stresscloud.export.tcp;

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

import fr.lelouet.stress.cpu.CPUMIPSStress;
import fr.lelouet.stress.cpu.UnsafeRamWriterStress;
import fr.lelouet.stress.net.UDPSenderStress;
import fr.lelouet.stresscloud.control.RegisteredVM;
import fr.lelouet.stresscloud.export.BasicVMListener;

/**
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 * 
 */
public class TCPlaunch {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(TCPlaunch.class);

	/**
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException {
		int port = 9090;
		TCPEntryPoint test = new TCPEntryPoint(port);
		BasicVMListener bl = new BasicVMListener();
		test.setVMListener(bl);
		final TCPExporter exp = new TCPExporter("127.0.0.1", port);
		final CPUMIPSStress cpu = new CPUMIPSStress();
		final UnsafeRamWriterStress ram = new UnsafeRamWriterStress();
		final UDPSenderStress net = new UDPSenderStress();
		new Thread(new Runnable() {
			@Override
			public void run() {
				logger.debug("exporting vm");
				exp.exportVM(cpu, ram, net);
			}
		}).start();
		bl.need(1);
		logger.debug("got 1 vm : " + test.getRegisteredVMsList());
		Thread.sleep(1000);
		logger.debug("vm registered");
		RegisteredVM vm = test.getRegisteredVMsList().get(0);
		vm.getStresser("cpu").add(5000 * 10);
		Thread.sleep(20000);
		// for (long load = 8; load < 10000; load *= 2) {
		// logger.debug("setting load to " + load);
		// vm.setCpu(load);
		// Thread.sleep(10000);
		// }
		logger.info("end of test");
		System.exit(0);
	}
}
