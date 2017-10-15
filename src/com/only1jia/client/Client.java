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
        startButton = new JButton("连接");  
        stopButton = new JButton("断开");  
        sendButton = new JButton("发送");  
        northPanel = new JPanel();  
        northPanel.setLayout(new GridLayout(1, 7));  
        northPanel.add(new JLabel("端口"));  
        northPanel.add(port);
        northPanel.add(new JLabel("读取文件序号"));  
        northPanel.add(index); 
        northPanel.add(startButton);  
        northPanel.add(stopButton);  
        northPanel.setBorder(new TitledBorder("连接信息"));  
  
        rightScroll = new JScrollPane(textArea);  
        rightScroll.setBorder(new TitledBorder("消息显示区"));  
        southPanel = new JPanel(new BorderLayout());  
        southPanel.add(textField, "Center");  
        southPanel.add(sendButton, "East");  
        southPanel.setBorder(new TitledBorder("写消息"));  
  
  
  
        frame = new JFrame("客户机");   
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
	        		// 找到配置文件，启动路由器
	        		File file = new File("./client" + i + ".txt");
	        		if (!file.exists() || !file.isFile())
	        			throw new RuntimeException("找不到路由信息文件");
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
	    
	    // 关闭窗口时事件  
        frame.addWindowListener(new WindowAdapter() {  
            public void windowClosing(WindowEvent e) {
                System.exit(0);// 退出程序  
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
				sysout("指令格式：" + sendFormat);
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
				sysout("指令格式：" + setFormat);
				break;
			}
			tokens = tokens[1].split(" ");
			if (tokens.length != 2) {
				sysout("指令格式：" + setFormat);
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
				sysout("指令格式出错");
			break;
		}
	}
	public static void main(String[] args) throws Exception {
		// 从参数读取配置文件根目录与文件名后缀
		@SuppressWarnings("unused")
		Client client = new Client();
		
	}

	public static void sysout(String msg) {
		textArea.append(msg + "\n");
	}
}
