package services.moleculer.utils;

import services.moleculer.Service;

final class ServiceContainer {

	// --- COMMON PROPERTIES ---

	final boolean cached;
	final boolean local;

	// --- LOCAL PROPERTIES ---

	final Service service;

	// --- MIDDLEWARES ---

	// Middleware[] middlewares

	// --- CONSTRUCTORS ---

	ServiceContainer(Service service, boolean cached) {
		this.service = service;
		this.cached = cached;
		this.local = true;
	}

	ServiceContainer(String nodeID, String name, boolean cached) {
		this.service = null;
		this.cached = cached;
		this.local = false;
	}

	// --- HELPERS ---

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (cached ? 1231 : 1237);
		result = prime * result + (local ? 1231 : 1237);
		result = prime * result + ((service == null) ? 0 : service.hashCode());
		return result;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ServiceContainer other = (ServiceContainer) obj;
		if (cached != other.cached) {
			return false;
		}
		if (local != other.local) {
			return false;
		}
		if (service == null) {
			if (other.service != null) {
				return false;
			}
		} else if (service != other.service) {
			return false;
		}
		return true;
	}

}