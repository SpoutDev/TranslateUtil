package org.spout.translationutil;

import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class MainWindow {

	private JFrame frame;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public MainWindow() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		GridLayout grid = new GridLayout(2, 2);
		frame.getContentPane().setLayout(grid);
		
		JLabel labelClasses = new JLabel("Classes");
		labelClasses.setHorizontalAlignment(SwingConstants.CENTER);
		JList listClasses = new JList();
		
		JLabel labelStrings = new JLabel("Strings");
		labelStrings.setHorizontalAlignment(SwingConstants.CENTER);
		JList listStrings = new JList();
		
		frame.getContentPane().add(labelClasses);
		frame.getContentPane().add(labelStrings);
		frame.getContentPane().add(listClasses);
		frame.getContentPane().add(listStrings);
	}

}
