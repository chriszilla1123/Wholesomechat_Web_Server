/*
 * Connects the the main server to log in a user, send and receive
 * messages, and analyze messages.
 * 
 * Will be used if I am able to get it connected to the main server in time.
 */

package wholesomeChat_Web_Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MainController {
	String address = "cs480.javahw.com";
	int port = 2222;
	
	private Socket socket = null;
	private DataInputStream input = null;
	private DataOutputStream out = null;
	
	ArrayList<String> responses = new ArrayList<String>();
	int nextResponse = 0;
	
	private static final String CRLF = "\r\n"; // newline
	
	public MainController() {
		try {
			socket = new Socket(address, port);
			/*System.out.println(new Date() + " :: Connected to Main Server on "
			+ address + ":" + port);*/
			
			input = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
		}
		catch(UnknownHostException u) {
			System.out.println(u);
		}
		catch(IOException i){
			System.out.println(i);
		}
	}
	
	public void close() {
		try {
			socket.close();
		}
		catch(IOException i) {
			System.out.println(i);
		}
	}
	
	//Send a lind of text to the main server
	//Got this from Michael's code.
	public void send(String text) {
        try {
            out.write((text + CRLF).getBytes());
            out.flush();
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }
	
	public boolean sendMessage(String text) {
		if(text.length() > 0) {
			JsonObject json = new JsonObject();
			json.addProperty("intent", "message");
			json.addProperty("text", text);
			this.send(json.toString());
			return true;
		}
		return false;
	}
	
	//Login to the server.
	//Also from Michael's code.
	public boolean login(String user, String pass) {
		if(user.length() > 0 && pass.length() > 0) {
			JsonObject json = new JsonObject();
			json.addProperty("intent", "login");
			json.addProperty("user", user);
			json.addProperty("pass", hashPass(pass));
			this.send(json.toString());
			return true;
		}
		return false;
	}
	
	@SuppressWarnings("deprecation")
	public void responseListener() {
		String response;
		while(true) {
			try {
				response = input.readLine();
				if(response != null) {
					responses.add(response);
					//System.out.println(response);
				}
			}
			catch(IOException e) {
				System.out.println(e);
			}
		}
	}
	
	public ArrayList<String> getAllResponses() {
		return responses;
	}
	
	public ArrayList<String> updateResponses() {
		ArrayList<String> result = new ArrayList<String>();
		for(int i=nextResponse; i<responses.size(); i++) {
			result.add(responses.get(i));
		}
		nextResponse = responses.size();
		return result;
	}
	
	public String getNextResponse() {  //Needs more testing.
		return responses.get(nextResponse++);
	}
	
	public boolean hasNewResponse() {
		return(nextResponse == responses.size());  //Also needs more testing.
	}
	
	public String hashPass(String pass) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-512");
			byte[] bytes;
			bytes = md.digest(pass.getBytes("UTF-8"));
			StringBuilder sb = new StringBuilder();
			for(int i=0; i < bytes.length; i++) {
				sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			String hashedPass = sb.toString();
			return hashedPass;
		} catch (NoSuchAlgorithmException e) {
			System.out.println(e);
		} catch (UnsupportedEncodingException e) {
		}
			
		return null;
	}
}
