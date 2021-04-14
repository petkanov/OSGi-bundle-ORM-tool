package com.riscogroup.nextgen.persistence.core;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.dbcp2.BasicDataSource;


/**
 * @author Dimitar Traykov
 * @version 1.0.0 October 2015
 */

public class ConnectionPoolImpl implements ConnectionPool {

	private BasicDataSource dataSource = null;

	public ConnectionPoolImpl() throws Exception {
		final String conFile = "etc/db.conf";

		String driverClass = null;
		String url = null;
		String user = null;
		String pass = null;
		Integer minIdleConnections = null;
		Integer maxIdleConnections = null;
		Integer maxPreparedStatements = null;

		Properties props = new Properties();

		try(FileInputStream fis = new FileInputStream(conFile)) {
			props.load(fis);
		} catch (FileNotFoundException e) {
			throw new Exception("Unable to read " + conFile + ". Check if the file exists.");
		}

		/* Load all connection pool configurations from external file */
		driverClass = props.getProperty("DB_DRIVER_CLASS");
		url = props.getProperty("DB_URL");
		user = props.getProperty("DB_USER");
		pass = props.getProperty("DB_PASS");
		
		/* Check the most important configuration parameters */
		if (driverClass == null || driverClass.isEmpty() || url == null || url.isEmpty() || user == null || user.isEmpty() || pass == null) {
			throw new Exception("Missing configuration parameters in " + conFile);
		}

		String property = "";
		if(props.containsKey("DB_MIN_IDLE_CONNECTIONS") && !props.getProperty("DB_MIN_IDLE_CONNECTIONS").isEmpty()) {
			property = props.getProperty("DB_MIN_IDLE_CONNECTIONS");
			minIdleConnections = Integer.parseInt(property);
		} else {
			/* If the property does not exist, put some default value */
			minIdleConnections = 5;
		}

		if(props.containsKey("DB_MAX_IDLE_CONNECTIONS") && !props.getProperty("DB_MAX_IDLE_CONNECTIONS").isEmpty()) {
			property = props.getProperty("DB_MAX_IDLE_CONNECTIONS");
			maxIdleConnections = Integer.parseInt(property);
		} else {
			/* If the property does not exist, put some default value */
			maxIdleConnections = 20;
		}

		if(props.containsKey("DB_MAX_PREPARED_STATEMENTS") && !props.getProperty("DB_MAX_PREPARED_STATEMENTS").isEmpty()) {
			property = props.getProperty("DB_MAX_PREPARED_STATEMENTS");
			maxPreparedStatements = Integer.parseInt(property);
		} else {
			/* If the property does not exist, put some default value */
			maxPreparedStatements = 20;
		}

		/* Load JDBC driver */
		Class.forName(driverClass);

		dataSource = new BasicDataSource();
		dataSource.setDriverClassName(driverClass);
		dataSource.setUrl(url);
		dataSource.setUsername(user);
		dataSource.setPassword(pass);
		dataSource.setMinIdle(minIdleConnections);
		dataSource.setMaxIdle(maxIdleConnections);
		dataSource.setMaxOpenPreparedStatements(maxPreparedStatements);
	}

	/**
	 * Pulls Connection object from data source and gives it to the caller method.
	 * 
	 * @return Connection object.
	 */
	public synchronized Connection getConnection() throws SQLException {
		Connection connection;
		connection = (Connection) dataSource.getConnection();
		return connection;
	}
}
