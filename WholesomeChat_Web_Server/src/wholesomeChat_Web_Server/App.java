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
import java.util.Date;
import java.util.Scanner;

import org.java_websocket.server.WebSocketServer;

public class App {

	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		/*
		int port = 1337;
		WebSocketServer server = new WebServer(new InetSocketAddress(port));
		System.out.println(new Date() + " :: Starting server on port " + port);
		server.run();
		System.out.println("goodbye");
		*/
		
		///*
		MainController server = new MainController();
		 
		
		Scanner userIn = new Scanner(System.in);
		
		Runnable task = () -> {
			server.getResponse();
		};
		Thread thread = new Thread(task);
		thread.start();
		
		server.login("cmhill", "password");
		userIn.nextLine();
		server.sendMessage("Hello World!");
		userIn.nextLine();
		System.out.println("goodbye!");
		server.sendMessage("/quit");
		thread.stop();
		server.close();
		//*/
	}

}
