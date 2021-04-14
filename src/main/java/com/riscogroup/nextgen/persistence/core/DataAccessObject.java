package com.riscogroup.nextgen.persistence.core;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * All Database Mapper Objects (Data Access Objects) should extend this class
 * and implement all appropriate for their data manipulation logic methods.
 * Data Access Objects purpose is to map certain fields of Domain Object instance of Type <T>
 * with corresponding table and row. Thus implementing CRUD functionality.
 * These DAO entities are the only ones wit Database knowledge and communication
 *   
 * @author Peter.Petkanov
 *
 * @param <T> Type of the concrete Domain Model Object, state of which needs to be persisted
 */
public abstract class DataAccessObject<T> {
	private final IdentityMap<T> cache;
	private final TransactionManager transactionManager;
	
	public DataAccessObject() {
		cache = new IdentityMap<>();
		transactionManager = TransactionManager.getInstance();
	}
	
	public abstract boolean persist(T object) throws SQLException;

	public abstract boolean update(T object) throws SQLException;

	public abstract List<T> getAllForID(int parentId) throws SQLException;

	public abstract boolean delete(Integer objectId) throws SQLException;

	public abstract boolean delete(T obj) throws SQLException;
	
	public abstract T get(int objectId) throws SQLException;
	
	public abstract Map<Integer, T> getAll() throws SQLException;

	public abstract void restoreObjectState(T object) throws SQLException;
	
	protected IdentityMap<T> getCache() {
		return cache;
	}

	protected TransactionManager getTransactionManager() {
		return transactionManager;
	}

	protected Map<String, String> deserializeMapFromToString(final String mapString)   {
		final Map<String, String> newMap = new HashMap<>();
		if(mapString != null && mapString.length() > 2) {
			final String[] mapArr = mapString.replaceAll("[\\{\\}]", "").split(",");
			for(String line : mapArr) {
				final String[] lineArr = line.split("=");
				final String value = lineArr.length > 1 ? lineArr[1].trim() : "";
				newMap.put(lineArr[0].trim(), value);
			}
		}
		return newMap;
	} 
	
	protected List<String> deserializeListFromToString(final String listString)   {
		final List<String> newList = new ArrayList<>();
		if(listString != null && listString.length() > 2) {
			final String[] listArr = listString.replaceAll("[\\[\\]]", "").split(",");
			for(String line : listArr) {
				String tmp = line.trim(); 
				if(tmp.length() > 0) {
					newList.add(tmp);
				}
			}
		}
		return newList;
	} 

	protected String[] deserializeArrayFromString(final String listString)   {
		String[] listArr = null;
		if(listString != null && listString.length() > 2) {
			listArr = listString.replaceAll("[\\[\\]]", "").split(",");
			for(int i = 0; i < listArr.length; i++) {
				listArr[i] = listArr[i].trim();
			}
		}
		return listArr;
	} 
}
