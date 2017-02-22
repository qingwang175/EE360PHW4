import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
	
  static Map<String,Integer> inventory = new HashMap<String,Integer>(); // <productname, quantity>
  static Map<String,ArrayList<String>> orders = new HashMap<String,ArrayList<String>>(); // <username, orderid productname quantity...>
  static int orderId = 1;
  
  public static void main (String[] args) throws IOException {
    int tcpPort;
    int udpPort;
    if (args.length != 3) {
      System.out.println("ERROR: Provide 3 arguments");
      System.out.println("\t(1) <tcpPort>: the port number for TCP connection");
      System.out.println("\t(2) <udpPort>: the port number for UDP connection");
      System.out.println("\t(3) <file>: the file of inventory");

      System.exit(-1);
    }
    
    tcpPort = Integer.parseInt(args[0]);
    udpPort = Integer.parseInt(args[1]);
    String fileName = args[2];

    // Parse the inventory file
    try(BufferedReader file = new BufferedReader(new FileReader(fileName))) {
        String line = file.readLine();

        while (line != null && !line.trim().equals("")) {
        	String[] split_input = line.split(" ");
        	inventory.put(split_input[0], Integer.parseInt(split_input[1]));
            line = file.readLine();
        }
    } catch (Exception e) {
    	e.printStackTrace();
    	System.out.println("ERROR: Parsing File");
    	System.exit(-1);
    }
    
    // TCP
    ServerSocket tcpSocket = new ServerSocket(tcpPort);
    Socket connectionSocket;
    
    // UDP
	DatagramSocket udpSocket = new DatagramSocket(udpPort);
    
    // ThreadPool
    ExecutorService threadPool = null;
    try {
        threadPool = Executors.newCachedThreadPool();
    } catch (Exception e) { 
        System.err.println(e); 
        System.exit(-1);
    }
    
    UDPThread ut = new UDPThread(udpSocket);
    threadPool.submit(ut);
    
    // Get incoming connection and create thread
    while(true) {
		connectionSocket = tcpSocket.accept();
    	ServerThread st = new ServerThread(connectionSocket);
    	threadPool.submit(st);
    }
    //threadPool.shutdown();
    
  }
  
  
  public static String processPurchase(String userName,String productName, int quantity) {
	  Integer pQuantity = inventory.get(productName);
	  if (pQuantity == null) {
		  return "Not Available - We do not sell this product";
	  } else if (quantity > pQuantity) {
		  return "Not Available - Not enough items" ;
	  }
	  
	  inventory.put(productName, pQuantity-quantity);
	  int currentId = orderId++;
	  
	  ArrayList<String> userData = orders.get(userName);
	  if (userData == null) {
		  ArrayList<String> anotherOne = new ArrayList<String>();
		  anotherOne.add(currentId + " " + productName + " " + quantity);
		  orders.put(userName, anotherOne);
	  } else {
		  userData.add(currentId + " " + productName + " " + quantity);
	  }
	  return "Your order has been placed, " + currentId + " " + userName + " " + productName + " " + quantity ;
  }
  
  public static String processCancel(int inputId) {
	  for (Map.Entry<String, ArrayList<String>> allOrders : orders.entrySet()) {
		  
		  ArrayList<String> current = allOrders.getValue();
		  for (int i = 0; i < current.size(); i ++) {
			  String[] line = current.get(i).split(" ");
			  String testId = line[0];
			  if (testId.equals(inputId+"")) {
				 current.remove(i);
				 inventory.put(line[1], inventory.get(line[1]) + Integer.parseInt(line[2]));
				 return "Order " + inputId + " is canceled";
			  }
		  }
	  }
	  
	  return inputId + " not found, no such order";
  }
  
  public static String processSearch(String userName) {
	  ArrayList<String> userHistory = orders.get(userName);
	  if (userHistory == null || userHistory.size() == 0) {
		  return "No order found for " + userName;
	  } else {
		  StringBuilder orderhist = new StringBuilder();
		  for (String line : userHistory) {
			  String[] split = line.split(" ");
			  orderhist.append(split[0] + ", " + split[1] + ", " + split[2] + "\n");
		  }
		  return orderhist.toString();
	  }
  }
  
  public static String list() {
	  StringBuilder inventoryList = new StringBuilder();
	  for (Map.Entry<String, Integer> item : inventory.entrySet()) {
		  inventoryList.append(item.getKey() + " " + item.getValue() + "\n");
	  }
	  return inventoryList.toString();
  }
}


class ServerThread implements Runnable {
	Socket mySocket;
	BufferedReader inFromClient;
	DataOutputStream outToClient;
	
	public ServerThread(Socket s) throws IOException {
		mySocket = s;
		inFromClient = new BufferedReader(new InputStreamReader(s.getInputStream()));
		outToClient = new DataOutputStream(s.getOutputStream());
	}
	
    public void run() {
    	try {
	    	while(true) {
	        	String clientCommand = inFromClient.readLine();
	        	if (clientCommand == null || clientCommand.equals("null")) { break; }
	        	String[] tokens = clientCommand.trim().split(" ");
	        	
	        	if (tokens[0].equals("setmode")) {
	            }
	            else if (tokens[0].equals("purchase")) {
	            	String response = Server.processPurchase(tokens[1], tokens[2], Integer.parseInt(tokens[3]));
	            	outToClient.writeBytes(response + "\n\n");
	            } else if (tokens[0].equals("cancel")) {
	            	String response = Server.processCancel(Integer.parseInt(tokens[1]));
	            	outToClient.writeBytes(response + "\n\n");
	            } else if (tokens[0].equals("search")) {
	            	String response = Server.processSearch(tokens[1]);
	            	outToClient.writeBytes(response + "\n\n");
	            } else if (tokens[0].equals("list")) {
	            	String response = Server.list();
	            	outToClient.writeBytes(response + "\n");
	            } else {
	          	  System.out.println("ERROR: No such command");
	          	  outToClient.writeBytes("ERROR: No such command\n\n");
	            }
	    	}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}

    }
}


class UDPThread implements Runnable {
	DatagramSocket udpSocket;
	
	public UDPThread(DatagramSocket ds) {
		udpSocket = ds;
	}
	
    public void run() {
    	try {
	    	while(true) {
				byte[] receiveData = new byte[1024];
				byte[] sendData = new byte[1024];
	    		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				udpSocket.receive(receivePacket);
				
				InetAddress IPAddress = receivePacket.getAddress();
				int port = receivePacket.getPort();
				String clientCommand = new String( receivePacket.getData());
	    		
	        	String[] tokens = clientCommand.trim().split(" ");
	        	
	        	if (tokens[0].equals("setmode")) {
	            }
	            else if (tokens[0].equals("purchase")) {
	            	String response = Server.processPurchase(tokens[1], tokens[2], Integer.parseInt(tokens[3])) + "\n\n";
	            	sendData = response.getBytes();
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
					udpSocket.send(sendPacket);
	            } else if (tokens[0].equals("cancel")) {
	            	String response = Server.processCancel(Integer.parseInt(tokens[1])) + "\n";
	            	sendData = response.getBytes();
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
					udpSocket.send(sendPacket);
	            } else if (tokens[0].equals("search")) {
	            	String response = Server.processSearch(tokens[1]) + "\n";
	            	sendData = response.getBytes();
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
					udpSocket.send(sendPacket);
	            } else if (tokens[0].equals("list")) {
	            	String response = Server.list();
	            	sendData = response.getBytes();
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
					udpSocket.send(sendPacket);
	            } else {
	          	  System.out.println("ERROR: No such command");
	          	  //outToClient.writeBytes("ERROR: No such command\n\n");
	            }
	    	}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}

    }
}
