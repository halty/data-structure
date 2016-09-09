package com.lee.data.structure;

/**
 * simple immutable entry
 * @ThreadSafe if key and value also are immutable class, otherwise
 * @NotThreadSafe
 */
public final class ImmutableEntry<K, V> {
	
	public final K key;
	public final V value;
	
	public ImmutableEntry(K key, V value) {
		this.key = key;
		this.value = value;
	}
	
	@Override
	public int hashCode() {
		int h = 1;
		h = 31*h + (key == null ? 0 : key.hashCode());
		h = 31*h + (value == null ? 0 : value.hashCode());
		return h;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == this) { return true; }
		if (obj == null) return false;
		if (getClass() != obj.getClass()) { return false; }
		
		@SuppressWarnings("rawtypes")
		ImmutableEntry entry = (ImmutableEntry) obj;
		if(key == null) {
			if(entry.key != null) { return false; }
		}else {
			if(!key.equals(entry.key)) { return false; }
		}
		if(value == null) {
			if(entry.value != null) { return false; }
		}else {
			if(!value.equals(entry.value)) { return false; }
		}
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(64);
		builder.append("ImmutableEntry [key=").append(key)
			.append(", value=").append(value).append("]");
		return builder.toString();
	}
}
