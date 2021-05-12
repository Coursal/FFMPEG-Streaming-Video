package ffmpeg_streaming_video;

import java.awt.EventQueue;
import javax.swing.JFrame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import javax.swing.JLabel;
import javax.swing.JScrollPane;

public class StreamingDirector 
{
	private JFrame frame;
	static Logger log = LogManager.getLogger(StreamingDirector.class);
	
	File[] generate_videos()
	{
		// setting up the relative paths of the directories to fetch and store videos
		String input_dir = "raw_videos/";
		String output_dir = "videos/";
		
		FFmpeg ffmpeg = null;
		FFprobe ffprobe = null;
		
		try 
		{
			log.debug("Initialising FFMpegClient");
			ffmpeg = new FFmpeg("/usr/bin/ffmpeg");
			ffprobe = new FFprobe("/usr/bin/ffprobe");
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		// array of raw videos from the /raw_videos directory
		File[] raw_videos = new File(input_dir).listFiles();
		
		// array of video formats to generate
		String[] video_formats = {".avi", ".mp4", ".mkv"};
		
		// hashmap of the video bitrates in both float (Mbps) and long (bps) data types
		HashMap<Float, Long> video_bitrates = new HashMap<>();
		video_bitrates.put(0.2f, 200000L);		//0.2Mbps
		video_bitrates.put(0.5f, 500000L);		//0.5Mbps
		video_bitrates.put(1.0f, 1000000L);		//1Mbps
		video_bitrates.put(3.0f, 3000000L);		//3Mbps
		
		// scanning each raw video...
		for(File video : raw_videos)
		{
			System.out.println("Raw video found: " + video.getName());
			String current_video_name = video.getName().split("/.")[0].replaceAll(" ", "_");

			// for each video format...
			for(String format : video_formats)
			{
				// and for each bitrate...
				for (Float bitrate : video_bitrates.keySet()) 
				{
					System.out.println("Converting '" + current_video_name + "' to '" + format + "' with " + bitrate + "Mbps bitrate");
					
					// generate the video file 
					// with the appropriate bitrate tag at the title
					// and the appropriate video format extension
					log.debug("Creating the transcoding");
					FFmpegBuilder builder = (new FFmpegBuilder()
								.setInput(input_dir + video.getName())
								.addOutput(output_dir + current_video_name + "-" + bitrate + "Mbps" + format))
								.setVideoBitRate(video_bitrates.get(bitrate))
								.done();
					
					log.debug("Creating the executor");
					FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
					
					log.debug("Starting the transcoding");
					// Run a one-pass encode
					executor.createJob(builder).run();
					
					log.debug("Transcoding finished");	
				}
			}
		}	
		
		// deleting all videos in the /raw_videos directory
		for(File video : raw_videos)
		{
			System.out.println("Deleting '" + video.getName() + "'...");
			video.delete();
		}
		
		System.out.println("Done!");
		
		//File[] output_videos = new File(output_dir).listFiles();
		
		return new File(output_dir).listFiles();
	}
	
	public StreamingDirector() 
	{
		frame = new JFrame("Streaming Director");
		frame.setBounds(100, 100, 450, 450);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		frame.setResizable(false);
		
		// Input Videos List Layout
		DefaultListModel<String> list_model = new DefaultListModel<>();
		String input_dir = "raw_videos/";
		
		File[] raw_videos = new File(input_dir).listFiles();
		
		for(File video : raw_videos)
		  list_model.addElement(video.getName());
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 58, 200, 300);
		frame.getContentPane().add(scrollPane);
		
		JList input_list = new JList(list_model);
		scrollPane.setViewportView(input_list);
		
		JScrollPane scrollPane_1 = new JScrollPane();
		scrollPane_1.setBounds(234, 58, 200, 300);
		frame.getContentPane().add(scrollPane_1);
		//--------------------------------------
		
		// Output Videos List Layout
		JList output_list = new JList();
		scrollPane_1.setViewportView(output_list);
		//--------------------------------------
		
		// Text labels
		JLabel lblFilesInrawvideos = new JLabel("/raw_videos directory");
		lblFilesInrawvideos.setBounds(10, 43, 150, 14);
		frame.getContentPane().add(lblFilesInrawvideos);
		
		JLabel lblvideosDirectory = new JLabel("/videos directory");
		lblvideosDirectory.setBounds(234, 43, 150, 14);
		frame.getContentPane().add(lblvideosDirectory);
		
		JLabel lblInfolabel = new JLabel("Press 'Start' to create all the versions to /videos.");
		lblInfolabel.setBounds(10, 11, 414, 14);
		frame.getContentPane().add(lblInfolabel);
		//--------------------------------------
		
		JButton btnStart = new JButton("Start");
		btnStart.setBounds(116, 369, 94, 32);
		frame.getContentPane().add(btnStart);
		
		btnStart.addActionListener(e -> {
			File[] output_videos = generate_videos();

			// if the /raw_videos/ directory was empty (so the generate_videos() function returned an empty array)
			// show a pop up dialog message about it
			if(output_videos.length == 0)
			{
				JOptionPane.showMessageDialog(frame, "/raw_videos/ directory seems empty.", "Exiting...", JOptionPane.ERROR_MESSAGE);
				System.exit(0);
			}
			else
			{
				DefaultListModel<String> updated_input_list_model = new DefaultListModel<>();
				updated_input_list_model.clear();
				input_list.setModel(updated_input_list_model);

				// update the output video list from /videos
				DefaultListModel<String> updated_output_list_model = new DefaultListModel<>();

				for(File video : output_videos)
					updated_output_list_model.addElement(video.getName());

				output_list.setModel(updated_output_list_model);

				// disable the 'Start' button after pressing it
				btnStart.setEnabled(false);
			}
		});
		
		JButton btnExit = new JButton("Exit");
		btnExit.setBounds(233, 369, 94, 32);
		frame.getContentPane().add(btnExit);
		
		btnExit.addActionListener(e -> System.exit(0));
	}
	
	public static void main(String[] args) 
	{
		EventQueue.invokeLater(() -> {
			try
			{
				StreamingDirector window = new StreamingDirector();
				window.frame.setVisible(true);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		});
	}
}
