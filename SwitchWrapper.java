package iowrapper;

import java.sql.SQLException;
import java.util.ArrayList;

import json.JSONException;

import nnode.NodeSwitch;

public class SwitchWrapper {
	NodeSwitch nSwitch;
	public SwitchWrapper() throws SQLException{
		nSwitch = new NodeSwitch();
		messages = new ArrayList<>();
	}
	
	ArrayList<String> messages;
	public void addMessage(String message) {
		messages.add(message);
	}
	
	public void consume(int consumes){
		for(int i = 0; i < consumes; i++)
			consume();
	}
	
	public String consume(){
		if(messages.isEmpty())
			return null;
		try {
			String message = messages.remove(0);
			nSwitch.handleMessage(message);
			return message;
		} catch (JSONException | SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void consumeAll() {
		while(!messages.isEmpty()){
			consume();
		}
		
	}
}
