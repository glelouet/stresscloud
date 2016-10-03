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
import fr.lelouet.stress.disk.DiskBitWritter;
import fr.lelouet.stress.net.UDPSenderStress;
import fr.lelouet.stresscloud.HttpExecutor;
import fr.lelouet.stresscloud.RestExecutor;
import fr.lelouet.stresscloud.control.BasicVMRegistar;

/**
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 * 
 */
public class PrepareTCPVMs {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(PrepareTCPVMs.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int num = 2;
		if (args.length > 0) {
			num = Integer.parseInt(args[0]);
		}
		final int port = 9090;
		TCPEntryPoint test = new TCPEntryPoint(port);
		BasicVMRegistar reg = new BasicVMRegistar();
		test.setVMListener(reg);
		Runnable vmStarter = new Runnable() {
			@Override
			public void run() {
				TCPExporter exp = new TCPExporter("127.0.0.1", port);
				CPUMIPSStress cpu = new CPUMIPSStress();
				UDPSenderStress net = new UDPSenderStress();
				DiskBitWritter disk = new DiskBitWritter();
				exp.exportVM(cpu, disk, net);
			}
		};
		for (int i = 0; i < num; i++) {
			new Thread(vmStarter).start();
		}
		RestExecutor ex = new RestExecutor();
		ex.setRegistar(reg);
		ex.publish();
		HttpExecutor h = new HttpExecutor();
		h.setRegistar(reg);
		h.publish();
	}
}
