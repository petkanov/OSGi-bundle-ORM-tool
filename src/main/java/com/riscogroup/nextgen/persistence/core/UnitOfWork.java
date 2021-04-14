package com.riscogroup.nextgen.persistence.core;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * This object allows tracking changes to individual objects from the Graph of Objects
 * and then committing all of those changes registered in objects in a single transaction atomically 
 * If something in any object screws up during this transaction, each object that has been registered 
 * for the Update will have it's state restored in memory back to what it was before and transaction rolled back
 * @author Peter.Petkanov
 */
public class UnitOfWork {
	private static final Logger logger = LoggerFactory.getLogger(UnitOfWork.class);
	
	private static final ThreadLocal<UnitOfWork> current = new ThreadLocal<>();

	private final Queue<Object> newObjects     = new LinkedList<>();
	private final Queue<Object> updatedObjects = new LinkedList<>();
	private final Queue<Object> deletedObjects = new LinkedList<>();

	public static UnitOfWork getCurrent() {
		if (current.get() == null) {
			current.set(new UnitOfWork());
		}
		return current.get();
	}

	public void registerForInsert(Object object) {
		if (!isRegisterCallFromOutsidePersistence()) {
			return;
		}
		if (!newObjects.contains(object)) {
			newObjects.add(object);
			updatedObjects.remove(object);
		}
	}

	public void registerForUpdate(Object object) {
		if (!isRegisterCallFromOutsidePersistence()) {
			return;
		}
		if (!newObjects.contains(object) && !updatedObjects.contains(object)) {
			updatedObjects.add(object);
		}
	}

	public void registerForDelete(Object object) {
		if (!isRegisterCallFromOutsidePersistence()) {
			return;
		}
		newObjects.remove(object);
		updatedObjects.remove(object);
		if (!deletedObjects.contains(object)) {
			deletedObjects.add(object);
		}
	}

	public void commitSession() throws SQLException {
		insertRegistered();
		deleteRegistered();
		updateRegistered();
	}

	public void restoreRegisteredObjectsState() {
		while (!updatedObjects.isEmpty()) {
			final Object obj = updatedObjects.remove();
			try {
				DaoRegistry.getInstance().getDAO(obj.getClass().getName()).restoreObjectState(obj);
			} catch (SQLException e) {
				logger.error("Object state was not restored properly. It might be in a inconsistent state\nObject: {}", obj);
			}
		}
	}
	
	public void unregisterObject(Object object) {
		updatedObjects.remove(object);
	}
	
	private void deleteRegistered() throws SQLException {
		while (!deletedObjects.isEmpty()) {
			final Object obj = deletedObjects.remove();
			DaoRegistry.getInstance().getDAO(obj.getClass().getName()).delete(obj);
		}
	}

	private void updateRegistered() throws SQLException {
		while (!updatedObjects.isEmpty()) {
			final Object obj = updatedObjects.remove();
			DaoRegistry.getInstance().getDAO(obj.getClass().getName()).update(obj);
		}
	}

	private void insertRegistered() throws SQLException {
		while (!newObjects.isEmpty()) {
			final Object obj = newObjects.remove();
			DaoRegistry.getInstance().getDAO(obj.getClass().getName()).persist(obj);
		}
	}
	
	private boolean isRegisterCallFromOutsidePersistence() {
		if (Thread.currentThread().getStackTrace()[7].toString().contains("com.riscogroup.nextgen.persistence.") ||
			Thread.currentThread().getStackTrace()[8].toString().contains("com.riscogroup.nextgen.persistence.") ||
			Thread.currentThread().getStackTrace()[9].toString().contains("com.riscogroup.nextgen.persistence.") ||
		    Thread.currentThread().getStackTrace()[9].toString().contains(".newInstance")) {
			return false;
		}
		return true;
	}
}
