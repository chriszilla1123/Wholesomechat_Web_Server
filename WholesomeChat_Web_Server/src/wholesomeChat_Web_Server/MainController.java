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
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MainController {
	String address = "cs480.javahw.com";
	int port = 2222;
	
	private Socket socket = null;
	private DataInputStream input = null;
	private DataOutputStream out = null;
	
	private static final String CRLF = "\r\n"; // newline
	
	public MainController() {
		try {
			socket = new Socket(address, port);
			System.out.println(new Date() + " :: Connected to Main Server on "
			+ address + ":" + port);
			
			input = new DataInputStream(System.in);
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
	
	//Login to the server.
	//Also from Michael's code.
	public boolean login(String user, String pass) {
		if(user.length() > 0 && pass.length() > 0) {
			JsonObject json = new JsonObject();
			json.addProperty("intent", "login");
			json.addProperty("user", user);
			json.addProperty("pass", pass);
			this.send(json.toString());
		}
		return true;
	}
}
