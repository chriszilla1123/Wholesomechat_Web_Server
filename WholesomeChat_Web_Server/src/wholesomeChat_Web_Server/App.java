/*
 * WholesomeChat Web Server
 * Intermediary between main server and client version
 * Accesses the main server via Java Socket, and forwards
 * 		to the client via WebSockets.
 * 
 * This class acts as a router between the main server (MainController)
 * 		and the web server (WebServer)
 */
package wholesomeChat_Web_Server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.java_websocket.server.WebSocketServer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class App {

	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		///*
		int port = 1337;
		WebSocketServer server = new WebServer(new InetSocketAddress(port));
		System.out.println(new Date() + " :: Starting server on port " + port);
		server.run();
		System.out.println("goodbye");
		//*/
		
		/*
		MainController server = new MainController();
		 
		
		Scanner userIn = new Scanner(System.in);
		
		Runnable task = () -> {
			server.responseListener();
		};
		Thread thread = new Thread(task);
		thread.start();
		
		String username = "";
		boolean loggedIn = false;
		ArrayList<String> response;
		server.login("cmhill", "password");
		while(!loggedIn) {
			if(server.hasNewResponse()) {
				System.out.print('.');
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				response = server.updateResponses();
				if(response.size() != 0) {
					JsonParser parser = new JsonParser();
					JsonObject json = parser.parse(response.get(0)).getAsJsonObject();
					if(json.has("nick")) {
						username = json.get("nick").getAsString();
						System.out.println("Found nick");
						loggedIn = true;
					}
				}
			}
		}
		System.out.println(username);
		
		userIn.nextLine();
		JsonObject json = new JsonObject();
		json.addProperty("intent", "message");
		json.addProperty("text", "/quit");
		server.send(json.toString());
		userIn.nextLine();
		thread.stop();
		server.close();
		*/
	}

}
