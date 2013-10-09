package controllers;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base64;

import play.Logger;

import util.ConfKeys;

public class Security extends Secure.Security {
	private static final String ONLY_USER_NAME = play.Play.configuration.getProperty( ConfKeys.ADMIN_NAME );
	private static final String ONLY_USER_PASSWORD = play.Play.configuration.getProperty( ConfKeys.ADMIN_PASS );
	private static final String ONLY_USER_SALT = play.Play.configuration.getProperty( ConfKeys.ADMIN_SALT );
	
	static boolean authenticate(String username, String password) {
		try {
			return ONLY_USER_NAME.equals(username) && ONLY_USER_PASSWORD.equals( saltedHashedBase64Encoded(password) );
		} catch (NoSuchAlgorithmException e) {
			throw new Error("Unable to hash password; apparently the selected algorithm has disappeared from Java's capabilities.", e);
		}
	}
	
	static String saltedHashedBase64Encoded(String password) throws NoSuchAlgorithmException {
		return saltedHashedBase64Encoded(password, ONLY_USER_SALT);
	}
	
	static String saltedHashedBase64Encoded(String password, String salt) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update((salt + password).getBytes());
		return  Base64.encodeBase64String( md.digest() ).trim(); //remove trailing newline
	}

}
