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
	
	public WebServer(InetSocketAddress address) {
		super(address);
	}

	@Override
	public void onClose(WebSocket conn, int arg1, String arg2, boolean arg3) {
		Connection connection = searchConnection(conn);
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
				
				connection.username = username;  //Sets username
				connection.usercolor = pickColor();  //Sets usercolor
				connection.server.login(username, password);  //Log this user in.
				//Send response to client with chosen color.
				JsonObject json = new JsonObject();
				json.addProperty("type", "color");
				json.addProperty("data", connection.usercolor);
				//connection.conn.send(json.toString());
				
				System.out.println(new Date() + " :: User " + username + " has"
						+ " joined the chat using color " + connection.usercolor);
			}
			else { //All other messages, case 2.
				if(isWholesome(message)) {  //Message is wholesome
					long time = System.currentTimeMillis() /1000;
					String username = connection.username;
					String usercolor = connection.usercolor;
					Message newMessage = new Message(  //Creates the new Message object
							time, message, username, usercolor);
					messages.add(newMessage);  //Saves the new message to the ArrayList.
					
					//Send message to everyone connected
					for(int i=0; i < connections.size(); i++) {
						connections.get(i).conn.send(jsonMessage(newMessage).toString());
					}
					
					System.out.println(new Date() + " :: User " + connection.username
							+ " has sent a message: \"" + message + "\"");
				}
				else {  //Message isn't wholesome
					JsonObject json = new JsonObject();
					json.addProperty("type", "wholesome");
					json.addProperty("data", false);
					connection.conn.send(json.toString());
					System.out.println(new Date() + " :: User " + connection.username
							+ " has sent an unwholesome message: \""
							+ message + "\"");
				}
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
	
	
	/*This will only be used if I have to keep this version as a standalone,
	 *  otherwise the main server will be doing all the message checking.
	 *  For now, I have it just returning true every time.
	 */
	public boolean isWholesome(String message) {
		final double MAX_ANGER = 0.7;
		final double MAX_DISGUST = 0.7;
		final double MAX_FEAR = 0.7;
		final double MAX_JOY = 1.0;
		final double MAX_SADNESS = 0.7;
		
		JsonObject jObject = toneAnalyzer(message);
		Gson gson = new Gson();
		
		return true;
	}
	
	/*
	 * Helper function for isWholesome(), this makes the watson call and
	 *   returns the response as JSON.
	 */
	public JsonObject toneAnalyzer(String message) {
		final String VERSION_DATE = "2016-05-19";
		String watsonUser = "";
		String watsonPass = "";
		String userFile = "/home/chris/Documents/watson/login/user";
		String passFile = "/home/chris/Documents/watson/login/pass";
		
		try {
			FileReader fReader = new FileReader(userFile);
			BufferedReader bReader = new BufferedReader(fReader);
			watsonUser = bReader.readLine();
			
			fReader = new FileReader(passFile);
			bReader = new BufferedReader(fReader);
			watsonPass = bReader.readLine();
			bReader.close();
			fReader.close();
		}
		catch(FileNotFoundException f) {
			System.out.println(new Date() + " :: Error: Watson user/pass file not found.");
			return null;
		}
		catch(IOException e){
			System.out.println(new Date() + " :: Error: Unable to open Watson user/pass file");
			return null;
		}
		
		ToneAnalyzer service = new ToneAnalyzer(VERSION_DATE,
				watsonUser, watsonPass);
		
		ToneOptions toneOptions = new ToneOptions.Builder().html(message).build();
		ToneAnalysis tone = service.tone(toneOptions).execute();
		
		String asString = tone.toString();
		JsonObject asJson = (JsonObject) new JsonParser().parse(asString);
		
		return asJson;
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
		
		public Connection(WebSocket conn, MainController server) {
			this.conn = conn;
			this.server = server;
			
			//Start the new thread to get responses
			/*
			task = () -> {
				server.getResponse();
			};
			thread = new Thread(task);
			thread.start();
			*/
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
