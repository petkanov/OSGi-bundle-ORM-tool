package com.riscogroup.nextgen.home.api.services;

import java.util.List;
import java.util.Map;

public interface DataService {

	void registerForInsert(Object object);

	void registerForUpdate(Object object);

	void registerForDelete(Object object);

	boolean commitTransactionRegisteredWork();

	boolean persistObject(Object object);

	boolean updateObject(Object object);

	boolean deleteObjectById(Class<?> objectClass, Integer objectId);

	<T> T getObjectById(Class<?> objectClass, int objectId);

	<T> Map<Integer, T> getAllObjects(Class<?> objectClass);

	<T> List<T> getAllObjectsForID(Class<?> objectClass, int parentId);
}