/*
 * Hosts the WebSocket server to connect to the web client 
 */

package wholesomeChat_Web_Server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.ToneAnalyzer;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneAnalysis;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneOptions;

public class WebServer extends WebSocketServer{
	
	int port = 1337;
	ArrayList<Connection> connections = new ArrayList<Connection>();
	ArrayList<Message> messages = new ArrayList<Message>();
	String colors[] = {"red", "blue", "green", "orange", "yellow", "purple"};
	
	MainController listenServer = new MainController();
	Runnable listenTask;
	Runnable broadcastTask;
	Thread listenThread;
	Thread broadcastThread;
	ArrayList<String> listenResponses = new ArrayList<String>();
	
	public WebServer(InetSocketAddress address) {
		super(address);
		//Start the new thread to get responses
		//This one ties into the 'broadcast' method.
		listenServer.login("WebServer_User", "password");
		listenTask = () -> {
			listenServer.responseListener();
		};
		listenThread = new Thread(listenTask);
		listenThread.start();
		
		broadcastTask = () -> {
			broadcast();
		};
		broadcastThread = new Thread(broadcastTask);
		broadcastThread.start();
	}

	@Override
	public void onClose(WebSocket conn, int arg1, String arg2, boolean arg3) {
		Connection connection = searchConnection(conn);
		JsonObject json = new JsonObject();
		json.addProperty("intent", "message");
		json.addProperty("text", "/quit");
		connection.server.send(json.toString());
		System.out.println(new Date() + " :: User " + connection.username
				+ " has left the chat");
		connections.remove(connection);
	}

	@Override
	public void onError(WebSocket conn, Exception arg1) {
		
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		Connection connection = searchConnection(conn);
		
		if(connection != null) {
			if(connection.username == "") { //First message is always username, case 1
				
				JsonElement jelement = new JsonParser().parse(message);
				JsonObject loginData = jelement.getAsJsonObject();
				String username = loginData.get("user").getAsString();
				String password = loginData.get("pass").getAsString();
				
				String nick = loginUser(connection, username, password);
				
				if(nick != null) {
				connection.username = nick;  //Sets username
				connection.usercolor = pickColor();  //Sets usercolor
				//connection.server.login(username, password);  //Log this user in.
				//Send response to client with chosen color.
				JsonObject json = new JsonObject();
				json.addProperty("type", "login");
				//json.addProperty("color", connection.usercolor);
				json.addProperty("user", nick);
				connection.conn.send(json.toString());
				
				System.out.println(new Date() + " :: User " + username + " has"
						+ " joined the chat");
				}
			}
			else { //All other messages, case 2.
				if(true) {  //Message is wholesome
					JsonObject json = new JsonObject();
					json.addProperty("intent", "message");
					json.addProperty("text", message);
					connection.server.send(json.toString());
					
					System.out.println(new Date() + " :: User " + connection.username
							+ " has sent a message: \"" + message + "\"");
				}
				/*else {  //Message isn't wholesome
					JsonObject json = new JsonObject();
					json.addProperty("type", "wholesome");
					json.addProperty("data", false);
					connection.conn.send(json.toString());
					System.out.println(new Date() + " :: User " + connection.username
							+ " has sent an unwholesome message: \""
							+ message + "\"");
				}*/
			}
		}
		else {
			System.out.println(new Date() + " :: Error: Connection not found");
		}
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake arg1) {
		//New server instance for each user is required.
		MainController server = new MainController();
		
		//Create the new Connection and add it to the array.
		connections.add(new Connection(conn, server));
		
		//Send new connection history of messages.
		//if(messages.size() > 0) conn.send(jsonHistory().toString());
		
		System.out.println(new Date() + " :: New connection from origin " 
				+ conn.getRemoteSocketAddress());
	}

	@Override
	public void onStart() {
		
	}
	
	/*
	 *Allows me to find a Connection object based on a username. 
	 *  See the Connection class below for more info
	 */
	public Connection searchUsername(String user) {
		for(int i=0; i < connections.size(); i++) {
			Connection connection = connections.get(i);
			
			if(connection.username != "" &&
					connection.username == user) {
				
				return connection;
			}
		}
		return null;
	}
	
	/*
	 * Allows me to find a Connection object by the users WebSocket connection.
	 */
	public Connection searchConnection(WebSocket conn) {
		for(int i=0; i < connections.size(); i++) {
			Connection connection = connections.get(i);
			
			if(connection.conn == conn) return connection;
		}
		return null;
	}
	
	public String loginUser(Connection conn, String user, String pass) {
		conn.server.login(user, pass);
		//Wait for user response
		boolean loggedIn = false;
		String username = null;
		int i = 0;
		
		ArrayList<String> response = new ArrayList<String>();
		while(!loggedIn) {
			if(i >= 10) {
				break;
			}
			if(conn.server.hasNewResponse()) {
				System.out.print(".");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				response = conn.updateResponses();
				if(response.size() != 0) {
					JsonParser parser = new JsonParser();
					JsonObject json = parser.parse(response.get(0)).getAsJsonObject();
					if(json.has("nick")) {
						username = json.get("nick").getAsString();
						System.out.println("found nick");
						loggedIn = true;
					}
				}
			}
			i++;
		}
		
		return username;
	}
	
	//Constantly running process listening for new messages.
	public void broadcast() {
		int numResponses = 0;
		while(true) {
			if(listenServer.hasNewResponse()) {
			System.out.println("");
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			listenResponses = listenServer.updateResponses(); 
			if(listenResponses.size() != numResponses){
				System.out.println("Response received!");
				for(int i = numResponses; i < listenResponses.size(); i++){
					JsonParser parser = new JsonParser();
					JsonObject json = parser.parse(listenResponses.get(i)).getAsJsonObject();
					if(json.has("text")) {
						System.out.println("Got to this point!");
						JsonObject message = new JsonObject();
						message.addProperty("type", "message");
						message.addProperty("data", json.get("text").getAsString());
						for(Connection connection : connections) {
							connection.conn.send(message.toString());
						}
					}
				}
				
				numResponses = listenResponses.size();
			}
			}
		}
	}
	
	//Formats a single message as JSON
	public JsonObject jsonMessage(Message message) {
		JsonObject json = new JsonObject();
		JsonObject formattedMessage = formatMessage(message);
		json.addProperty("type", "message");
		json.add("data", formattedMessage);
		
		return json;
	}
	
	//Formats history of messages as JSON
	public JsonObject jsonHistory() {
		JsonObject json = new JsonObject();
		JsonArray formattedHistory = formatHistory();
		json.addProperty("type", "messages");
		json.add("data", formattedHistory);
		
		return json;
	}
	
	/*
	 * Helper function for jsonMessage()
	 */
	private JsonObject formatMessage(Message message) {
		JsonObject formatted = new JsonObject();
		formatted.addProperty("time", message.time);
		formatted.addProperty("text", message.text);
		formatted.addProperty("user", message.user);
		formatted.addProperty("color", message.color);
		
		return formatted;
	}
	
	/*
	 * Helper function for jsonHistory()
	 */
	private JsonArray formatHistory() {
		JsonArray formatted = new JsonArray();
		
		for(int i=0; i < messages.size(); i++) {
			JsonObject message = new JsonObject();
			message.addProperty("time", messages.get(i).time);
			message.addProperty("text", messages.get(i).text);
			message.addProperty("user", messages.get(i).user);
			message.addProperty("color", messages.get(i).color);
			
			formatted.add(message);
		}
		return formatted;
	}
	
	/*
	 * Assigns the user a random color.  Used for displaying in the
	 * 		web client.
	 */
	public String pickColor() {
		int color =  ThreadLocalRandom.current().nextInt(0, colors.length + 1);
		return colors[color];
	}
	
	//=========== Connection Class ===============================
	/*
	 * This allows the WebSocket connection, username, and usercolor to all be
	 * 		tied together.  Easily keep track of connection, add and remove as
	 * 		users join and leave.
	 */
	public class Connection{
		WebSocket conn;
		MainController server;
		public String username = "";
		public String usercolor = "";
		Runnable task;
		Thread thread;
		ArrayList<String> responses = new ArrayList<String>();
		
		public Connection(WebSocket conn, MainController server) {
			this.conn = conn;
			this.server = server;
			
			//Start the new thread to get responses
			task = () -> {
				server.responseListener();
			};
			thread = new Thread(task);
			thread.start();
		}
		
		public void setServer(MainController server) {
			this.server = server;
		}
		
		public void setUsername(String user) {
			this.username = user;
		}
		
		public void setUsercolor(String color) {
			this.usercolor = color;
		}
		
		public void startThread() {
			thread.start();
		}
		
		@SuppressWarnings("deprecation")
		public void stopThread() {
			thread.stop();
		}
		
		public ArrayList<String> updateResponses() {
			ArrayList<String> newResponses = server.updateResponses();
			responses.addAll(newResponses);
			return newResponses;
		}
		
		public void postMessages() {
			Runnable task = () -> {
				if(server.hasNewResponse()) {
					ArrayList<String> newResponses = this.updateResponses();
					for(String response : newResponses) {
						//conn.send();
					}
				}
			};
		}
		
		public void parseMessage(String message) {
			String formatted;
			JsonParser parser = new JsonParser();
			JsonObject json = parser.parse(message).getAsJsonObject();
			System.out.println(json);
			if(json.has("text")) {
				
			}
		}
	}
	//========== End Connection Class ============================
	
	//=========== Message Class ===============================
	/*
	 * Stores all neccessary information about a message, so it can be stored in history.
	 * 		Saves the time sent, the text of the message, and the username/color of the user.
	 * 		If I get the main server working, this may not be neccessary.
	 */
	public class Message{
		long time;
		String text;
		String user;
		String color;
		
		public Message(long time, String text, String user, String color) {
			this.time = time;
			this.text = text;
			this.user = user;
			this.color = color;
		}
	}
	//========== End Message Class ============================
}
