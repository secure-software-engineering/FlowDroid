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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletRegistration.Dynamic;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.descriptor.JspConfigDescriptor;

public class DummyServletContext implements ServletContext {

	@Override
	public Object getAttribute(String arg0) {
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getAttributeNames() {
		return null;
	}

	@Override
	public ServletContext getContext(String arg0) {
		return this;
	}

	@Override
	public String getInitParameter(String arg0) {
		return arg0;
	}

	@Override
	public String getContextPath() {
		return null;
	}

	@Override
	public int getEffectiveMajorVersion() {
		return 0;
	}

	@Override
	public int getEffectiveMinorVersion() {
		return 0;
	}

	@Override
	public boolean setInitParameter(String s, String s1) {
		return false;
	}

	@Override
	public ServletRegistration.Dynamic addServlet(String s, String s1) {
		return null;
	}

	@Override
	public ServletRegistration.Dynamic addServlet(String s, Servlet servlet) {
		return null;
	}

	@Override
	public ServletRegistration.Dynamic addServlet(String s, Class<? extends Servlet> aClass) {
		return null;
	}

	@Override
	public <T extends Servlet> T createServlet(Class<T> aClass) throws ServletException {
		return null;
	}

	@Override
	public ServletRegistration getServletRegistration(String s) {
		return null;
	}

	@Override
	public Map<String, ? extends ServletRegistration> getServletRegistrations() {
		return null;
	}

	@Override
	public FilterRegistration.Dynamic addFilter(String s, String s1) {
		return null;
	}

	@Override
	public FilterRegistration.Dynamic addFilter(String s, Filter filter) {
		return null;
	}

	@Override
	public FilterRegistration.Dynamic addFilter(String s, Class<? extends Filter> aClass) {
		return null;
	}

	@Override
	public <T extends Filter> T createFilter(Class<T> aClass) throws ServletException {
		return null;
	}

	@Override
	public FilterRegistration getFilterRegistration(String s) {
		return null;
	}

	@Override
	public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
		return null;
	}

	@Override
	public SessionCookieConfig getSessionCookieConfig() {
		return null;
	}

	@Override
	public void setSessionTrackingModes(Set<SessionTrackingMode> set) {

	}

	@Override
	public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
		return null;
	}

	@Override
	public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
		return null;
	}

	@Override
	public void addListener(String s) {

	}

	@Override
	public <T extends EventListener> void addListener(T t) {

	}

	@Override
	public void addListener(Class<? extends EventListener> aClass) {

	}

	@Override
	public <T extends EventListener> T createListener(Class<T> aClass) throws ServletException {
		return null;
	}

	@Override
	public JspConfigDescriptor getJspConfigDescriptor() {
		return null;
	}

	@Override
	public ClassLoader getClassLoader() {
		return null;
	}

	@Override
	public void declareRoles(String... strings) {

	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getInitParameterNames() {
		return new StringTokenizer("one two three");
	}

	@Override
	public int getMajorVersion() {
		return 0;
	}

	@Override
	public String getMimeType(String arg0) {
		return null;
	}

	@Override
	public int getMinorVersion() {
		return 0;
	}

	@Override
	public RequestDispatcher getNamedDispatcher(String arg0) {
		return null;
	}

	@Override
	public String getRealPath(String arg0) {
		return null;
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String arg0) {
		return null;
	}

	@Override
	public URL getResource(String arg0) throws MalformedURLException {
		return null;
	}

	@Override
	public InputStream getResourceAsStream(String arg0) {
		return new DummyServletInputStream();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Set getResourcePaths(String arg0) {
		return null;
	}

	@Override
	public String getServerInfo() {
		return null;
	}

	@Override
	public String getServletContextName() {
		return null;
	}

	@Override
	public void log(String arg0) {
	}

	@Override
	public void log(String arg0, Throwable arg1) {
	}

	@Override
	public void removeAttribute(String arg0) {
	}

	@Override
	public void setAttribute(String arg0, Object arg1) {
	}

	@Override
	public String getVirtualServerName() {
		return "default-server";
	}

	@Override
	public Dynamic addJspFile(String servletName, String jspFile) {
		return null;
	}

	@Override
	public int getSessionTimeout() {
		return 0;
	}

	@Override
	public void setSessionTimeout(int sessionTimeout) {
	}

	@Override
	public String getRequestCharacterEncoding() {
		return "UTF-8";
	}

	@Override
	public void setRequestCharacterEncoding(String encoding) {
	}

	@Override
	public String getResponseCharacterEncoding() {
		return "UTF-8";
	}

	@Override
	public void setResponseCharacterEncoding(String encoding) {
	}

}
