/*
 * Copyright (c) 2016 by RISCO Group,
 * url: http://www.riscogroup.com/
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information
 * of RISCO Group. You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms
 * of the license agreement you entered into with RISCO Group.
 */
package com.riscogroup.nextgen.persistence.core;

import java.sql.Connection;
import java.sql.SQLException;


/**
 * @author Dimitar Traykov
 * @version 1.0.0 October 2015
 */

public interface ConnectionPool {
	/**
	 * Pulls Connection object from data source and gives it to the caller method.
	 * 
	 * @return Connection object.
	 */
	public Connection getConnection() throws SQLException;
}
