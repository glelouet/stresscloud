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

import fr.lelouet.stress.cpu.CPUMIPSStress;
import fr.lelouet.stress.cpu.UnsafeRamWriterStress;
import fr.lelouet.stress.net.UDPSenderStress;
import fr.lelouet.stresscloud.BurstStress;

/**
 * contains several stressers to stress at the same time.<br />
 * The load can be in percentage of relative for ram and CPU stressers.
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public class MultipleStresser implements BurstStress.MaxLoadListener {

	private static org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(MultipleStresser.class);

	public MultipleStresser() {
		setCpuStress(new CPUMIPSStress());
		setdiskStress(new UnsafeRamWriterStress());
		setNetStress(new UDPSenderStress());
	}

	BurstStress cpuStress;

	public BurstStress getCpuStress() {
		return cpuStress;
	}

	public void setCpuStress(BurstStress cpuStress) {
		this.cpuStress = cpuStress;
		cpuStress.registerMaxLoadListener(this);
	}

	BurstStress diskStress;

	public BurstStress getRamStress() {
		return diskStress;
	}

	public void setdiskStress(BurstStress diskStress) {
		this.diskStress = diskStress;
		diskStress.registerMaxLoadListener(this);
	}

	BurstStress netStress;

	public BurstStress getNetStress() {
		return netStress;
	}

	public void setNetStress(BurstStress netStress) {
		this.netStress = netStress;
	}

	long loadCPU = 0;

	public long getLoadCPU() {
		return loadCPU;
	}

	public void setLoadCPU(long loadCPU) {
		this.loadCPU = loadCPU;
	}

	long loadRAM = 0;

	public long getLoadRAM() {
		return loadRAM;
	}

	public void setLoadRAM(long loadRAM) {
		this.loadRAM = loadRAM;
	}

	long loadNET = 0;

	public long getLoadNET() {
		return loadNET;
	}

	public void setLoadNET(long loadNET) {
		this.loadNET = loadNET;
	}

	/** should we use the load as a percentage of the max load ? */
	boolean percentLoad = false;

	public void setPercentLoad(boolean percentLoad) {
		this.percentLoad = percentLoad;
	}

	@Override
	public void onNewMaxLoad(BurstStress stress, double maxLoad) {
		applyConfig();
	}

	/**
	 * apply the load to the stressers. If {@link #percentLoad} , set the load
	 * relative to the max Load a stresser has reached. Else, set the net and
	 * ram stressers to given value, and the CPU stress to the wanted value
	 * minus the activity the ram stresser evaluates it produces.
	 */
	void applyConfig() {
		if (percentLoad) {
			applyConfigPercent();
			return;
		}
		netStress.setLoad(loadNET);
		diskStress.setLoad(loadRAM);
		double loadCPU = this.loadCPU - loadRAM
				* cpuStress.getMaxUsagesPerSecond()
				/ diskStress.getMaxUsagesPerSecond();
		if (loadCPU < 0) {
			logger.debug("invalid config : CPU={}/{}, RAM={}/{}", new Object[]{
					this.loadCPU, cpuStress.getMaxUsagesPerSecond(), loadRAM,
					diskStress.getMaxUsagesPerSecond()});
			loadCPU = 0;
		}
		cpuStress.setLoad(loadCPU);
	}

	/**
	 * set the stressers' load with percentages instead of direct load. The net
	 * stress is absolute B/s though.
	 */
	void applyConfigPercent() {
		if (loadNET != netStress.getLoad()) {
			netStress.setLoad(loadNET);
		}
		double ramloadpc = loadRAM * diskStress.getMaxUsagesPerSecond() / 100;
		if (ramloadpc != diskStress.getLoad()) {
			diskStress.setLoad(ramloadpc);
		}
		double cpuloadpc = (loadCPU - loadRAM)
				* cpuStress.getMaxUsagesPerSecond() / 100;
		if (cpuloadpc != cpuStress.getLoad()) {
			cpuStress.setLoad(cpuloadpc);
		}
	}

	/**
	 * start the stressers threads
	 */
	public void launch() {
		applyConfig();
		new Thread(cpuStress).start();
		new Thread(diskStress).start();
		new Thread(netStress).start();
	}

	/** set the granularity of the internal stressers. */
	public void setLoopMS(long loopMS) {
		cpuStress.setLoopMS(loopMS);
		diskStress.setLoopMS(loopMS);
		netStress.setLoopMS(loopMS);
	}

}
