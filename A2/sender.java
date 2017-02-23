

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Queue;

public class sender {
    String host_addr;
    int data_port;
    int ack_port;
    String filename;
    DatagramSocket send_socket;
    DatagramSocket receive_socket;
    InetAddress IPAddress;
    int seqnum ;
    int wind_size;
    //packet buffer for  all packets that no ack-ed yet
    Queue<packet> queue = new LinkedList<packet>();
    Timer timer;
    File seqLog;
    File ackLog;
    FileWriter writerSeq;
    FileWriter writerAck;
    //constructor
    sender(String args[]) throws Exception{
        //check args length
        if(args.length!=4){
            throw new Exception("Expected 4 Arguments");
        }
        host_addr = args[0];
        data_port = Integer.parseInt(args[1]);
        ack_port = Integer.parseInt(args[2]);
        filename = args[3];
        //check file existence
        if(!new File(filename).exists()){
            throw new Exception(filename + "does not exits");
        }
        seqnum = 0;
        wind_size = 10;
        //200ms interrput
        timer = new Timer(200, new ActionListener() {
            @Override
synchronized public void actionPerformed(ActionEvent e) {
                try {
                    //resend all packets in buffer;
                    for (packet pk : queue) {
			 send_packet(pk);
                    }
                    //restart timer
                    timer.restart();
                }catch (Exception ex){
                    System.out.println(ex);
                }
            }
        });
        //used to write to file
        seqLog = new File("seqnum.log");
        ackLog = new File("ack.log");
        seqLog.createNewFile();
        ackLog.createNewFile();
        writerSeq = new FileWriter(seqLog);
        writerAck = new FileWriter(ackLog);
    }


    //setup connection sockets
    public  void Connection_setup() throws Exception{
        send_socket = new DatagramSocket();
        receive_socket = new DatagramSocket(ack_port);
        IPAddress  = InetAddress.getByName(host_addr);
    }
 

   //send packet
    public  void send_packet(packet sendPacket) throws  Exception{
        //record the seqnum to send
        //System.out.println("sending "+ sendPacket.getSeqNum());
        writerSeq.write(sendPacket.getSeqNum()+"\n");
        writerSeq.flush();
        //send packet
	    byte[] sendData = sendPacket.getUDPdata();
        DatagramPacket sendDatagramPacket = new DatagramPacket(sendData, sendData.length, IPAddress, data_port);
        send_socket.send(sendDatagramPacket);
    }
 


   //receive packets
synchronized public packet receive_packet() throws Exception{
        //receive packet
        byte[] ack = new byte[1024];
        DatagramPacket receiveDatagramPacket = new DatagramPacket(ack, ack.length);
        receive_socket.receive(receiveDatagramPacket);
        packet pk =  packet.parseUDPdata(receiveDatagramPacket.getData());
        //record received packet seqnum;
        writerAck.write(pk.getSeqNum()+"\n");
   	writerAck.flush();
        //check duplicated ack
	boolean dup = true;
	for(packet p : queue){
		if(pk.getSeqNum() == p.getSeqNum())dup = false;
	}
	    //if this is duplicated ack, ignore it
	if(dup)return pk;
	    //Not duplicated ack! remove everything packets prior to it
	while (pk.getSeqNum() != queue.element().getSeqNum() ){
            queue.remove();
	    timer.restart();
        }
        //remove the ack-ed packet
        queue.remove();
	    timer.restart();
        return pk;
    }

    public static void main(String args[]) {
	sender sd = null;
        try {
            //create sender and setup connection sockets
            sd = new sender(args);
            sd.Connection_setup();
            //file reader obj
            BufferedReader file = new BufferedReader(new FileReader(sd.filename));
            while (true){
                //check the available spot in the buffer
                if(sd.queue.size() <= sd.wind_size) {
                    //data buffer
            	    char[] data = new char[500];
                    //read full data buffer from the file;
                    int len = file.read(data,0,500);
                    //if all char in the file have been read, break;
                    if( len < 0 && sd.queue.size() == 0 ) break;
		    //System.out.println("packet size "+len +" queue size  "+sd.queue.size());
                    //make a new packet
		    if( len != -1 ){
	           	packet sendPacket = packet.createPacket(sd.seqnum, new String(data));
			//put into packet buffer and send it
        	    	sd.queue.add(sendPacket);
                    	sd.send_packet(sendPacket);
                    //update seqnum
                    	sd.seqnum+=1;
		    }
                    //setup timer
                    if(!sd.timer.isRunning()){
                        sd.timer.start();
                    }
                    //receive packet
                    packet receivePacket = sd.receive_packet();
                    //empty  packet buffer stops timer
                    if(sd.queue.size() == 0 ){
                        sd.timer.stop();
                    }
                }
            }
            //send EOT
            packet sendPacket = packet.createEOT(sd.seqnum);
            sd.send_packet(sendPacket);
            //close sockets,and file writer
            sd.writerAck.close();
            sd.writerSeq.close();
        }catch (Exception e){
            System.out.println(e);
        }finally{
		if(sd!=null){
            sd.receive_socket.close();
            sd.send_socket.close();

		}
	}
    }
}
