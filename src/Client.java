import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
  public static void main (String[] args) throws UnknownHostException, IOException {
    String hostAddress;
    int tcpPort;
    int udpPort;

    if (args.length != 3) {
      System.out.println("ERROR: Provide 3 arguments");
      System.out.println("\t(1) <hostAddress>: the address of the server");
      System.out.println("\t(2) <tcpPort>: the port number for TCP connection");
      System.out.println("\t(3) <udpPort>: the port number for UDP connection");
      System.exit(-1);
    }

    hostAddress = args[0];
    tcpPort = Integer.parseInt(args[1]);
    udpPort = Integer.parseInt(args[2]);
    boolean mode = true;
    
    // TCP Config
    Socket tcpSocket = new Socket(hostAddress, tcpPort);
    DataOutputStream outToServer = new DataOutputStream(tcpSocket.getOutputStream());
    BufferedReader inFromServer = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
    System.out.println("Connected to server using TCP");

    
    //UDP Config
    DatagramSocket udpSocket = null;
	byte[] sendData = new byte[1024];
	byte[] receiveData = new byte[1024];
    
    Scanner sc = new Scanner(System.in);

    while(sc.hasNextLine()) {
    	if (!mode) {
        	sendData = new byte[1024];
        	receiveData = new byte[1024];
    	}
        
    	String cmd = sc.nextLine();
        String[] tokens = cmd.split(" ");

        if (tokens[0].equals("setmode")) {
        	if (!tokens[1].equals("U")) {
          		tcpSocket = new Socket(hostAddress, tcpPort);
          		outToServer = new DataOutputStream(tcpSocket.getOutputStream());
          		inFromServer = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
          		mode = true;
          		System.out.println("Connected to server using TCP");	
        	} else {
        		mode = false;
        		udpSocket = new DatagramSocket();
        		System.out.println("Connected to server using UDP");
        	}
        } else if (tokens[0].equals("purchase") || tokens[0].equals("cancel") || tokens[0].equals("search") || tokens[0].equals("list")) {
        	if (mode) {
            	outToServer.writeBytes(cmd + '\n');
            	
            	String response = null;
            	while ((response = inFromServer.readLine()) != null) {
            	    if (response.isEmpty() || response == null) {
            	        break;
            	    }
            	    System.out.println(response);
            	}
            	System.out.println();
        	} else {
        		// UDP
        		sendData = cmd.getBytes();
        		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(hostAddress), udpPort);
        		udpSocket.send(sendPacket);
        		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        		udpSocket.receive(receivePacket);
        		String response = new String(receivePacket.getData());
        		System.out.println(response);
        	}

        } else {
        	System.out.println("ERROR: No such command");
        }
    
    }
    /*
    while(sc.hasNextLine()) {
      String cmd = sc.nextLine();
      String[] tokens = cmd.split(" ");
      if (clientSocket != null) {
    	  outToServer = new DataOutputStream(clientSocket.getOutputStream());
      }
      
      if (tokens[0].equals("setmode")) {
    	  clientSocket = new Socket("localhost", 6789);
    	  outToServer = new DataOutputStream(clientSocket.getOutputStream());
    	  System.out.println("Connected to server using TCP");
    	  //outToServer.writeBytes(sentence + '\n');
      }
      else if (tokens[0].equals("purchase")) {
        // TODO: send appropriate command to the server and display the
        // appropriate responses form the server
      } else if (tokens[0].equals("cancel")) {
        // TODO: send appropriate command to the server and display the
        // appropriate responses form the server
      } else if (tokens[0].equals("search")) {
        // TODO: send appropriate command to the server and display the
        // appropriate responses form the server
      } else if (tokens[0].equals("list")) {
        // TODO: send appropriate command to the server and display the
        // appropriate responses form the server
      } else {
    	  outToServer.writeBytes(cmd + '\n');
    	  System.out.println("ERROR: No such command");
      }
    }
    */
    
    
  }
}
