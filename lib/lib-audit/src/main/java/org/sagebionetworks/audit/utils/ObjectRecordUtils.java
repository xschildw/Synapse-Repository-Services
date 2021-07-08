package org.sagebionetworks.audit.utils;

public class ObjectRecordUtils {

	public static long synapseIdToLong(String synapseId) {
		if (synapseId == null)
			throw new IllegalArgumentException();
		synapseId = synapseId.trim().toLowerCase();
		if (synapseId.startsWith("syn"))
			synapseId = synapseId.substring(3);
		return Long.parseLong(synapseId);
	}

}
