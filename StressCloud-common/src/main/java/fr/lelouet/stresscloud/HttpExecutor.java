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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import fr.lelouet.stresscloud.control.BasicVMRegistar;
import fr.lelouet.stresscloud.control.BasicVMRegistar.ScriptDetail;

/**
 * gives a simple page with a form to send the script
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
@SuppressWarnings("restriction")
public class HttpExecutor implements com.sun.net.httpserver.HttpHandler {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(HttpExecutor.class);

	public HttpExecutor() {
		this(DEFAULT_PORT);
	}

	public HttpExecutor(int port) {
		this.port = port;
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		StringBuilder response = new StringBuilder(
				"<html><head></head><body><table>");
		String ret = "";
		String script = "availableVMs";
		String request = new BufferedReader(new InputStreamReader(
				t.getRequestBody())).readLine();
		if (request != null) {
			for (String s : request.split("&")) {
				if (s.startsWith("script=")) {
					script = unparseHTML(s.substring("script=".length()));
				}
			}
		}
		logger.debug("script from post request: " + script);
		if (registar == null) {
			ret = "<tr><td>error</td><td>no registar</td></tr>\n";
		} else {
			try {
				script = script.replaceAll("\\n[\\n\\r]+", "\n");
				Object res = registar.evaluate(script);
				ret = "<tr><td>result</td><td>" + res == null
						? null
						: escapeHTML("" + res) + "</td></tr>";
			} catch (Exception e) {
				logger.warn("while executing " + script, e);
				ret = "<tr><td>error</td><td>" + e + "</td></tr>\n";
			}
		}
		// System.err.println("returned " + ret);
		response.append(ret);
		response.append("<tr><td>send</td><td><form method=\"post\">\n <textarea name=\"script\" cols=\"100\" rows=\"20\">"
				+ script
				+ "</textarea>\n <input name=\"exec\" type=\"submit\" value=\"submit\"/>\n </form>");
		response.append("</td></tr>\n");
		response.append("<tr><td>scripts</td><td>" + makescripts()
				+ "</td></tr>\n");
		response.append("<tr><td>loads</td><td>"
				+ escapeHTML(registar.loads().toCSV()) + "</td></tr>\n");
		response.append("<tr><td>works</td><td>"
				+ escapeHTML(registar.works().toCSV()) + "</td></tr>\n");
		response.append("</table></body></html>");

		t.sendResponseHeaders(200, response.length());
		OutputStream os = t.getResponseBody();
		os.write(response.toString().getBytes());
		os.close();
	}

	/**
	 * make a table of the scripts running
	 * 
	 * @return
	 */
	private String makescripts() {
		StringBuilder ret = new StringBuilder(
				"<table><tr><th>start</th><th>script</th><th>result</th></tr>\n");
		for (ScriptDetail det : registar.getScriptsResults()) {
			ret.append("<tr><td>").append(det.start).append("</td><td>")
					.append(det.script).append("</td><td>")
					.append(det.finished == -1 ? "" : det.res)
					.append("</td></tr>\n");
		}
		ret.append("</table>");
		return ret.toString();
	}

	public static String escapeHTML(String res) {
		return res == null ? null : res.toString().replaceAll("<", "&lt;")
				.replaceAll(">", "&gt;").replaceAll("\n", "<br />");
	}

	protected HttpServer server;

	public static final int DEFAULT_PORT = 8000;
	protected int port;

	public void publish() {
		try {
			server = HttpServer.create(new InetSocketAddress(port), 0);
		} catch (IOException e) {
			logger.warn("", e);
		}
		server.createContext("/execute.html", this);
		Executor ex;
		ex = Executors.newCachedThreadPool();
		server.setExecutor(ex); // creates a default executor
		server.start();
		logger.info("executing request on http://<ip>:" + port
				+ "/execute.html");
	}

	public void unpublish() {
		server.stop(1);
	}

	BasicVMRegistar registar = null;

	public void setRegistar(BasicVMRegistar reg) {
		registar = reg;
	}

	public static String unparseHTML(String from) {
		if (from == null) {
			return null;
		}
		try {
			return URLDecoder.decode(from, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			logger.warn("cannot decode the required string", e);
		}
		return null;
	}

}
