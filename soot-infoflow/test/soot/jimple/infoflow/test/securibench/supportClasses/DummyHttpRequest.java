/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow.test.securibench.supportClasses;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;

public class DummyHttpRequest implements HttpServletRequest {
	enum count {
		ONE, TWO
	};

	@Override
	public Object getAttribute(String arg0) {
		return "";
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Enumeration getAttributeNames() {
		return new StringTokenizer("one two");
	}

	@Override
	public String getCharacterEncoding() {
		return "UTF-8";
	}

	@Override
	public int getContentLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getContentType() {
		return "contenttype";
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		return new DummyServletInputStream();
	}

	@Override
	public Locale getLocale() {
		return Locale.ENGLISH;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Enumeration getLocales() {
		return null;
	}

	@Override
	public String getParameter(String arg0) {
		return arg0;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Map getParameterMap() {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("dummy", "dummy");
		return map;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Enumeration getParameterNames() {
		return new StringTokenizer("parameter names");
	}

	@Override
	public String[] getParameterValues(String arg0) {
		return new String[] { arg0 };
	}

	@Override
	public String getProtocol() {
		return "http";
	}

	@Override
	public BufferedReader getReader() throws IOException {
		return null;
	}

	@Override
	public String getRemoteAddr() {
		return "127.0.0.1";
	}

	@Override
	public String getRemoteHost() {
		return "localhost";
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getScheme() {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public String getServerName() {
		return "localhost";
	}

	@Override
	public int getServerPort() {
		return 80;
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public void removeAttribute(String arg0) {
	}

	@Override
	public void setAttribute(String arg0, Object arg1) {
	}

	@Override
	public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {
	}

	@Override
	public String getAuthType() {
		return "";
	}

	@Override
	public String getContextPath() {
		return "";
	}

	@Override
	public Cookie[] getCookies() {
		Cookie c = new Cookie("", "");
		return new Cookie[] { c };
	}

	@Override
	public long getDateHeader(String arg0) {
		return 0;
	}

	@Override
	public String getHeader(String arg0) {
		return arg0;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Enumeration getHeaderNames() {
		return new StringTokenizer("secret1 secret2 secret3");
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Enumeration getHeaders(String arg0) {
		return new StringTokenizer("secret1 secret2 secret3");
	}

	@Override
	public int getIntHeader(String arg0) {
		return 0;
	}

	@Override
	public String getMethod() {
		return "GET";
	}

	@Override
	public String getPathInfo() {
		return "/stuff";
	}

	@Override
	public String getPathTranslated() {
		return null;
	}

	@Override
	public String getQueryString() {
		return "";
	}

	@Override
	public String getRemoteUser() {
		return "";
	}

	@Override
	public String getRequestURI() {
		return "";
	}

	@Override
	public StringBuffer getRequestURL() {
		return new StringBuffer("http://");
	}

	@Override
	public String getRequestedSessionId() {
		return null;
	}

	@Override
	public String getServletPath() {
		return null;
	}

	@Override
	public Principal getUserPrincipal() {
		return null;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		return false;
	}

	@Override
	public String getLocalAddr() {
		return null;
	}

	@Override
	public String getLocalName() {
		return null;
	}

	@Override
	public int getLocalPort() {
		return 0;
	}

	@Override
	public int getRemotePort() {
		return 80;
	}

	@Override
	public AsyncContext getAsyncContext() {
		return null;
	}

	@Override
	public long getContentLengthLong() {
		return 0;
	}

	@Override
	public DispatcherType getDispatcherType() {
		return null;
	}

	@Override
	public String getProtocolRequestId() {
		return null;
	}

	@Override
	public String getRequestId() {
		return null;
	}

	@Override
	public ServletConnection getServletConnection() {
		return null;
	}

	@Override
	public ServletContext getServletContext() {
		return null;
	}

	@Override
	public boolean isAsyncStarted() {
		return false;
	}

	@Override
	public boolean isAsyncSupported() {
		return false;
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		return null;
	}

	@Override
	public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) throws IllegalStateException {
		return null;
	}

	@Override
	public boolean authenticate(HttpServletResponse arg0) throws IOException, ServletException {
		return false;
	}

	@Override
	public String changeSessionId() {
		return null;
	}

	@Override
	public Part getPart(String arg0) throws IOException, ServletException {
		return null;
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		return null;
	}

	@Override
	public void login(String arg0, String arg1) throws ServletException {
	}

	@Override
	public void logout() throws ServletException {
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> arg0) throws IOException, ServletException {
		return null;
	}

	@Override
	public HttpSession getSession() {
		return new DummyHttpSession();
	}

	@Override
	public HttpSession getSession(boolean arg0) {
		return new DummyHttpSession();
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		return false;
	}

	@Override
	public boolean isUserInRole(String arg0) {
		return false;
	}

}
