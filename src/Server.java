import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
//import java.net.DatagramPacket;
//import java.net.DatagramSocket;
//import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
//import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Queue;
import java.util.LinkedList;

public class Server {
	
  static Map<String,Integer> inventory = new HashMap<String,Integer>(); // <productname, quantity>
  static Map<String,ArrayList<String>> orders = new HashMap<String,ArrayList<String>>(); // <username, orderid productname quantity...>
  static int orderId = 1;
  Queue<Integer> commandQ = new LinkedList<Integer> ();
  
  public static void main (String[] args) throws IOException {
	  
	Scanner sc = new Scanner(System.in);
	int myID = sc.nextInt();
	int numServer = sc.nextInt();
	String inventoryPath = sc.next();
	ArrayList<String> IPList = new ArrayList<String> ();
	ArrayList<Integer> portList = new ArrayList<Integer> ();
	

    for (int i = 0; i < numServer; i++) {
        //parses inputs to get the ips and ports of servers
        String str = sc.next();
        String[] strSplit = str.trim().split(":");
        IPList.add(strSplit[0]);
        portList.add(Integer.parseInt(strSplit[1]));
        System.out.println("address for server " + i + ": " + str);
    }
    
    sc.close();
    
    int tcpPort = portList.get(myID - 1);
    
    // Parse the inventory file
    try(BufferedReader file = new BufferedReader(new FileReader(inventoryPath))) {
        String line = file.readLine();

        while (line != null && !line.trim().equals("")) {
        	String[] split_input = line.split(" ");
        	inventory.put(split_input[0], (int) Double.parseDouble(split_input[1]));
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
    
    // ThreadPool
    ExecutorService threadPool = null;
    try {
        threadPool = Executors.newCachedThreadPool();
    } catch (Exception e) { 
        System.err.println(e); 
        System.exit(-1);
    }
    
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
				 inventory.put(line[1], inventory.get(line[1]) + (int) Double.parseDouble(line[2]));
				 return "Order " + inputId + " is canceled";
			  }
		  }
	  }
	  
	  return inputId + " not found, no such order";
  }
  
  public static String processSearch(String userName) {
	  ArrayList<String> userHistory = orders.get(userName);
	  if (userHistory == null || userHistory.size() == 0) {
		  return "No order found for " + userName + "\n";
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
  
  //Can't make these methods static
  
  //TODO: Add to commandQ
  public synchronized void addToList(int threadHash) {
	  return;
  }
  
  //TODO: Check if thread is next in local commandQ
  public synchronized boolean isNext() {
	  return true;
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
	    while(true) {
	    	//Synchronized add to queue
	    	//if(begin of queue)
	    	try {
		        String clientCommand = inFromClient.readLine();
		        if (!(clientCommand == null || clientCommand.equals("null"))) {
		        	//add to List
		        }
		        //Here, also ask for conditions using Lamport's
		        //if(isNext()) {
		        	//singleRun(String command);
		        //}
		    } catch (Exception e) {
	    		e.printStackTrace();
	    	} finally {
	    		try {
					mySocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
	    	}
	    }

    }
    
    //Change this so it doesn't act until acks received
    private synchronized void singleRun(String command) {
    	try {
	        String[] tokens = command.trim().split(" ");
	        
	        if (tokens[0].equals("purchase") && tokens.length == 4) {
	            String response = Server.processPurchase(tokens[1], tokens[2], (int) Double.parseDouble(tokens[3]));
	            outToClient.writeBytes(response.trim() + "\n\n");
	        } else if (tokens[0].equals("cancel") && tokens.length == 2) {
	            String response = Server.processCancel((int) Double.parseDouble(tokens[1]));
	            outToClient.writeBytes(response.trim() + "\n\n");
	        } else if (tokens[0].equals("search") && tokens.length == 2) {
	            String response = Server.processSearch(tokens[1]);
	            outToClient.writeBytes(response.trim() + "\n\n");
	        } else if (tokens[0].equals("list") && tokens.length == 1) {
	            String response = Server.list();
	            outToClient.writeBytes(response.trim() + "\n\n");
	        } else {
	        	//System.out.println("ERROR: No such command");
	          	outToClient.writeBytes("ERROR: No such command\n\n");
	        }
	        outToClient.flush();
    	} catch (Exception e) {
    		e.printStackTrace();
    	} finally {
    		try {
				mySocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    }
}


