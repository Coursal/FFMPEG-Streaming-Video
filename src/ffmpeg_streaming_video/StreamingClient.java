package ffmpeg_streaming_video;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;

public class StreamingClient
{
	private Socket socket;
	private ObjectOutputStream output_stream;
	private ObjectInputStream input_stream;
	
	private static JFrame frame;
	
	private JLabel lblBitrate;
	private JComboBox bitrate;
	private JLabel lblFormat;
	private JComboBox format;
	private JButton btnConnect;

	private JLabel lblVideo;
	private JComboBox video;
	private JLabel lblProtocol;
	private JComboBox protocol;
	private JButton btnStream;
	
	static Logger log = LogManager.getLogger(StreamingClient.class);
	
	void send_request_to_server(ObjectOutputStream output_stream, ObjectInputStream input_stream) throws Exception
	{
		ArrayList<String> request = new ArrayList<>();
		request.add(bitrate.getSelectedItem().toString());
		request.add(format.getSelectedItem().toString());
		
		log.debug("Sending request to server: " + bitrate.getSelectedItem().toString() + " bitrate and " + format.getSelectedItem().toString() + " format");
		output_stream.writeObject(request);	// send the request with the selected bitrate and format
		
		ArrayList<String> available_videos = (ArrayList<String>) input_stream.readObject(); // receive a list of videos based on the request
		log.debug("Received list of available videos to stream");
		
		// fill the dropdown menu of the videos on the gui with the contents of this list
		for(String current_video : available_videos)
			video.addItem(current_video);	
		
		log.debug("Sent the list to the GUI");
	}
	
	void send_specs_to_server(ObjectOutputStream output_stream) throws Exception
	{
		ArrayList<String> stream_specs = new ArrayList<>();
		stream_specs.add(video.getSelectedItem().toString());
		stream_specs.add(protocol.getSelectedItem().toString());
	
		log.debug("Sending stream specs to server: " + video.getSelectedItem().toString() + " using " + protocol.getSelectedItem().toString());
		output_stream.writeObject(stream_specs);
		
		// create a process through the command line to run the ffplay program
		// to play the incoming streamed video with the appropriate arguments
		ArrayList<String> command_line_args = new ArrayList<>();

		command_line_args.add("ffplay");
		
		if(protocol.getSelectedItem().toString().equals("UDP"))
			command_line_args.add("udp://127.0.0.1:6000");
		else if(protocol.getSelectedItem().toString().equals("TCP"))
			command_line_args.add("tcp://127.0.0.1:5100");
		else	// for RTP/UDP, mention the session description protocol file to the server 
		{
			command_line_args.add("-protocol_whitelist");
			command_line_args.add("file,rtp,udp");
			command_line_args.add("-i");
			command_line_args.add("video.sdp");
		}
		
		ProcessBuilder process_builder = new ProcessBuilder(command_line_args);
		Process streamer_client = process_builder.start();
		
		log.debug("Process to play the incoming stream started");
	}
	
	public StreamingClient() throws IOException
	{
		// setting up the socket address and port
		// and the output and input streams to send and receive data to the server
		socket = new Socket("127.0.0.1", 5000);
		output_stream = new ObjectOutputStream(socket.getOutputStream());
		input_stream = new ObjectInputStream(socket.getInputStream());
		
		frame = new JFrame("Streaming Client");
		frame.setBounds(250, 250, 250, 280);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		frame.setResizable(false);	
	
		// Bitrate Dropdown Menu (with its choices)
		lblBitrate = new JLabel("Bitrate");
		lblBitrate.setBounds(10, 11, 100, 14);
		frame.getContentPane().add(lblBitrate);
		
		bitrate = new JComboBox();
		bitrate.setBounds(10, 36, 83, 20);
		frame.getContentPane().add(bitrate);
		bitrate.addItem("0.2");
		bitrate.addItem("0.5");
		bitrate.addItem("1.0");
		bitrate.addItem("3.0");
		//--------------------------------------
		
		// Format Dropdown Menu (with its choices)
		lblFormat = new JLabel("Format");
		lblFormat.setBounds(191, 11, 43, 14);
		frame.getContentPane().add(lblFormat);
		
		format = new JComboBox();
		format.setBounds(145, 36, 89, 20);
		frame.getContentPane().add(format);
		format.addItem("avi");
		format.addItem("mp4");
		format.addItem("mkv");
		//--------------------------------------
		
		// Connect Button
		btnConnect = new JButton("Connect");
		btnConnect.setBounds(82, 67, 89, 23);
		frame.getContentPane().add(btnConnect);
		//--------------------------------------
	
		// Video Dropdown Menu (with its choices added later, sent by the server)
		lblVideo = new JLabel("Video");
		lblVideo.setBounds(10, 101, 100, 14);
		frame.getContentPane().add(lblVideo);
		
		video = new JComboBox();
		video.setBounds(10, 126, 224, 20);
		frame.getContentPane().add(video);
		//--------------------------------------
		
		// Protocol Dropdown Menu (with its choices)
		lblProtocol = new JLabel("Protocol");
		lblProtocol.setBounds(10, 162, 51, 14);
		frame.getContentPane().add(lblProtocol);
		
		protocol = new JComboBox();
		protocol.setBounds(10, 187, 89, 20);
		frame.getContentPane().add(protocol);
		protocol.addItem("UDP");
		protocol.addItem("TCP");
		protocol.addItem("RTP/UDP");
		//--------------------------------------
		
		// Stream Button
		btnStream = new JButton("Stream");
		btnStream.setBounds(82, 218, 89, 23);
		frame.getContentPane().add(btnStream);	
		//--------------------------------------
		
		// gray out the components that are to be used after the first response of the server
		video.setEnabled(false);
		protocol.setEnabled(false);
		btnStream.setEnabled(false);
		
		// implementation of the listener after the Connect button is pressed
		btnConnect.addActionListener(event -> {
			log.debug("Connect button has been pressed");

			try
			{
				// send the request (bitrate and format) to the server
				// and receive a list of videos based on the request
				send_request_to_server(output_stream, input_stream);

				// gray out the components already used for the first response of the server
				bitrate.setEnabled(false);
				format.setEnabled(false);
				btnConnect.setEnabled(false);

				// enable the components to be used for the second response of the server
				video.setEnabled(true);
				protocol.setEnabled(true);
				btnStream.setEnabled(true);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		});

		// implementation of the listener after the Stream button is pressed
		btnStream.addActionListener(event -> {
			log.debug("'Stream' button has been pressed");

			try
			{
				// send the specifications (selected video and protocol) to the server
				// and stream the incoming video through ffplay
				send_specs_to_server(output_stream);

				// close the socket and streams from the client when all communications are done
				output_stream.close();
				input_stream.close();
				socket.close();

				System.exit(0);	// close the GUI window of the client
			}
				catch (Exception e)
				{
				e.printStackTrace();
			}
		});
	}
	
	public static void main(String[] args) 
	{
		EventQueue.invokeLater(() -> {
			try
			{
				StreamingClient window = new StreamingClient();
				window.frame.setVisible(true);
			}
			catch(ConnectException e)
			{
				JOptionPane.showMessageDialog(frame, "Connection refused. Start the server and try again.", "Exiting...", JOptionPane.ERROR_MESSAGE);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}

		});
	}
}

