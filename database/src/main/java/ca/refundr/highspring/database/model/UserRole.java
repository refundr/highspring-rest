package ca.refundr.highspring.database.model;

/**
 * Who the person is allowed to act as in the store.
 */
public enum UserRole {
	CUSTOMER,
	ADMIN;

	public static UserRole fromDb(String value) {
		return UserRole.valueOf(value);
	}
}
