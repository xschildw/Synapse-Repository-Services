package org.sagebionetworks.audit.utils;

public class ObjectRecordUtils {

	public static Long synapseIdToLong(String synapseId) {
		if (synapseId == null)
			return null;
		synapseId = synapseId.trim().toLowerCase();
		if (synapseId.startsWith("syn"))
			synapseId = synapseId.substring(3);
		return Long.parseLong(synapseId);
	}

}
