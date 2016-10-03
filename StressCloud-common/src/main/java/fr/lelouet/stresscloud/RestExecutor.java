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

import java.io.StringReader;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Provider;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.http.HTTPBinding;

import fr.lelouet.stresscloud.control.BasicVMRegistar;

/**
 * provide an access to the executor over REST presentation.
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
@javax.xml.ws.WebServiceProvider
@javax.xml.ws.ServiceMode(value = javax.xml.ws.Service.Mode.PAYLOAD)
public class RestExecutor implements Provider<Source> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(RestExecutor.class);

	@javax.annotation.Resource(type = Object.class)
	protected WebServiceContext wsContext;

	@Override
	public Source invoke(Source request) {
		MessageContext mc = wsContext.getMessageContext();
		return execute(mc);
	}

	/**
	 * @param mc
	 * @return
	 */
	private Source execute(MessageContext mc) {
		String script = (String) mc.get(MessageContext.QUERY_STRING);
		script = unparseHTML(script);
		logger.debug("executing script : " + script);
		String res = "" + registar.evaluate(script);
		logger.debug("script <{}> resulted in <{}>", new Object[]{script, res});
		return new StreamSource(
				new StringReader("<result>" + res + "</result>"));
	}

	public static String unparseHTML(String html) {
		if (html == null) {
			return null;
		}
		return HttpExecutor.unparseHTML(html).replaceAll("<<", "{")
				.replaceAll(">>", "}");
	}

	private BasicVMRegistar registar = new BasicVMRegistar();

	/** @return the registar */
	public BasicVMRegistar getRegistar() {
		return registar;
	}

	/**
	 * @param registar
	 *            the registar to set
	 */
	public void setRegistar(BasicVMRegistar registar) {
		this.registar = registar;
	}

	public RestExecutor() {
	}

	protected Endpoint e;

	public int port = 8090;

	public void publish() {
		e = Endpoint.create(HTTPBinding.HTTP_BINDING, this);
		e.publish("http://0.0.0.0:" + port + "/execute");
		logger.info("executing request on http://<ip>:" + port + "/execute");
	}

	public void unpublish() {
		e.stop();
	}
}
