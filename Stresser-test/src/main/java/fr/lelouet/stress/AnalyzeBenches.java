package fr.lelouet.stress;

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

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import fr.lelouet.stress.StressBenchResult.BenchEntry;

/**
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 * 
 */
public class AnalyzeBenches {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(AnalyzeBenches.class);

	public static class AnalyzeResult {

		StressBenchResult res;

		public double load100 = -1;

		public double overHead = 0;

		public int threads = 1;

		public long loopMS = 10;

		/** linear solving of y, with y=f(x), knowing y1=f(x1) and y2=f(x2) */
		public static double linearizeSolve(double x1, double y1, double x2,
				double y2, double x) {
			if (x == x1) {
				return y1;
			}
			if (x == x2) {
				return y2;
			}
			if (x1 == x2) {
				return y1 == y2 ? y1 : Double.NaN;
			}
			return y1 - (y2 - y1) * (x1 - x) / (x2 - x1);
		}

		public AnalyzeResult(StressBenchResult res) {
			this.res = res;
			loopMS = Long.parseLong(res.getProperty("loopms"));
			threads = Integer.parseInt(res.getProperty("threads", "1"));
			// entry with max cpupct <100 and min cpupct >100
			BenchEntry before100 = null, after100 = null;
			// entry with min CPUpct and 2nd entry with min CPUpct
			BenchEntry minCPU = null, minCPU2 = null;
			for (BenchEntry e : res.entries) {
				if (before100 == null || e.cpupct > before100.cpupct
						&& after100.cpupct < 100) {
					before100 = after100;
					after100 = e;
				}
				if (minCPU == null || minCPU.cpupct > e.cpupct) {
					minCPU2 = minCPU;
					minCPU = e;
				} else if (minCPU2 == null) {
					minCPU2 = e;
				}
			}
			if (before100 != null) {
				load100 = linearizeSolve(before100.cpupct, before100.load,
						after100.cpupct, after100.load, 100);
			}
			if (minCPU != null) {
				if (minCPU2.cpupct != minCPU.cpupct) {
					overHead = linearizeSolve(minCPU.load, minCPU.cpupct,
							minCPU2.load, minCPU2.cpupct, 0);
					// System.err.println("over head with data : " + minCPU.load
					// + " "
					// + minCPU.cpupct + " " + minCPU2.load + " " +
					// minCPU2.cpupct
					// + " is " + overHead);
				} else {
					overHead = (minCPU.cpupct + minCPU2.cpupct) / 2;
					System.err.println("same cpupct for minCPU and minCPU2");
				}
			} else {
				System.err.println("no data for overhead");
			}
		}
	}

	HashMap<Integer, ArrayList<AnalyzeResult>> resultsPerThreads = new HashMap<Integer, ArrayList<AnalyzeResult>>();
	HashMap<Long, ArrayList<AnalyzeResult>> resultsPerLoopMS = new HashMap<Long, ArrayList<AnalyzeResult>>();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new AnalyzeBenches().load(args[0]).analyze()
		// .writeGraphs(args[1])
		;
	}

	ArrayList<AnalyzeResult> results = new ArrayList<AnalyzeResult>();

	public AnalyzeBenches load(String dirName) {
		File d = new File(dirName);
		for (File f : d.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".csv");
			}
		})) {
			StressBenchResult res = new StressBenchResult();
			res.load(f.getAbsolutePath());
			add(res);
		}
		return this;
	}

	protected void add(StressBenchResult res) {
		AnalyzeResult an = new AnalyzeResult(res);
		results.add(an);
		ArrayList<AnalyzeResult> l = resultsPerLoopMS.get(an.loopMS);
		if (l == null) {
			l = new ArrayList<AnalyzeBenches.AnalyzeResult>();
			resultsPerLoopMS.put(an.loopMS, l);
		}
		l.add(an);
		l = resultsPerThreads.get(an.threads);
		if (l == null) {
			l = new ArrayList<AnalyzeBenches.AnalyzeResult>();
			resultsPerThreads.put(an.threads, l);
		}
		l.add(an);
	}

	/**
	 * list of data to write as graph. title->dimension->values ; first
	 * dimension is used as X axis.
	 */
	protected LinkedHashMap<String, LinkedHashMap<String, Double[]>> analyzedData = null;

	public AnalyzeBenches analyze() {
		analyzedData = new LinkedHashMap<String, LinkedHashMap<String, Double[]>>();
		analyzeLoad100();
		analyzeOverHead();
		return this;
	}

	/**
	 *
	 */
	protected void analyzeLoad100() {
		LinkedHashMap<String, Double[]> vals = new LinkedHashMap<String, Double[]>();
		ArrayList<Long> loopMS = new ArrayList<Long>(resultsPerLoopMS.keySet());
		Collections.sort(loopMS);
		Double[] loopmsa = new Double[loopMS.size()];
		for (int i = 0; i < loopMS.size(); i++) {
			loopmsa[i] = (double) loopMS.get(i);
		}
		vals.put("loopms", loopmsa);
		for (Entry<Integer, ArrayList<AnalyzeResult>> e : resultsPerThreads
				.entrySet()) {
			int threads = e.getKey();
			Double[] valsForThisThreads = new Double[loopMS.size()];
			for (int i = 0; i < loopMS.size(); i++) {
				long loopms = loopMS.get(i);
				valsForThisThreads[i] = -1.0;
				for (AnalyzeResult ar : e.getValue()) {
					if (ar.loopMS == loopms) {
						valsForThisThreads[i] = ar.load100;
						break;
					}
				}
			}
			vals.put("" + threads + "threads", valsForThisThreads);
		}
		analyzedData.put("load100PerThread-LoopMS", vals);
	}

	protected void analyzeOverHead() {
		LinkedHashMap<String, Double[]> vals = new LinkedHashMap<String, Double[]>();
		ArrayList<Long> loopMS = new ArrayList<Long>(resultsPerLoopMS.keySet());
		Collections.sort(loopMS);
		Double[] loopmsa = new Double[loopMS.size()];
		for (int i = 0; i < loopMS.size(); i++) {
			loopmsa[i] = (double) loopMS.get(i);
		}
		vals.put("loopms", loopmsa);
		for (Entry<Integer, ArrayList<AnalyzeResult>> e : resultsPerThreads
				.entrySet()) {
			int threads = e.getKey();
			Double[] valsForThisThreads = new Double[loopMS.size()];
			for (int i = 0; i < loopMS.size(); i++) {
				long loopms = loopMS.get(i);
				valsForThisThreads[i] = -1.0;
				for (AnalyzeResult ar : e.getValue()) {
					if (ar.loopMS == loopms) {
						// System.err.println("overhead for " + threads +
						// " threads and "
						// + loopms + " loopms is " + ar.overHead);
						valsForThisThreads[i] = ar.overHead;
						break;
					}
				}
			}
			vals.put("" + threads + "threads", valsForThisThreads);
		}
		analyzedData.put("CPUPCT0PerThread-LoopMS", vals);
	}

	// public void writeGraphs(String dirName) {
	// for (Entry<String, LinkedHashMap<String, Double[]>> e : analyzedData
	// .entrySet()) {
	// String title = e.getKey();
	// LinkedHashMap<String, Double[]> dimensions = e.getValue();
	// JavaPlot p = new JavaPlot();
	// p.setTitle(title);
	// PlotStyle style = new PlotStyle();
	// style.setStyle(Style.LINESPOINTS);
	// style.setStyle(Style.LINESPOINTS);
	// Double[] xVals = null;
	// for (Entry<String, Double[]> dim : dimensions.entrySet()) {
	// String name = dim.getKey();
	// Double[] vals = dim.getValue();
	// if (xVals == null) {
	// xVals = vals;
	// p.set("xlabel", "'" + name + "'");
	// } else {
	// PointDataSet<Double> pds = new PointDataSet<Double>();
	// for (int i = 0; i < xVals.length && i < vals.length; i++) {
	// pds.addPoint(xVals[i], vals[i]);
	// }
	// DataSetPlot dsp = new DataSetPlot(pds);
	// dsp.setTitle(name);
	// dsp.setPlotStyle(style);
	// p.addPlot(dsp);
	// }
	// }
	// ImageTerminal term = new ImageTerminal();
	// p.setTerminal(term);
	// p.plot();
	// try {
	// ImageIO.write(term.getImage(), "png", new File(dirName, title
	// + ".png"));
	// logger.info("wrote graph " + title);
	// } catch (IOException ex) {
	// System.err.print(ex);
	// }
	// }
	// }
}
