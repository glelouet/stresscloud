package fr.lelouet.stresscloud.control;

/*
 * #%L
 * StressCloud-common
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

import java.util.Arrays;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author guillaume
 *
 */
public class LoadBaseTest {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
	.getLogger(LoadBaseTest.class);

	@Test
	public void testApply() {
		LoadBase test = new LoadBase(5, 150);
		RegisteredStresser stresser = Mockito.mock(RegisteredStresser.class);
		RegisteredVM parent = Mockito.mock(RegisteredVM.class);
		Mockito.when(stresser.getParent()).thenReturn(parent);
		test.to(stresser, 1, 0.5, 10, 5.5);

		ArgumentCaptor<Double> loadsCapturer = ArgumentCaptor
				.forClass(Double.class);
		Mockito.verify(stresser, Mockito.times(5)).setLoad(
				loadsCapturer.capture());
		Assert.assertEquals(Arrays.asList(150.0, 75.0, 1500.0, 5.5 * 150, 0.0),
				loadsCapturer.getAllValues());

		ArgumentCaptor<Long> afterCapturer = ArgumentCaptor
				.forClass(Long.class);
		Mockito.verify(parent, Mockito.times(4)).after(afterCapturer.capture());
		Assert.assertEquals(Arrays.asList(5l, 5l, 5l, 5l),
				afterCapturer.getAllValues());
	}

}
