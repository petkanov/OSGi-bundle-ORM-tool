package com.riscogroup.nextgen.persistence.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.riscogroup.nextgen.home.api.monitor.ServiceStatusResponse;
import com.riscogroup.nextgen.home.api.monitor.StatusObservable;
import com.riscogroup.nextgen.home.api.services.DataService;
import com.riscogroup.nextgen.persistence.core.DaoRegistry;
import com.riscogroup.nextgen.persistence.core.TransactionManager;
import com.riscogroup.nextgen.persistence.core.UnitOfWork;

public class PersistenceServiceImpl implements DataService, StatusObservable {
	private static final Logger logger = LoggerFactory.getLogger(DataService.class);
	private final TransactionManager transactionManager;
	
	public PersistenceServiceImpl() {
		this.transactionManager = TransactionManager.getInstance();
	}
	
	@Override
	public void registerForInsert(Object object) {
		UnitOfWork.getCurrent().registerForInsert(object);
	}

	@Override
	public void registerForUpdate(Object object) {
		UnitOfWork.getCurrent().registerForUpdate(object);
	}

	@Override
	public void registerForDelete(Object object) {
		UnitOfWork.getCurrent().registerForDelete(object);
	}

	@Override
	public boolean commitTransactionRegisteredWork() {
		try {
			transactionManager.openSessionAndStartTransaction();
			UnitOfWork.getCurrent().commitSession();
			transactionManager.commitTransactionAndCloseSession();
			return true;
		}catch(Exception e) {
			logger.error(e.getMessage());
			transactionManager.rollbackTransactionAndCloseSession();
			return false;
		}
	}

	@Override
	public boolean persistObject(Object object) {
		try {
			transactionManager.openSessionAndStartTransaction();
			DaoRegistry.getInstance().getDAO(object.getClass().getName()).persist(object);
			transactionManager.commitTransactionAndCloseSession();
			return true;
		}catch(Exception e) {
			logger.error(e.getMessage());
			transactionManager.rollbackTransactionAndCloseSession();
			return false;
		}
	}

	@Override
	public boolean updateObject(Object object) {
		try {
			transactionManager.openSessionAndStartTransaction();
			DaoRegistry.getInstance().getDAO(object.getClass().getName()).update(object);
			transactionManager.commitTransactionAndCloseSession();
			return true;
		}catch(Exception e) {
			logger.error(e.getMessage());
			transactionManager.rollbackTransactionAndCloseSession();
			return false;
		}
	}

	@Override
	public boolean deleteObjectById(Class<?> objectClass, Integer objectId) {
		try {
			transactionManager.openSessionAndStartTransaction();
			DaoRegistry.getInstance().getDAO(objectClass.getName()).delete(objectId);
			transactionManager.commitTransactionAndCloseSession();
			return true;
		}catch(Exception e) {
			logger.error(e.getMessage());
			transactionManager.rollbackTransactionAndCloseSession();
			return false;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getObjectById(Class<?> objectClass, int objectId) {
		try {
			transactionManager.openSessionAndStartTransaction();
			final T object = (T) DaoRegistry.getInstance().getDAO(objectClass.getName()).get(objectId);
			transactionManager.commitTransactionAndCloseSession();
			return object;
		}catch(Exception e) {
			logger.error(e.getMessage());
			transactionManager.rollbackTransactionAndCloseSession();
			return null;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Map<Integer, T> getAllObjects(Class<?> objectClass) {
		Map<Integer, T> objects = null;
		try {
			transactionManager.openSessionAndStartTransaction();
			objects = (Map<Integer, T>) DaoRegistry.getInstance().getDAO(objectClass.getName()).getAll();
			transactionManager.commitTransactionAndCloseSession();
		}catch(Exception e) {
			logger.error(e.getMessage());
			transactionManager.rollbackTransactionAndCloseSession();
		}
		return objects;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> getAllObjectsForID(Class<?> objectClass, int parentId) {
		List<T> objects = null;
		try {
			transactionManager.openSessionAndStartTransaction();
			objects = (List<T>) DaoRegistry.getInstance().getDAO(objectClass.getName()).getAllForID(parentId);
			transactionManager.commitTransactionAndCloseSession();
		}catch(Exception e) {
			logger.error(e.getMessage());
			transactionManager.rollbackTransactionAndCloseSession();
		}
		return objects;
	}

	@Override
	public ServiceStatusResponse getServiceStatus() {
		final ServiceStatusResponse response = new ServiceStatusResponse();
		response.setMessage(this.getClass().getCanonicalName());
		response.setServiceStatus(true);
		return response;
	}
}
