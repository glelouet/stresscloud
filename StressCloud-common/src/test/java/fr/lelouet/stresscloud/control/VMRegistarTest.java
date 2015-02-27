package fr.lelouet.stresscloud.control;

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

import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;

import org.testng.Assert;
import org.testng.annotations.Test;

import fr.lelouet.stresscloud.ControlledStresser;
import fr.lelouet.stresscloud.Stresser;
import fr.lelouet.stresscloud.local.EmptyStresser;
import fr.lelouet.stresscloud.local.LocalEntryPoint;
import fr.lelouet.stresscloud.local.LocalRegisteredVM;
import groovy.lang.Closure;

/**
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 * 
 */
public class VMRegistarTest {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(VMRegistarTest.class);

	@Test
	public void testVMAdd() {
		LocalEntryPoint lep = new LocalEntryPoint();
		lep.addVM("ip1", 1, 12);
		lep.addVM("ip2", 1, 13);
		lep.addVM("ip3", 2, 13);
		lep.addVM("ip3", 2, 13);
		BasicVMRegistar test = new BasicVMRegistar();
		lep.setVMListener(test);
		test.need(4);
		List<RegisteredVM> res = test.require((String) null, 4);
		Assert.assertNotNull(res);
		Assert.assertEquals(res.size(), 4);
		Assert.assertEquals(res.get(0).getIp(), "ip1");
		Assert.assertEquals(res.get(1).getIp(), "ip2");
		Assert.assertEquals(res.get(2).getIp(), "ip3");
		Assert.assertEquals(res.get(3).getIp(), "ip3");
		Assert.assertEquals(res.get(3).getId(), 4);

	}

	@Test
	public void simpleFiltering() {
		BasicVMRegistar test = new BasicVMRegistar();
		LocalEntryPoint lep = new LocalEntryPoint();
		lep.addVM("ip1", 4, 1024);
		lep.addVM("ip2", 4, 10243);
		lep.addVM("ip3", 4, 1024);
		lep.addVM("ip4", 1, 2048);
		lep.addVM("ip5", 10, 1024);
		lep.addVM("ip6", 10, 1024);
		lep.addVM("ip7", 10, 1024);
		lep.addVM("ip8", 10, 1024);
		lep.setVMListener(test);
		test.need(8);
		Closure<?> filter1 = GroovyTooling.makeClosure("mem==2048");
		RegisteredVM res1 = test.require(filter1);
		Assert.assertEquals(res1.getIp(), "ip4");
		RegisteredVM res1b = test.require(filter1);
		Assert.assertEquals(res1b, null);
		Closure<?> filter2 = GroovyTooling.makeClosure("cores==4");
		List<RegisteredVM> res2 = test.require(filter2, 3);
		Assert.assertEquals(res2.size(), 3);
		List<RegisteredVM> res2b = test.require(filter2, 1);
		Assert.assertEquals(res2b, null);
		Assert.assertEquals(test.getAvailableVMs().size(), 4);
		Assert.assertEquals(test.getReservedVMs().size(), 4);
	}

	@Test
	public void releaseSleepTest() {
		BasicVMRegistar test = new BasicVMRegistar();
		LocalEntryPoint ep = new LocalEntryPoint();
		ep.setVMListener(test);
		Stresser s = new EmptyStresser().withType("net");
		ep.addVM(new EmptyStresser(), new EmptyStresser().withType("ram"), s);
		test.need(1);
		RegisteredVM vm = test.require((String) null);
		Assert.assertNotNull(vm);
		vm.getStresser("net").add(10000000000000.0);
		vm.after();
		test.release();
		// TODO correct test here, not dependant of sync.
		// Assert.assertEquals(s.get(Stresser.WORK_KEY), "0.0");
	}

	/** test the result of a simple condition with timeout modifications */
	@Test
	public void testTimeout() {
		BasicVMRegistar test = new BasicVMRegistar();
		final LocalEntryPoint ep = new LocalEntryPoint();
		ep.setVMListener(test);
		final ControlledStresser s = new ControlledStresser();
		ep.addVM(s, s, s);
		Assert.assertEquals(test.require((String) null, 2), null);
		test.setRequireTimeout(5000);
		final Semaphore lock = new Semaphore(0);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					lock.acquire();
				} catch (InterruptedException e) {
					throw new UnsupportedOperationException(e);
				}
				ep.addVM(s, s, s);
			}
		}).start();
		lock.release();
		Assert.assertEquals(test.require((String) null, 2).size(), 2);
	}

	@Test
	public void testVMComparator() {
		TreeSet<RegisteredVM> test = new TreeSet<RegisteredVM>(
				BasicVMRegistar.VMCMP);
		int nb = 32;
		LocalRegisteredVM[] vms = new LocalRegisteredVM[nb];
		for (int i = 0; i < nb; i++) {
			int j = nb - 1 - i;
			LocalRegisteredVM vm = new LocalRegisteredVM();
			vms[j] = vm;
			vm.setIp("0.0.0." + j);
			vm.id = j;
			vm.setCores(1);
			vm.setMem(1000);
			test.add(vm);
		}
		Assert.assertEquals(test.first(), vms[0]);
		test.remove(vms[0]);
		Assert.assertEquals(test.first(), vms[1]);
		test.remove(vms[1]);
		Assert.assertEquals(test.first(), vms[10], "list is : " + test);
	}

	@Test
	public void testVMOrdering() {
		LocalEntryPoint local = new LocalEntryPoint();
		BasicVMRegistar reg = new BasicVMRegistar();
		local.setVMListener(reg);
		// LocalRegisteredVM vm1 = local.addVM(new ControlledStresser(),
		// new ControlledStresser(), new ControlledStresser());
		// LocalRegisteredVM vm2 = local.addVM(new ControlledStresser(),
		// new ControlledStresser(), new ControlledStresser());
		// LocalRegisteredVM vm3 = local.addVM(new ControlledStresser(),
		// new ControlledStresser(), new ControlledStresser());
		// LocalRegisteredVM vm4 = local.addVM(new ControlledStresser(),
		// new ControlledStresser(), new ControlledStresser());
		// vm1.cpu.add(1);
		// TODO
	}
}
