package fr.lelouet.stresscloud;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import fr.lelouet.stresscloud.Stresser.StopHook;

/**
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 * 
 */
public class ControlledStresserTest {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(ControlledStresserTest.class);

	@Test
	public void testSimpleAccess() throws Exception {
		final ControlledStresser test = new ControlledStresser();

		// test running state
		Assert.assertEquals(test.keepsRunning(), false);
		test.start();
		Assert.assertEquals(test.keepsRunning(), true, " acces traces is : "
				+ test.accessesTraces);

		// test setLoad and addWork
		test.setLoad(50);
		Assert.assertEquals(test.getLoad(), 50.0);
		test.addWork(10);
		Assert.assertEquals(test.getLoad(), 0.0);
		Assert.assertEquals(test.getWork(), 10.0);

		// test after
		final Semaphore bloc = new Semaphore(0);
		new Thread(new Runnable() {

			@Override
			public void run() {
				bloc.release();
				test.after();
				bloc.release();
			}
		}).start();
		Assert.assertTrue(bloc.tryAcquire(200, TimeUnit.MILLISECONDS));
		Assert.assertEquals(bloc.availablePermits(), 0);
		test.clearWork();
		Assert.assertTrue(bloc.tryAcquire(200, TimeUnit.MILLISECONDS));
		Assert.assertEquals(test.getWork(), 0.0);

		// test setOnExit
		StopHook sh = Mockito.mock(StopHook.class);
		test.addOnExit(sh);
		test.stop();
		Assert.assertEquals(test.keepsRunning(), false);
		Mockito.verify(sh).onExit(test);

		// test traces
		List<String> required = new ArrayList<String>(Arrays.asList(
				"get started", "set started = true", "set started = false",
				"get load", "get work", "set load = 0.0", "set load = 50.0"));
		Assert.assertTrue(
				test.accessesTraces.containsAll(required),
				"traces not present : "
						+ (required.removeAll(test.accessesTraces) ? "" : "")
						+ required);
	}
}
