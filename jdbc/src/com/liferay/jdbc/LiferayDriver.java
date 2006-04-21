/**
 * Copyright (c) 2000-2006 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.jdbc;

import java.io.InputStream;

import java.net.URL;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;

import java.util.Properties;
import java.util.logging.Logger;

/**
 * <a href="LiferayDriver.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 *
 */
public class LiferayDriver implements Driver {

	static {
		try {
			DriverManager.registerDriver(new LiferayDriver());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public LiferayDriver() {
	}

	public boolean acceptsURL(String url) throws SQLException {

		// Ensuer that the Liferay JDBC driver wrapper is only used to wrap one
		// type of database connection per VM. For example, you can use this
		// driver to wrap many connections to multiple Oracle databases, but you
		// cannot use this driver to wrap connections to Oracle and DB2
		// databases because the private variable _driver can only refer to one
		// underlying driver type.

		if (_acceptableURLPrefix == null) {
			_acceptableURLPrefix =
				_getJDBCProperties().getProperty("acceptable.url.prefix");
		}

		if ((_acceptableURLPrefix == null) ||
			(url.startsWith(_acceptableURLPrefix))) {

			_log.fine("Accepts url " + url);

			if (_acceptableURLPrefix == null) {
				int x = url.indexOf(":");
				int y = url.indexOf(":", x + 1);

				_acceptableURLPrefix = url.substring(0, y + 1);
			}

			return true;
		}
		else {
			_log.fine("Does not accept url " + url);

			return false;
		}
	}

	public synchronized Connection connect(String url, Properties props)
		throws SQLException {

		if (_driver == null) {
			_log.fine("Driver is null");

			try {
				_log.fine("Url " + url);

				int x = url.indexOf(":");
				int y = url.indexOf(":", x + 1);

				String jdbcType = url.substring(x + 1, y);

				_log.fine("Type " + jdbcType);

				if (jdbcType.equals("db2")) {
					_db2 = true;
				}

				String driverClassName =
					_getJDBCProperties().getProperty(jdbcType);

				_log.fine("Class name " + jdbcType);

				Class driverClass = Class.forName(driverClassName);

				_driver = (Driver)driverClass.newInstance();
			}
			catch (Exception e) {
				throw new SQLException(e.getMessage());
			}
		}
		else {
			_log.fine("Driver is not null");
		}

		Connection con = _driver.connect(url, props);

		_log.fine("Connecting " + con);

		return new LiferayConnection(con, _db2);
	}

	public int getMajorVersion() {
		return _driver.getMajorVersion();
	}

	public int getMinorVersion() {
		return _driver.getMinorVersion();
	}

	public DriverPropertyInfo[] getPropertyInfo(String url, Properties props)
		throws SQLException {

		return _driver.getPropertyInfo(url, props);
	}

	public boolean jdbcCompliant() {
		return _driver.jdbcCompliant();
	}

	private Properties _getJDBCProperties() {
		Properties jdbcProps = new Properties();

		ClassLoader classLoader = getClass().getClassLoader();

		try {
			URL url = classLoader.getResource("jdbc.properties");

			if (url != null) {
				 InputStream is = url.openStream();

				jdbcProps.load(is);

				is.close();

				System.out.println("Loading " + url);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		try {
			URL url = classLoader.getResource("jdbc-ext.properties");

			if (url != null) {
				 InputStream is = url.openStream();

				jdbcProps.load(is);

				is.close();

				System.out.println("Loading " + url);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return jdbcProps;
	}

	private static Logger _log =
		Logger.getLogger(LiferayDriver.class.getName());

	private Driver _driver;
	private String _acceptableURLPrefix;
	private boolean _db2;

}