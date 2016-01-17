package com.droid.melodydroid.display;

import com.droid.melodydroid.helper.CaseInsensitiveString;

public class ValueItem implements Comparable<Object> {

	private String value;
	
	public ValueItem (String value) {
        if (value == null)
            throw new NullPointerException();

		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
    public boolean equals(Object o) {
        return o instanceof ValueItem &&
            ((ValueItem)o).value.equalsIgnoreCase(value);
    }

    private volatile int hashCode = 0; 

    public int hashCode() {
        if (hashCode == 0)
            hashCode = value.toUpperCase().hashCode();

        return hashCode;
    }

    public int compareTo(Object o) {
        ValueItem valueItem = (ValueItem)o;
        return String.CASE_INSENSITIVE_ORDER.compare(value, valueItem.value);
    }

    public String toString() {
        return value;
    }	
	
}
