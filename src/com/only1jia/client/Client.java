package com.only1jia.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import com.only1jia.model.Node;
import com.only1jia.router.Router;



public class Client {
	
	private JFrame frame;  
	private JButton startButton;
	private static JTextArea textArea = new JTextArea();
	private JTextField textField;
	public static JTextField port = new JTextField("23300");
	private JButton stopButton;
	private JButton sendButton;
	private JPanel northPanel;
	private JScrollPane rightScroll;
	private JPanel southPanel;
	private JTextField index;
	
	private Router router;

	static final String sendFormat = "send [address]:[port] [text]";
	static final String setFormat = "modify [neibour address]:[port] [positive integer]|-1";
	
	public Client() { 
        textArea.setEditable(false);  
        textArea.setForeground(Color.blue);  
        textField = new JTextField();  
        port.setEditable(false);
        index = new JTextField("0");  
        startButton = new JButton("����");  
        stopButton = new JButton("�Ͽ�");  
        sendButton = new JButton("����");  
        northPanel = new JPanel();  
        northPanel.setLayout(new GridLayout(1, 7));  
        northPanel.add(new JLabel("�˿�"));  
        northPanel.add(port);
        northPanel.add(new JLabel("��ȡ�ļ����"));  
        northPanel.add(index); 
        northPanel.add(startButton);  
        northPanel.add(stopButton);  
        northPanel.setBorder(new TitledBorder("������Ϣ"));  
  
        rightScroll = new JScrollPane(textArea);  
        rightScroll.setBorder(new TitledBorder("��Ϣ��ʾ��"));  
        southPanel = new JPanel(new BorderLayout());  
        southPanel.add(textField, "Center");  
        southPanel.add(sendButton, "East");  
        southPanel.setBorder(new TitledBorder("д��Ϣ"));  
  
  
  
        frame = new JFrame("�ͻ���");   
        frame.setLayout(new BorderLayout());  
        frame.add(northPanel, "North");  
        frame.add(rightScroll, "Center");  
        frame.add(southPanel, "South");  
        frame.setSize(600, 400);  
        int screen_width = Toolkit.getDefaultToolkit().getScreenSize().width;  
        int screen_height = Toolkit.getDefaultToolkit().getScreenSize().height;  
        frame.setLocation((screen_width - frame.getWidth()) / 2,  
                (screen_height - frame.getHeight()) / 2);  
        frame.setVisible(true);
        
		
		
	    startButton.addActionListener(new ActionListener() {  
	            public void actionPerformed(ActionEvent e) {
	                int i = Integer.parseInt(index.getText().trim());
	        		System.out.println(i);
	        		// �ҵ������ļ�������·����
	        		File file = new File("./client" + i + ".txt");
	        		if (!file.exists() || !file.isFile())
	        			throw new RuntimeException("�Ҳ���·����Ϣ�ļ�");
	        		try {
	        			router = new Router(file);
	        		} catch (Exception e2) {
	        			// TODO Auto-generated catch block
	        			e2.printStackTrace();
	        		}
	            	router.start();
	            }
	        });
	    stopButton.addActionListener(new ActionListener() {  
            public void actionPerformed(ActionEvent e) {
            	if (router != null)	router.shutdown();
            }
        });
	    
	    sendButton.addActionListener(new ActionListener() {  
            public void actionPerformed(ActionEvent e) {
            	try {
					sendMessage();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
            }
        });
	    
	    // �رմ���ʱ�¼�  
        frame.addWindowListener(new WindowAdapter() {  
            public void windowClosing(WindowEvent e) {
                System.exit(0);// �˳�����  
            }  
        });  
	}
	
	
	public void sendMessage() throws Exception {
		String message = textField.getText().trim();
		String[] tokens = message.split(" ", 2);
		String msg = null;
		switch (tokens[0]) {
		case "send":
			if (tokens.length < 2) {
				sysout("ָ���ʽ��" + sendFormat);
				break;
			}
			tokens = tokens[1].split(" ", 2);
			String text = null;
			if (tokens.length == 2)
				text = tokens[1];
			else
				text = "";
			Node node = new Node(tokens[0]);
			msg = router.send(node, text);
			sysout(msg);
			break;

		case "modify":
			if (tokens.length < 2) {
				sysout("ָ���ʽ��" + setFormat);
				break;
			}
			tokens = tokens[1].split(" ");
			if (tokens.length != 2) {
				sysout("ָ���ʽ��" + setFormat);
				break;
			}
			Node neibour = new Node(tokens[0]);
			int dis = Integer.parseInt(tokens[1]);
			msg = router.change(neibour, dis);
			sysout(msg);
			break;
			
		case "shutdown":
			router.shutdown();
			break;

		default:
			if (message.length() != 0)
				sysout("ָ���ʽ����");
			break;
		}
	}
	public static void main(String[] args) throws Exception {
		// �Ӳ�����ȡ�����ļ���Ŀ¼���ļ�����׺
		@SuppressWarnings("unused")
		Client client = new Client();
		
	}

	public static void sysout(String msg) {
		textArea.append(msg + "\n");
	}
}
