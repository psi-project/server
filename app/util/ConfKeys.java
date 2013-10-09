package util;

public final class ConfKeys {
	private ConfKeys() { }
	
	/**
	 * Identifies the timeout (in seconds) before the server will respond with
	 * 202 Accepted during training.
	 */
	public static final String TRAINING_TIMEOUT = "psi.max_training_wait";
	
	/** Identifies the scikit learn micro web service root URI. */
	public static final String SKLEARN_SERVICE_ROOT = "psi.sklearn_service.root";
	
	/** Identifies the name of only user who can log in to admin pages. */
	public static final String ADMIN_NAME = "psi.admin.name";
	
	/** Identifies the password for the only user who can log in to admin pages. */
	public static final String ADMIN_PASS = "psi.admin.password";

	/** Identifies the salt used to augment the hashed password. */
	public static final String ADMIN_SALT = "psi.admin.salt";

}
