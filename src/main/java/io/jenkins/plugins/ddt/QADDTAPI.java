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
	private static final String RESOURCE_URL = "https://qa-resource.doorzz.com/";
	private static final String RESULTS_URL = "https://s3-eu-west-1.amazonaws.com/tester-qa/uploads/";
	
	private String API_URL = null;
	
	private String username = null;
	private String password = null;
	
	private String uid = null;
	private String hash = null;
	
	private transient QADDTConfig config;
	
	public QADDTAPI() {
		config = QADDTConfig.get();
		
		username = config.getUser();
		password = config.getPass();
		
		if (config.isDevMode()) {
			API_URL = "http://localhost:8008/";
		} else {
			API_URL = "https://qa-api.doorzz.com/";
		}
	}
	
	public synchronized boolean login(String user, String pass) {
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
	
	public synchronized boolean logout() {
		if (hash == null) {
			return true;
		}
		
		_request("logout", "{" +
				"\"uid\": \"" + uid + "\", " +
				"\"hash\": \"" + hash + "\"" +
			"}");
		
		return false;
	}
	
	public synchronized String test(String uuid, String tags) {
		// In this context, this is NOT a "clean" test from scratch.
		// This test must be created beforehand in the "qa-app" !!!
		
		if (username == null || password == null || !login(username, password)) {
			return null;
		}
		// At this point we should be logged in.
		
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
				"\"uid\": \"" + uid + "\"," +
				"\"hash\": \"" + hash + "\"," +
				"\"files\": " + tmp_file + "," + // This must be left unquoted
				"\"tags\": \"" + tags + "\"" +
			"}");
		
		// System.out.println(">>> Received data: " + new_test);
		
		JSONObject new_test_obj = new JSONObject(new_test);
		
		if (new_test_obj.getBoolean("error")) {
			return null;
		}
		
		uuid = _hash(hash + ":" + new_test_obj.getString("tid")); // TODO: Document this !!!
		hash = new_test_obj.getString("hash");
		
		return uuid;
	}
	
	public synchronized String fetch(String uuid, String filename, String mime) {
		String is_uid = "";
		
		if (uid != null && uid.length() > 0 && !uid.equals("93a72a541c29aed27b59155266b7f04f1a6bd89df23dc434e471f5eb6c818050")) {
			is_uid = _hash(uid) + "/";
		}
		
		return _fetch(RESULTS_URL + is_uid + uuid + "/results/" + filename + "?_=" + Math.random(), mime);
	}
	
	@SuppressFBWarnings(value = "SWL_SLEEP_WITH_LOCK_HELD", justification = "This is a dedicated thread")
	public synchronized boolean poll(String uuid, String filename, String mime, Integer trials) {
		String report = null;
		
		do {
			report = fetch(uuid, filename, mime);
			if (trials > 1) {
				// Don't wait for the next loop in the last iteration
				try {
					Thread.sleep(7000); // ~ 5000 in java ... epic fail...
				} catch (Exception e) {
					System.out.println("Failed sleeping: " + e.getMessage());
					trials = 0;
				}
			}
			--trials;
		} while (trials > 0 && (report == null || report.length() == 0));
		
		if (trials <= 0) {
			return false;
		}
		
		return true;
	}
	
	private synchronized String _fetch(String path, String mime) {
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(path);
		request.setHeader("Content-type", mime);
		
		try {
			HttpResponse response = httpClient.execute(request);
			Integer status_code = response.getStatusLine().getStatusCode();
			System.out.println("Fetched " + path + " with status code " + status_code);
			
			if (status_code != 200) {
				return null;
			}
			
			return _accamulate(response);
		} catch (Exception e) {
			System.out.println("Exception while fetching " + path + " : " + e.getMessage());
		}
		
		return null;
	}
	
	private synchronized String _request(String path, String payload) {
		StringEntity entity = new StringEntity(payload, ContentType.APPLICATION_JSON);

		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpPost request = new HttpPost(API_URL + "v1/" + path);
		request.setEntity(entity);
		request.setHeader("Content-type", "application/json");
		
		try {
			HttpResponse response = httpClient.execute(request);
			Integer status_code = response.getStatusLine().getStatusCode();
			System.out.println("Requested " + path + " with status code " + status_code);
			
			if (status_code != 200) {
				return null;
			}
			
			return _accamulate(response);
		} catch (Exception e) {
			System.out.println("Sent data: " + payload);
			System.out.println("Exception while requesting /v1/" + path + " : " + e.getMessage());
		}
		
		return null;
	}
	
	private synchronized String _accamulate(HttpResponse response) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"))) {
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
	
	private synchronized String _hash(String str) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(str.getBytes(StandardCharsets.UTF_8));
			return new String(Hex.encode(hashed), "UTF-8");
		} catch (Exception e) {
			System.out.println("Exception encrypting hash: " + e.getMessage());
		}
		
		return null;
	}
}