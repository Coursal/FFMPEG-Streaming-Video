package ffmpeg_streaming_video;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JButton;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ClassNotFoundException;
import javax.swing.JTextPane;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.swing.JTextField;
import java.awt.ScrollPane;

public class StreamingServer implements Runnable
{
	private static ServerSocket server;
	
	private Thread server_thread;
	
	private JFrame frame;
	private JLabel lblServerLog;
	private JTextArea server_log;
	JButton btnStartServer;
	JButton btnStopServer;
	
	static Logger log = LogManager.getLogger(StreamingServer.class);
	
	void start_server() throws IOException, ClassNotFoundException, InterruptedException
	{
		server = new ServerSocket(5000);
		
		File[] videos_list = new File("videos/").listFiles();	// list with the filenames in the /videos/ directory
		
		while(true)
		{
			server_log.append("Listening for requests...\n");
			
			Socket socket = server.accept();
			ObjectInputStream input_stream = new ObjectInputStream(socket.getInputStream());
			ObjectOutputStream output_stream = new ObjectOutputStream(socket.getOutputStream());
			
			// receive the request (bitrate and format) from client and
			ArrayList<String> received_request = (ArrayList<String>) input_stream.readObject();
			Float selected_bitrate =  Float.parseFloat(received_request.get(0));
			String selected_format = received_request.get(1);
			
			server_log.append("Received request for " + selected_bitrate + " bitrate and " + selected_format + " format, ");
			
			ArrayList<String> available_videos = new ArrayList<String>(); // list of videos available to stream
			
			// for each video in the /videos/ directory...
			for(File video : videos_list)
			{
				String current_video = video.getName();
				
				// take the last 3 characters of the filenames (e.g. .avi, .mp4, .mkv) 
				// and compare them to the received format to filter out the rest of the videos at another format
				if(current_video.substring(current_video.length()-3).equals(selected_format))
				{
					// split the current video filename at the '-' character (e.g. 'Test-0.2Mbps.avi' to 'Test' and '0.2Mbps.avi')
					// to filter out the 3 characters that signify the bitrate of the video
					// and turn it to a floating point number
					String[] splitted_video_name = new String(current_video).split("-");
					float current_video_bitrate = Float.parseFloat(splitted_video_name[1].substring(0, 3));
					
					// if the current video is in equal or less bitrate, it can be streamed
					// so add it to the list of  videos available to stream
					if(selected_bitrate >= current_video_bitrate)
						available_videos.add(current_video);
				}	
			}
			
			// send the list of videos that are available to stream based on the specified format and bitrate
			output_stream.writeObject(available_videos);
			
			// receive the selected video and the protocol specification to stream with
			ArrayList<String> stream_specs = (ArrayList<String>) input_stream.readObject();
			String selected_video = stream_specs.get(0);
			String selected_protocol = stream_specs.get(1);
			
			server_log.append("using the " + selected_protocol + " protocol to stream \'" + selected_video + "\'\n\n");
			
			String videos_dir_full_path = "C:\\Users\\N\\eclipse-workspace\\FFMPEG-Streaming-Video\\videos\\";
			
			// create a process through the command line to run the ffplay program
			// to play the incoming streamed video with the appropriate arguments
			ArrayList<String> command_line_args = new ArrayList<String>();
			
			command_line_args.add("cmd");
			command_line_args.add("/c");
			command_line_args.add("C:\\ffmpeg\\bin\\ffmpeg.exe");
		
			if(selected_protocol.equals("UDP"))
			{
				command_line_args.add("-re");
				command_line_args.add("-i");
				command_line_args.add(videos_dir_full_path + selected_video);
				command_line_args.add("-f");
				command_line_args.add("mpegts");
				command_line_args.add("udp://127.0.0.1:6000");
			}
			else if(selected_protocol.equals("TCP"))
			{
				command_line_args.add("-i");
				command_line_args.add(videos_dir_full_path + selected_video);
				command_line_args.add("-f");
				command_line_args.add("mpegts");
				command_line_args.add("tcp://127.0.0.1:5100?listen");
			}
			else
			{
				command_line_args.add("-re");
				command_line_args.add("-i");
				command_line_args.add(videos_dir_full_path + selected_video);
				command_line_args.add("-an");
				command_line_args.add("-c:v");
				command_line_args.add("copy");
				command_line_args.add("-f");
				command_line_args.add("rtp");
				command_line_args.add("-sdp_file");
				command_line_args.add("C:\\Users\\N\\eclipse-workspace\\FFMPEG-Streaming-Video\\video.sdp");
				command_line_args.add("rtp://127.0.0.1:5004?rtcpport=5008");
			}
			
			ProcessBuilder process_builder = new ProcessBuilder(command_line_args);
			Process streamer_host = process_builder.start();
			
			output_stream.close();
			input_stream.close();
			socket.close();
		}
	}
	
	@Override
	public void run() 
	{
		try 
		{
			start_server();
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	public StreamingServer() 
	{
		frame = new JFrame("Streaming Server");
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		frame.setResizable(false);
		
		// Server Log Text with its scrollbar
		lblServerLog = new JLabel("Server log:");
		lblServerLog.setBounds(20, 11, 66, 14);
		frame.getContentPane().add(lblServerLog);
		
		server_log = new JTextArea();
		server_log.setBounds(10, 36, 424, 181);
		server_log.setWrapStyleWord(true);
		server_log.setLineWrap(true);
		frame.getContentPane().add(server_log);
		server_log.setColumns(10);
		
		JScrollPane scrollPane = new JScrollPane(server_log);
		scrollPane.setBounds(20, 36, 404, 181);
		frame.getContentPane().add(scrollPane);
		//--------------------------------------
		
		// Start Server Button
		btnStartServer = new JButton("Start Server");
		btnStartServer.setBounds(20, 228, 113, 23);
		frame.getContentPane().add(btnStartServer);
		//--------------------------------------
		
		// Stop Server Button
		btnStopServer = new JButton("Stop Server");
		btnStopServer.setBounds(311, 228, 113, 23);
		frame.getContentPane().add(btnStopServer);
		//--------------------------------------
		
		btnStopServer.setEnabled(false); // gray out the stop server button on startup

		server_thread = new Thread(this);	// create a thread for the server to run
		
		// implementation of the listener after the Start Server button is pressed
		btnStartServer.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent event)
		    {
		    	log.debug("\'Start Server\' button has been pressed");
		  
	    		btnStartServer.setEnabled(false);
	    		btnStopServer.setEnabled(true);

	    		server_thread.start();
		    }
		});
		
		// implementation of the listener after the Stop Server button is pressed
		btnStopServer.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent event)
		    {
		    	log.debug("\'Stop Server\' button has been pressed");
		  
		    	System.exit(0);	// close the GUI window of the server
			
		    }
		});
	}
	
	public static void main(String[] args) 
	{
		EventQueue.invokeLater(new Runnable() 
		{
			public void run() 
			{
				try 
				{
					StreamingServer window = new StreamingServer();
					window.frame.setVisible(true);
				} 
				catch(Exception e) 
				{
					e.printStackTrace();
				}
			}
		});
	}
}
