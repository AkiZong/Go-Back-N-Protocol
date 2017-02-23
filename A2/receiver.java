import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class receiver {
    String hostname;
    int ack_port;
    int data_port;
    String filename;
    DatagramSocket send_socket;
    DatagramSocket receive_socket;
    InetAddress IPAddress;
    int seqnum;
    boolean terminate;
    File output;
    File arrLog ;
    FileWriter writerOut;
    FileWriter writeArr;
    boolean connected;
    receiver(String argv[]) throws  Exception{
        if(argv.length!=4){
            throw new Exception("Expected 4 Arguments");
        }
        hostname  = argv[0];
        ack_port  = Integer.parseInt(argv[1]);
        data_port = Integer.parseInt(argv[2]);
        filename  = argv[3];
        terminate = false;
        connected = false;
        //expected seqnum
	seqnum = 0;
        //used to write to a file
        output = new File(filename);
        arrLog = new File("arrival.log");
        writerOut = new FileWriter(output);
        writeArr = new FileWriter(arrLog);
        output.createNewFile();
        arrLog.createNewFile();
    }
    //Setup socket for connection
    public  void Connection_setup() throws Exception{
        send_socket = new DatagramSocket();
        receive_socket = new DatagramSocket(data_port);
    }
    //check if duplicated packet
    public boolean check(int exp,int rec){
   	if(exp - rec <= 10){
		if(rec < exp){
			return true;
		}
	}else{
		if(rec > exp){
			return true;
		}
	}
	return false;
    }
    //used to send packet
    public  void send_packet(packet sendPacket) throws  Exception{
//	System.out.println("sending ack "+sendPacket.getSeqNum());
        byte[] sendData = sendPacket.getUDPdata();
        DatagramPacket sendDatagramPacket = new DatagramPacket(sendData, sendData.length, IPAddress, ack_port);
        send_socket.send(sendDatagramPacket);
    }


    //to receive_packet
    public packet receive_packet() throws Exception{
        //receive
        byte[] data = new byte[1024];//buffer
        DatagramPacket receiveDatagramPacket = new DatagramPacket(data, data.length);
        receive_socket.receive(receiveDatagramPacket);
        packet pk =  packet.parseUDPdata(receiveDatagramPacket.getData());
        //EOT then ready to terminate
        if(pk.getType() == 2){
            terminate = true;
        }
        //used to send back
        IPAddress = receiveDatagramPacket.getAddress();
        return pk;
    }


    public static void main(String argv[]) throws Exception {
       	 receiver rc = null;
	 try{
            //create receiver and setup connection
            rc = new receiver(argv);
            rc.Connection_setup();

            while (true) {
                //receive packets
                packet receivePacket = rc.receive_packet();
		//record seqnum from received packet
                rc.writeArr.write(receivePacket.getSeqNum()+"\n");
                rc.writeArr.flush();
                //if EOT then terminates
                if(rc.terminate) {
                    break;
                }
                //fisrt time connection
                if( !rc.connected ) {
                    //if first packet is lost do nothing
                    if(receivePacket.getSeqNum() !=0 ){
			continue;
		    }else{
			 rc.connected = true;
		    }
                } 
		//create packets
		packet sendPacket;
		
		if(rc.check(rc.seqnum,receivePacket.getSeqNum())){
        	        sendPacket = packet.createACK(receivePacket.getSeqNum());
                	rc.send_packet(sendPacket);
		}else{
                	sendPacket = packet.createACK(rc.seqnum);
                	rc.send_packet(sendPacket);
		}
                //send packet back
		//update variables
                //check whether the received seqnum == expected
		if(receivePacket.getSeqNum() == rc.seqnum){
                    //update expected seqnum
			rc.seqnum = (rc.seqnum +1)%32;
                    //store the file
                	rc.writerOut.write(new String(receivePacket.getData()));
                	rc.writerOut.flush();
                } 
            }
            //close sockets,file writer
            rc.writeArr.close();
            rc.writerOut.close();
        }catch (Exception e){
            System.out.println(e);
        }finally{
		if(rc!=null){
            rc.send_socket.close();
            rc.receive_socket.close();
	
		}
	}

    }

}
