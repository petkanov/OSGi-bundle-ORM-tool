package com.riscogroup.nextgen.persistence.core;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IdentityMap<T> {

	private final Map<Integer, T> cache = new ConcurrentHashMap<>();
	
	public T addIfAbsent(Integer key, T value) {
		return cache.putIfAbsent(key, value);
	}
	
	public void replace(Integer key, T newValue) {
		cache.put(key, newValue);
	}

	public T get(Integer objectId) {
		return cache.get(objectId);
	}
	
	public Map<Integer, T> getAll(){
		return Collections.unmodifiableMap(cache);
	}

	public boolean contains(int objectId) {
		return cache.containsKey(objectId);
	}

	public void remove(int objectId) {
		cache.remove(objectId);
	}
	
	public String getExistingIndexesString() {
		final StringBuilder sb = new StringBuilder();
		for(Integer key : cache.keySet()) {
			sb.append(key).append(",");
		}
		if(sb.length() == 0) {
			return "";
		}
		return sb.replace(sb.length() - 1, sb.length(), "").toString();
	}

	public boolean isEmpty() {
		return cache.size() == 0;
	}
}
