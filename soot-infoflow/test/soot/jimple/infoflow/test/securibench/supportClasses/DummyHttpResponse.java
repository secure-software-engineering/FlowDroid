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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Locale;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

public class DummyHttpResponse implements HttpServletResponse {

	@Override
	public void flushBuffer() throws IOException {
	}

	@Override
	public java.util.Collection<java.lang.String> getHeaders(String arg0) {
		return Arrays.asList("secret1", "secret2", "secret3");
	}

	@Override
	public String getHeader(String arg0) {
		return arg0;
	}

	@Override
	public int getStatus() {
		return 0;
	}

	@Override
	public int getBufferSize() {
		return 0;
	}

	@Override
	public java.util.Collection<java.lang.String> getHeaderNames() {
		return Arrays.asList("secret1", "secret2", "secret3");
	}

	@Override
	public String getCharacterEncoding() {
		return "UTF-8";
	}

	@Override
	public Locale getLocale() {
		return Locale.ENGLISH;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		return new PrintWriter("123");
	}

	@Override
	public boolean isCommitted() {
		return false;
	}

	@Override
	public void reset() {
	}

	@Override
	public void resetBuffer() {
	}

	@Override
	public void setBufferSize(int arg0) {
	}

	@Override
	public void setContentLength(int arg0) {
	}

	@Override
	public void setContentType(String arg0) {
	}

	@Override
	public void setLocale(Locale arg0) {
	}

	@Override
	public void addCookie(Cookie arg0) {
	}

	@Override
	public void addDateHeader(String arg0, long arg1) {
	}

	@Override
	public void addHeader(String arg0, String arg1) {
	}

	@Override
	public void addIntHeader(String arg0, int arg1) {
	}

	@Override
	public boolean containsHeader(String arg0) {
		return false;
	}

	@Override
	public String encodeRedirectURL(String arg0) {
		return arg0;
	}

	@Override
	public String encodeURL(String arg0) {
		return arg0;
	}

	@Override
	public void sendError(int arg0) throws IOException {
	}

	@Override
	public void sendError(int arg0, String arg1) throws IOException {
	}

	@Override
	public void sendRedirect(String arg0) throws IOException {
	}

	@Override
	public void setDateHeader(String arg0, long arg1) {
	}

	@Override
	public void setHeader(String arg0, String arg1) {
	}

	@Override
	public void setIntHeader(String arg0, int arg1) {
	}

	@Override
	public void setStatus(int arg0) {
	}

	@Override
	public String getContentType() {
		return null;
	}

	@Override
	public void setCharacterEncoding(String arg0) {
	}

	@Override
	public void setContentLengthLong(long len) {
	}

	@Override
	public void sendRedirect(String location, int sc, boolean clearBuffer) throws IOException {
	}

}
