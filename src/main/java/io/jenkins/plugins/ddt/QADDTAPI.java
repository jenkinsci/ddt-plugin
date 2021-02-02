package io.jenkins.plugins.ddt;

import hudson.util.Secret;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import org.bouncycastle.util.encoders.Hex;

import org.json.JSONObject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This is the QA DDT API implementation according to: https://uiqa.io/api.html
 * @author Evgeny Kolyakov
 */
public class QADDTAPI {
	private static final String RESOURCE_URL = "https://resource.uiqa.io/";
	private static final String RESULTS_URL = "https://s3-eu-west-1.amazonaws.com/tester-ui-qa/uploads/";
	private static final String API_URL = "https://api.uiqa.io/";
	
	private static String DEFAULT_UUID = null;
	
	private String username = null;
	private String password = null;
	
	private transient String uid = null;
	private transient String hash = null;
	
	private static QADDTConfig config;
	
	/**
	 * The constructor gets the credentials from QADDTConfig.
	 */
	@SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", justification = "The config should get updated upon initialization")
	public QADDTAPI() {
		config = QADDTConfig.get();
		
		username = config.getUser();
		password = config.getPass();
	}
	
	/**
	 * Login to the API (https://api.uiqa.io/v1/) with the given credentials. 
	 * @param user {String} The username.
	 * @param pass {String} The password.
	 * @return {boolean} Returns true for a successful login, otherwise, false.
	 */
	public synchronized boolean login(String user, String pass) {
		username = user;
		password = pass;
		
		JSONObject user_obj = _request("login", 
			new JSONObject().put("email", username).put("password", _hash(Secret.toString(Secret.fromString(password)))).toString());
		
		boolean error = user_obj.getBoolean("error");
		
		if (error) {
			System.out.println("QA DDT API response: Wrong credentials!");
		} else {
			uid = user_obj.getString("uid");
			hash = user_obj.getString("hash");
		}
		
		if (DEFAULT_UUID == null) {
			JSONObject defaults_obj = _request("defaults", "{}");
			DEFAULT_UUID = _get_uuid(defaults_obj.getString("tid"), "");
		}
		
		return !error;
	}
	
	/**
	 * Logout from the API (https://api.uiqa.io/v1/) - Revoke the current token.
	 * @return {boolean} Returns true upon a successful logout, otherwise, false.
	 */
	public synchronized boolean logout() {
		if (hash == null) {
			return false;
		}
		
		_request("logout", 
			new JSONObject().put("uid", uid).put("hash", hash).toString());
		
		uid = null;
		hash = null;
		
		return true;
	}
	
	/**
	 * Run the given test (uuid) again with the given tags.
	 * @param uuid {String} The parent test's UUID.
	 * @param tags {String} The tags for the current run/job.
	 * @return {String} Returns the new UUID upon success, otherwise, null.
	 */
	public synchronized String test(String uuid, String tags) {
		// In this context, this is NOT a "clean" test from scratch.
		// This test must be created beforehand in the "qa-app" !!!
		
		if (username == null || password == null || !login(username, password)) {
			return null;
		}
		// At this point we should be logged in.
		
		if (uuid.equals(DEFAULT_UUID)) {
			return uuid;
		}
		
		JSONObject last_test = _request("restore", 
			new JSONObject().put("uid", uid).put("hash", hash).put("uuid", uuid).toString());
		
		if (last_test.getBoolean("error")) {
			return null;
		}
		
		hash = last_test.getString("hash");
		
		String filename = last_test.getString("filename");
		String tmp_file = _fetch(RESOURCE_URL + filename, "application/x-yaml");
		
		return run(tmp_file, tags);
	}
	
	/**
	 * Run the given raw test (yaml) with the given tags.
	 * @param yaml {String} The test data.
	 * @param tags {String} The tags for the current run/job.
	 * @return {String} Returns the new UUID upon success, otherwise, null.
	 */
	public synchronized String test_raw(String yaml, String tags) {
		// In this context, this IS a "clean" test from scratch.
		
		if (username == null || password == null || !login(username, password)) {
			return null;
		}
		// At this point we should be logged in.
		
		return run(new JSONObject().put("test/default.yml", yaml).toString(), tags);
	}
	
	/**
	 * Run the given raw test with the given tags.
	 * @param files {String} The test data.
	 * @param tags {String} The tags for the current run/job.
	 * @return {String} Returns the new UUID upon success, otherwise, null.
	 */
	public synchronized String run(String files, String tags) {
		JSONObject new_test = _request("test", 
			new JSONObject().put("uid", uid).put("hash", hash).put("files", new JSONObject(files)).put("tags", tags).toString());
		
		if (new_test.getBoolean("error")) {
			return null;
		}
		
		String uuid = _get_uuid(new_test.getString("tid"), hash);
		hash = new_test.getString("hash");
		
		return uuid;
	}
	
	/**
	 * Fetch a result via GET.
	 * @param uuid {String} The current run's UUID.
	 * @param filename {String} The file to fetch.
	 * @param mime {String} The file's mime type.
	 * @return {String} Returns the fetched content if successful, otherwise, null.
	 */
	public synchronized String fetch(String uuid, String filename, String mime) {
		String is_uid = "";
		
		if (uid != null && uid.length() > 0 && !uuid.equals(DEFAULT_UUID)) {
			is_uid = _hash(uid) + "/";
		}
		
		return _fetch(RESULTS_URL + is_uid + uuid + "/results/" + filename + "?_=" + Math.random(), mime);
	}
	
	/**
	 * Poll for a result (for maximum "trials" times).
	 * @param uuid {String} The current run's UUID.
	 * @param filename {String}  The file to fetch.
	 * @param mime {String} The file's mime type.
	 * @param trials {Integer} The left number of trials.
	 * @return {String} Returns the requested content if successful, otherwise, null.
	 */
	@SuppressFBWarnings(value = "SWL_SLEEP_WITH_LOCK_HELD", justification = "This is a dedicated thread")
	@SuppressWarnings("SleepWhileInLoop")
	public synchronized boolean poll(String uuid, String filename, String mime, Integer trials) {
		String report;
		
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
		
		return trials > 0;
	}
	
	/**
	 * Render uuid.
	 * @param tid {String} The test ID.
	 * @param hash {String} The test hash.
	 * @return {String} Returns a generated uuid from the tid & hash.
	 */
	private synchronized String _get_uuid(String tid, String hash) {
		return _hash(hash + ":" + tid);
	}
	
	/**
	 * Fetch a URI via GET.
	 * Used for getting the results.
	 * @param path {String} A URI to a file.
	 * @param mime {String} The file's mime type.
	 * @return {String} Returns the fetched content if successful, otherwise, null.
	 */
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
	
	/**
	 * Call a POST request to the API (https://api.uiqa.io/v1/)
	 * @param path {String} One of the API's endpoints.
	 * @param payload {String} The data to send.
	 * @return {JSONObject} Returns the fetched content as a JSONObject if successful, otherwise, null.
	 */
	private synchronized JSONObject _request(String path, String payload) {
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
			
			String ret = _accamulate(response);
			
			if (ret != null) {
				return new JSONObject(ret);
			}
		} catch (Exception e) {
			System.out.println("Sent data: " + payload);
			System.out.println("Exception while requesting /v1/" + path + " : " + e.getMessage());
		}
		
		return null;
	}
	
	/**
	 * Accamulate the received data in batches.
	 * @param response {HttpResponse} The HTTP response.
	 * @return {String} Returns the whole response body.
	 */
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
	
	/**
	 * Create a sha256 out of the given string.
	 * @param str {String} The input data.
	 * @return {String} Returns the sha256 of the given data.
	 */
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