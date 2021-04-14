package com.riscogroup.nextgen.persistence.core;

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TransactionManager makes it so that all queries Thread performs after starting a transaction with Database
 * will be Atomic within that transaction, Isolated between different threads performing actions in their
 * transactions and that Data in the Database will be Consistent
 * 
 * @author Peter.Petkanov
 */
public class TransactionManager {
	private static final Logger logger = LoggerFactory.getLogger(TransactionManager.class);
	
	private final ConnectionPool cpool;

	private static final ThreadLocal<Connection> context = new ThreadLocal<Connection>() {
		@Override
		public Connection initialValue() {
			logger.error("Uninitialized Session! Can Not get a Connection! Start Session First");
			throw new RuntimeException("Uninitialized Session! Can Not get a Connection! Start Session First");
		}
	};

	private TransactionManager() {
		try {
			this.cpool = new ConnectionPoolImpl();
		} catch (Exception e) {
			logger.error("Connection Pool has not been initialized");
			throw new RuntimeException(e);
		}
	}

	private static class TransactionManagerMaker {
		private static final TransactionManager INSTANCE = new TransactionManager();
	}

	public static TransactionManager getInstance() {
		return TransactionManagerMaker.INSTANCE;
	}

	/**
	 * Gets from the pool a Connection that is gonna be the same for this Thread
	 * whenever and wherever it is required to perform Database DML query within a transaction.
	 *  
	 * Autocommit of this connection is set to false, but if DDL query is performed
	 * within opened transaction it might result into committing all prior to DDL query 
	 * queries, thus breaking atomicity. Do not issue DDL query!  
	 * 
	 * @throws SQLException
	 */
	public void openSessionAndStartTransaction() throws SQLException {
		final Connection conn = cpool.getConnection();
		conn.setAutoCommit(false);
		context.set(conn);
	}
	
	/**
	 * Commits all issued queries after call to openSessionAndStartTransaction() method
	 * as an Atomic action. Then it closes the Connection object and returns it to the pool.
	 *  
	 * @throws SQLException
	 */
	public void commitTransactionAndCloseSession() throws SQLException {
		final Connection conn = context.get();
		try {
			conn.commit();
		} finally {
			closeAndReleaseConnection(conn);
			context.remove();
		}
	}

	/**
	 * If some Exception happens in some of the queries between calls to openSessionAndStartTransaction()
	 * and commitTransactionAndCloseSession() this method will rollback all the changes made by other queries
	 * within opened transaction, then it will close the Connection object and return it back to the pool.
	 */
	public void rollbackTransactionAndCloseSession() {
		final Connection conn = context.get();
		try {
			conn.rollback();
		} catch (SQLException e) {
			logger.error(e.getMessage());
		} finally {
			UnitOfWork.getCurrent().restoreRegisteredObjectsState();
			closeAndReleaseConnection(conn);
			context.remove();
			logger.error("Transaction Failed! Domain objects state restored.");
		}
	}

	public Connection getConnection() {
		return context.get();
	}
	
	private void closeAndReleaseConnection(Connection conn) {
		try {
			conn.close();
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
	}
}
