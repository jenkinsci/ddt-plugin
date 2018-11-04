package io.jenkins.plugins.ddt;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import org.bouncycastle.util.encoders.Hex;

import java.net.*;
import java.io.*;
import java.util.*;
import org.json.*;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


public class QADDTAPI {
	// private static final String API_URL = "https://qa-api.doorzz.com/";
	private static final String API_URL = "http://localhost:8008/";
	private static final String RESOURCE_URL = "https://qa-resource.doorzz.com/";
	
	private static String username = null;
	private static String password = null;
	
	private static String uid = null;
	private static String hash = null;
	
	public static String getUsername() {
		return username;
	}
	
	public static String getPassword() {
		return password;
	}
	
	public static boolean login(String user, String pass) {
		username = user;
		password = pass;
		boolean error = false;
		
		String auth = _request("login", "{" +
				"\"email\": \"" + username + "\", " +
				"\"password\": \"" + _hash(password) + "\"" +
			"}");
		
		JSONObject user_obj = new JSONObject(auth);
		
		error = user_obj.getBoolean("error");
		
		if (error) {
			System.out.println("QA DDT API response: Wrong credentials!");
		} else {
			uid = user_obj.getString("uid");
			hash = user_obj.getString("hash");
		}
		
		return !error;
	}
	
	public static boolean logout() {
		if (hash == null) {
			return true;
		}
		
		_request("logout", "{" +
				"\"uid\": \"" + uid + "\", " +
				"\"hash\": \"" + hash + "\"" +
			"}");
		
		return false;
	}
	
	public static String test(String uuid, String tags) {
		// In this context, this is NOT a "clean" test from scratch.
		// This test must be created beforehand in the "qa-app" !!!
		
		if (hash == null) {
			if (username == null || password == null || !login(username, password)) {
				return null;
			}
			// At this point we should be logged in.
		}
		
		String last_test = _request("restore", "{" +
				"\"uid\": \"" + uid + "\", " +
				"\"hash\": \"" + hash + "\", " +
				"\"uuid\": \"" + uuid + "\"" +
			"}");
		
		JSONObject last_test_obj = new JSONObject(last_test);
		
		if (last_test_obj.getBoolean("error")) {
			return null;
		}
		
		hash = last_test_obj.getString("hash");
		
		String filename = last_test_obj.getString("filename");
		String tmp_file = _fetch(RESOURCE_URL + filename, "application/x-yaml");
		
		// TODO: Replace bash ${ENV.foo}
		
		String new_test = _request("test", "{" +
				"\"uid\": \"" + uid + "\", " +
				"\"hash\": \"" + hash + "\", " +
				"\"files\": \"" + tmp_file + "\", " +
				"\"tags\": \"" + tags + "\"" +
			"}");
		
		JSONObject new_test_obj = new JSONObject(new_test);
		
		if (new_test_obj.getBoolean("error")) {
			return null;
		}
		
		hash = new_test_obj.getString("hash");
		uuid = _hash(new_test_obj.getString("tid"));
		
		return uuid;
	}
	
	private static String _fetch(String path, String mime) {
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(path);
		request.setHeader("Content-type", mime);
		
		try {
			HttpResponse response = httpClient.execute(request);
			System.out.println("QA DDT API response status code: " + response.getStatusLine().getStatusCode());
			
			return _accamulate(response);
		} catch (Exception e) {
			System.out.println("Exception while requesting: " + e.getMessage());
		}
		
		return null;
	}
	
	private static String _request(String path, String payload) {
		StringEntity entity = new StringEntity(payload, ContentType.APPLICATION_JSON);

		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpPost request = new HttpPost(API_URL + "v1/" + path);
		request.setEntity(entity);
		request.setHeader("Content-type", "application/json");
		
		try {
			HttpResponse response = httpClient.execute(request);
			System.out.println("QA DDT API response status code: " + response.getStatusLine().getStatusCode());
			
			return _accamulate(response);
		} catch (Exception e) {
			System.out.println("Exception while requesting: " + e.getMessage());
		}
		
		return null;
	}
	
	@SuppressFBWarnings(value = "DM_DEFAULT_ENCODING", justification = "This is in try-catch")
	private static String _accamulate(HttpResponse response) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())))) {
			// Read in all of the post results into a String.
			Boolean keepGoing = true;
			StringBuffer output = new StringBuffer();
			while (keepGoing) {
				String currentLine = br.readLine();
				
				if (currentLine == null) {
					keepGoing = false;
				} else {
					output.append(currentLine);
				}
			}
			
			return output.toString();
		} catch (Exception e) {
			System.out.println("Exception while parsing response: " + e.getMessage());
		}
		
		return null;
	}
	
	@SuppressFBWarnings(value = "DM_DEFAULT_ENCODING", justification = "This is in try-catch and should return ASCII")
	private static String _hash(String str) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(str.getBytes(StandardCharsets.UTF_8));
			return new String(Hex.encode(hashed));
		} catch (Exception e) {
			System.out.println("Exception encrypting hash: " + e.getMessage());
		}
		
		return null;
	}
}