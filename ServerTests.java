package iowrapper;

import java.sql.SQLException;
import java.util.ArrayList;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.client.WebSocketClient.WebListener;

import android.GUIController;
import android.NActivity;
import android.NarratorService;
import android.alerts.PlayerPopUp;
import android.alerts.TeamEditor;
import android.app.Environment;
import android.app.FragmentManager;
import android.day.ActivityDay;
import android.day.ChatItem;
import android.day.DayScreenController;
import android.os.Bundle;
import android.parse.Server;
import android.screens.ActivityHome;
import android.screens.SimpleGestureFilter;
import android.setup.ActivityCreateGame;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import json.JSONException;
import json.JSONObject;
import junit.framework.TestCase;
import nnode.Instance;
import nnode.NodeSwitch.SwitchListener;
import shared.logic.Narrator;
import shared.logic.Player;
import shared.logic.PlayerList;
import shared.logic.support.Constants;
import shared.logic.support.Random;
import shared.logic.support.RoleTemplate;
import shared.logic.support.rules.Rules;
import shared.logic.templates.BasicRoles;
import shared.roles.Arsonist;
import shared.roles.Doctor;
import shared.roles.RandomMember;
import shared.roles.Role;
import voss.narrator.R;

public class ServerTests extends TestCase{

	
	private static SwitchWrapper nSwitch;
	private void startSwitch(IOWrapper wrap){
		try {
			nSwitch = new SwitchWrapper();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		pseudoConnect(wrap);
	}
	
	public void runTest() throws Throwable{
		Server.testing = true;
		super.runTest();
	}
	
	public void tearDown() throws Exception{
		Server.testing = true;
		super.tearDown();
	}
	
	private void pseudoConnect(IOWrapper wrap){
		WebSocketClient.wl = new WebListener(){
			public void onMessageReceive(String message) {
				nSwitch.addMessage(message);
			}
		};
		nSwitch.nSwitch.switchListener = new SwitchListener(){
			public void onSwitchMessage(String s){
				try {
					JSONObject jo = new JSONObject(s);
					if(jo.has("server") && jo.getBoolean("server"))
						return;
					String receiver = jo.getString("name");
					for(Environment e: wrap.envs){
						NActivity na = (NActivity) e.getActive();
						String iName = na.ns.server.GetCurrentUserName();
						if(iName.equals(receiver)){
							na.ns.mWebSocketClient.onMessage(s);
							break;
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		};
	}
	
	public void testNeutralBenign(){
		IOWrapper wrap = new IOWrapper();
		startSwitch(wrap);
		
		long seed = new Random().nextLong();
		//long seed = Long.parseLong("6278177073870124829");
		System.out.println("Testing Neutral Benign(Instance)\t" + seed);

		ArrayList<Interacter> interacters = init(2);
		Host h1 = (Host) interacters.get(0);
		h1.addRole(RandomMember.NeutralBenignRandom(), "Randoms");
		
		assertEquals(1, nSwitch.nSwitch.instances.get(0).n.getAllRoles().size());
	}
	
	public void testJoinGame(){
		IOWrapper wrap = new IOWrapper();
		startSwitch(wrap);
		Host h1 = wrap.initHost();
		ActivityHome ah = (ActivityHome) h1.getEnvironment().getActive();
		assertFalse(ah.ns.server.IsLoggedIn());
		
		h1.login(0);
		assertTrue(ah.ns.server.IsLoggedIn());
		
		h1.clickButton(R.id.home_join);
		nSwitch.consume();
		nSwitch.consume();
		assertEquals(nSwitch.nSwitch.instances.get(0).host.getName(), h1.getName());
	}
	
	public void testAssassinScreen(){
		ArrayList<Interacter> interacters = init(2);
		Host h1 = (Host) interacters.get(0);
		
		h1.addRole(BasicRoles.Assassin(), "Mafia");
		h1.addRole(BasicRoles.Assassin(), "Mafia");
		h1.addRole(BasicRoles.Assassin(), "Yakuza");
		
		h1.dayStart();
		Narrator n = nSwitch.nSwitch.instances.get(0).n;
		h1.clickStart();
		assertEquals(3, n.getAllRoles().size());
		assertTrue(n.isInProgress());
		
		ActivityDay ad = h1.getController().dScreen;
		assertTrue(h1.getController().swipeAbilityPanel("Assassinate"));
		h1.getController().dScreen.onSwipe(SimpleGestureFilter.SWIPE_LEFT);
		
		assertEquals(View.GONE, ad.button.getVisibility());

		assertTrue(h1.getController().swipeAbilityPanel("Assassinate"));
		assertEquals(View.VISIBLE, ad.button.getVisibility());
		
		ad.actionLV.click(0);
		nSwitch.consumeAll();
		assertTrue(n.isInProgress());
		assertTrue(ad.actionLV.getCheckedItemPosition() == 0);
		assertTrue(n.getLivePlayers().size() == 3);
		
		h1.clickButton(R.id.day_button);
		nSwitch.consumeAll();
		assertTrue(n.getLivePlayers().size() == 2);
	}
	
	public void testHostGame(){
		IOWrapper wrap = new IOWrapper();
		startSwitch(wrap);
		Host h1 = wrap.initHost();
		ActivityHome ah = (ActivityHome) h1.getEnvironment().getActive();
		assertFalse(ah.ns.server.IsLoggedIn());
		
		h1.login(0);
		assertTrue(ah.ns.server.IsLoggedIn());
		
		h1.clickButton(R.id.home_host);
		nSwitch.consume();
		nSwitch.consume();
		assertEquals(nSwitch.nSwitch.instances.get(0).host.getName(), h1.getName());
		assertEquals(h1.getEnvironment().getActive().getClass(), ActivityCreateGame.class);
	}
	
	private ArrayList<Interacter> init(int clients){
		ArrayList<Interacter> interacters = new ArrayList<>();
		IOWrapper wrap = new IOWrapper();
		startSwitch(wrap);
		Host h1 = wrap.initHost();
		h1.login(0);
		h1.setSwitch(nSwitch);
		h1.hostGame();
		
		
		interacters.add(h1);
		
		for(int i = 0; i < clients; i++){
			Client c = wrap.initClient();
			c.login(i + 1);
			c.setSwitch(nSwitch);
			c.joinGame();
			interacters.add(c);
		}
		nSwitch.consumeAll();
		return interacters;
	}
	
	public void testPlayerListChange(){
		IOWrapper wrap = new IOWrapper();
		startSwitch(wrap);
		Host h1 = wrap.initHost();
		h1.login(0);
		h1.setSwitch(nSwitch);
		h1.hostGame();
		
		ActivityCreateGame ach = h1.getActivityCreateGame();
		h1.clickButton(R.id.roles_show_Players);
		PlayerPopUp pPop = ach.pPop;
		assertEquals(1, pPop.lv.size());
		
		Client c = wrap.initClient();
		c.login(1);
		c.setSwitch(nSwitch);
		c.clickButton(R.id.home_join);
		nSwitch.consume(3);
		
		assertEquals(2, pPop.lv.size());
	}
	
	public void testEditRoles(){
		ArrayList<Interacter> interacters = init(2);
		Host h1 = (Host) interacters.get(0);

		h1.addTeam("Bro", "#5500F9");
		h1.clickFaction("Bro");
		h1.clickButton(R.id.create_editAlliesButton);
		
		TeamEditor tEditor = (TeamEditor) h1.getActivityCreateGame().getFragmentManager().getFragment(new Bundle(), ActivityCreateGame.EDIT_TEAM_PROMPT);
		ListView nonEmptyAdapter, emptyAdapter;
		if(tEditor.l1.size() != 0){
			nonEmptyAdapter = tEditor.l1;
			emptyAdapter = tEditor.l2;
		}else{
			emptyAdapter = tEditor.l2;
			nonEmptyAdapter = tEditor.l1;
		}
		nonEmptyAdapter.click(0);
		nSwitch.consume();
		
		assertEquals(1, emptyAdapter.size());
		
	}
	
	public void testButton(){
		ArrayList<Interacter> interacters = init(2);
		Host h = (Host) interacters.get(0);
		
		h.addRole(BasicRoles.Mayor(), "Town");
		h.addRole(BasicRoles.Arsonist(), "Neutrals");
		h.addRole(BasicRoles.Citizen(), "Town");
		
		Instance curInstance = nSwitch.nSwitch.instances.get(0);
		curInstance.n.getRules().setBool(Rules.DAY_START, Narrator.DAY_START);
		curInstance.n.setSeed(0);
		
		h.clickStart();
		
		assertTrue(curInstance.n.isInProgress());
		
		Player host = curInstance.n.getPlayerByName("voss");
		assertTrue(host.is(Arsonist.class));
		assertTrue(host.hasDayAction(Role.MAIN_ABILITY));
		assertTrue(curInstance.n.isDay());
		
		ActivityDay ad = (ActivityDay) h.getActivity();
		GUIController gCon = h.getController();
		gCon.infoPanelClick();
		assertTrue(ad.manager.getCurrentPlayer() != null);
		assertEquals(View.VISIBLE, ad.button.getVisibility());
		
		for(Player p: curInstance.n.getAllPlayers()){
			if(curInstance.n.isDay())
				p.voteSkip();
		}
		
		assertTrue(curInstance.n.isNight());
		gCon.actionPanelClick();
		
		assertEquals(View.VISIBLE, ad.button.getVisibility());
		
		//text changes, server doesn't quite now yet that host ended night
		assertEquals(DayScreenController.SKIP_NIGHT_TEXT, ad.button.getText().toString());
		h.clickButton(R.id.day_button);
		assertEquals(DayScreenController.CANCEL_SKIP_NIGHT_TEXT, ad.button.getText().toString());
		
		//server knows ended night, and tells client as well
		nSwitch.consume();
		assertTrue(ad.ns.endedNight(null));
		assertTrue(host.endedNight());
		assertEquals(DayScreenController.CANCEL_SKIP_NIGHT_TEXT, ad.button.getText().toString());
		
		//text changes, server doesn't quite now yet that host changed his mind
		h.clickButton(R.id.day_button);
		assertTrue(ad.ns.endedNight(null));
		assertEquals(DayScreenController.SKIP_NIGHT_TEXT, ad.button.getText().toString());
		
		//now server knows
		nSwitch.consume();
		assertFalse(ad.ns.endedNight(null));
		assertFalse(host.endedNight());
		assertEquals(DayScreenController.SKIP_NIGHT_TEXT, ad.button.getText().toString());
	}
	
	public void testChat(){
		ArrayList<Interacter> interacters = init(2);
		Host h = (Host) interacters.get(0);
		
		h.addRole(BasicRoles.Mayor(),    "Town");
		h.addRole(BasicRoles.Arsonist(), "Neutrals");
		h.addRole(BasicRoles.Citizen(),  "Town");
		
		h.dayStart();
		h.clickStart();

		ActivityDay ad = h.getController().dScreen;
		GUIController.selectScreen(ad, "Voss");
		h.getController().actionPanelClick();
		ad.actionLV.click(0);
		nSwitch.consumeAll();
		
		ad.actionLV.click(0);
		nSwitch.consumeAll();
	}
	
	public void testLeaveGame(){
		ArrayList<Interacter> interacters = init(2);
		Client client = (Client) interacters.get(1);
		
		client.clickButton(R.id.roles_leaveGame);
		assertEquals(client.getActivity().getClass(), ActivityHome.class);
		nSwitch.consume();
		assertEquals(2, nSwitch.nSwitch.instances.get(0).n.getPlayerCount());
		assertEquals(client.getActivity().getClass(), ActivityHome.class);
		
	}
	
	public void testPreGameChat(){
		ArrayList<Interacter> interacters = init(2);
		Host h1 = (Host) interacters.get(0);
		
		//tests the switch to chat
		ActivityCreateGame ach = h1.getActivityCreateGame();
		
		if(ach.chatVisible())
			h1.clickButton(R.id.create_toChat);
		
		assertFalse(ach.chatVisible());
		h1.clickButton(R.id.create_toChat);
		assertTrue(ach.chatVisible());
		h1.clickButton(R.id.create_toChat);
		assertFalse(ach.chatVisible());
		
		h1.chat("hi");
		EditText hChat = (EditText) h1.getEnvironment().getActive().findViewById(R.id.create_chatET);
		assertEquals(0, hChat.getText().toString().length());
		
		assertFalse(h1.getActivityCreateGame().chatAdapter.chatReference.isEmpty());
		/*TextView chatTV = (TextView) h1.getEnvironment().getActive().findViewById(R.id.create_chatTV);
		String chatContents = chatTV.getText().toString();
		assertFalse("".equals(chatContents));*/
		
		//3 lines should be shown.  2 that people have joined. 1 that host said something.
		assertEquals(3, h1.getActivityCreateGame().chatAdapter.chatReference.size());
		
		
		Client c2 = (Client) interacters.get(2);
		c2.chat("sup");
		
		for(ChatItem s: c2.getActivityCreateGame().chatAdapter.chatReference){
			if(s.text.contains("sup"))
				return;
		}
		fail();
	}
	
	public void testTrio(){
		IOWrapper wrap = new IOWrapper();
		startSwitch(wrap);
		Host h1 = wrap.initHost();
		h1.login(0);
		
		Client c1 = wrap.initClient();
		ActivityHome c1_ah = (ActivityHome) c1.getEnvironment().getActive();
		assertFalse(c1_ah.ns.server.IsLoggedIn());
		c1.login(1);
		assertTrue(c1_ah.ns.server.IsLoggedIn());
		
		Client c2 = wrap.initClient();
		ActivityHome c2_ah = (ActivityHome) c2.getEnvironment().getActive();
		assertFalse(c2_ah.ns.server.IsLoggedIn());
		c2.login(2);
		assertTrue(c2_ah.ns.server.IsLoggedIn());
		
		h1.clickButton(R.id.home_host);
		nSwitch.consume(4); //3 greets, 1 host request
		assertEquals(ActivityCreateGame.class, h1.getEnvironment().getActive().getClass());
		c1.clickButton(R.id.home_join);
		c2.clickButton(R.id.home_join);
		
		nSwitch.consumeAll();
		
		assertEquals(3, nSwitch.nSwitch.instances.get(0).n.getPlayerCount());
		
		ArrayList<Interacter> interacters = new ArrayList<>();
		interacters.add(h1);
		interacters.add(c1);
		interacters.add(c2);
		
		FragmentManager fm = null;
		for(Interacter i: interacters){
			i.clickButton(R.id.roles_show_Players);
			fm = i.getEnvironment().getActive().getFragmentManager();
			PlayerPopUp pops = (PlayerPopUp) fm.getFragment(null, "playerlist");
			assertEquals(3, pops.players.length());
			pops.dismiss();
		}
		
		h1.addRole(BasicRoles.Agent(), "Mafia");
		nSwitch.consume();
		
		for(Interacter i: interacters){
			assertEquals(1, ((ActivityCreateGame) i.getEnvironment().getActive()).rolesListLV.size());
		}
		ActivityCreateGame hac = (ActivityCreateGame) h1.getEnvironment().getActive();
		ActivityCreateGame cac1 = (ActivityCreateGame) c1.getEnvironment().getActive();
		
		//removes role
		hac.rolesListLV.click(0);
		nSwitch.consume();
		
		for(Interacter i: interacters){
			assertEquals(0, ((ActivityCreateGame) i.getEnvironment().getActive()).rolesListLV.size());
		}
		
		PlayerPopUp popUp = (PlayerPopUp) cac1.getFragmentManager().getFragment(null, ActivityCreateGame.PLAYER_POP_UP);
		assertEquals(3, popUp.players.length());
		
		c1.clickFaction("Town");
		c1.clickListing(((ActivityCreateGame) c1.getEnvironment().getActive()).rolesLV, "Escort");
		CheckBox cBox = cac1.getManager().screenController.cBox[0];
		
		h1.clickFaction("Town");
		h1.clickListing(hac.rolesLV, "Escort");
		nSwitch.consume();
		
		CheckBox hBox = hac.getManager().screenController.cBox[0];
		boolean prevValue = hBox.isChecked();
		hBox.toggle();
		assertEquals(!prevValue, hBox.isChecked());
		nSwitch.consume();
		
		assertEquals(!prevValue, cBox.isChecked());
		assertEquals(!prevValue, hBox.isChecked());
		
		h1.addTeam("Bro", "#5500F9");
		nSwitch.consume();
		
		for(Interacter i: interacters){
			assertEquals(6, ((ActivityCreateGame) i.getEnvironment().getActive()).cataLV.size());
		}
		
		h1.addRole(BasicRoles.Citizen(), "Town");
		h1.addRole(BasicRoles.Witch(), "Neutrals");
		nSwitch.consume();
		nSwitch.consume();
		
		h1.clickStart();
		nSwitch.consume();
		
		assertEquals(h1.getEnvironment().getActive().getClass(), ActivityDay.class);
	}
	
	public void testDeleteButton(){
		ArrayList<Interacter> interacters = init(2);
		Host h = (Host) interacters.get(0);
		
		h.addTeam("Bro", "#5500F9");
		h.clickFaction("Bro");
		h.clickButton(R.id.create_deleteTeamButton);
		nSwitch.consume();
		
		for(Interacter i: interacters){
			assertEquals(5, ((ActivityCreateGame) i.getEnvironment().getActive()).cataLV.size());
		}
	}
	
	public void testReconnect(){
		Host h = (Host) init(0).get(0);
		NarratorService.WAIT_TIME = 0;
		WebSocketClient socket = h.getActivity().ns.mWebSocketClient;
		socket.onError(new NullPointerException());
		
		try{
			Thread.sleep(500);	
		}catch(InterruptedException e){}
		
		assertTrue(socket != h.getActivity().ns.mWebSocketClient);
	}
	
	public void testVotePersistance(){
		ArrayList<Interacter> interacters = init(2);
		Host h = (Host) interacters.get(0);
		
		assertTrue(nSwitch.messages.isEmpty());
		
		h.addRole(BasicRoles.Mayor(), "Town");
		h.addRole(BasicRoles.Arsonist(), "Neutrals");
		h.addRole(BasicRoles.Citizen(), "Town");
		
		Instance curInstance = nSwitch.nSwitch.instances.get(0);
		curInstance.n.getRules().setBool(Rules.DAY_START, Narrator.DAY_START);
		curInstance.n.setSeed(0);
		
		h.clickStart();
		
		assertTrue(curInstance.n.isInProgress());
		
		Player host = curInstance.n.getPlayerByName("voss");
		assertTrue(host.is(Arsonist.class));
		assertTrue(host.hasDayAction(Role.MAIN_ABILITY));
		assertTrue(curInstance.n.isDay());
		
		ActivityDay ad = (ActivityDay) h.getActivity();
		GUIController gCon = h.getController();
		gCon.actionPanelClick();
		
		ad.actionLV.click(0);
		String clickedText = ad.getCheckedPlayers(0).get(0);
		assertTrue(clickedText.contains(ad.actionList.get(0)));
		
		nSwitch.consume();
		clickedText = ad.getCheckedPlayers(0).get(0);
		assertTrue(clickedText.contains(ad.actionList.get(0)));
		
		
		
		ad.actionLV.click(1);
		clickedText = ad.getCheckedPlayers(0).get(0);
		assertTrue(clickedText.contains(ad.actionList.get(0)));
		
		nSwitch.consume();
		clickedText = ad.getCheckedPlayers(0).get(0);
		assertTrue(clickedText.contains(ad.actionList.get(1)));
		
		
		
		ad.actionLV.click(2);
		clickedText = ad.getCheckedPlayers(0).get(0);
		assertTrue(clickedText.contains(ad.actionList.get(1)));
		
		nSwitch.consume();
		clickedText = ad.getCheckedPlayers(0).get(0);
		assertTrue(clickedText.contains(ad.actionList.get(2)));
	}
	
	public void testClientChat(){
		ArrayList<Interacter> interacters = init(1);
		Host h = (Host) interacters.get(0);
		Client c = (Client) interacters.get(1);
		
		h.clickFaction("Town");

		c.clickFaction("Mafia");
		
		c.clickButton(R.id.create_toChat);
		if(c.getActivityCreateGame().chatVisible())
			c.clickButton(R.id.create_toChat);
		c.clickButton(R.id.create_toChat);

		h.clickListing(h.getActivityCreateGame().rolesLV, "Citizen");

		assertTrue(c.getActivityCreateGame().chatVisible());
		
		nSwitch.consumeAll();
		assertTrue(c.getActivityCreateGame().chatVisible());
		assertEquals(View.GONE, c.getActivityCreateGame().rolesLV.getVisibility());
	}
	
	public void testCustomFactions(){
		ArrayList<Interacter> interacters = init(0);
		Host h = (Host) interacters.get(0);
		

		ActivityCreateGame ac = h.getActivityCreateGame();
		h.clickFaction("Randoms");
		int prevSize = ac.rolesLV.size();
		
		h.addTeam("Bro", "FF0011");
		for(Interacter i: interacters){
			assertEquals(6, ((ActivityCreateGame) i.getEnvironment().getActive()).cataLV.size());
		}
		
		h.clickFaction("Bro");
		h.clickButton(R.id.create_editMembersButton);

		
		TeamEditor te = h.getActivityCreateGame().tEditor;
		ArrayList<RoleTemplate> randomRoles;
		randomRoles = nSwitch.nSwitch.instances.get(0).fManager.getFaction(Constants.A_RANDOM).members;
		assertTrue(te != null);
		
		te.l2.click(8);//hopefully bd
		nSwitch.consumeAll();
		assertEquals(1, te.l1.size());
		assertEquals(11, randomRoles.size());
		
		
		te.l2.click(13);//hopefully escort
		nSwitch.consumeAll();
		assertEquals(2, te.l1.size());
		assertEquals(12, randomRoles.size());
		

		te.l2.click(19);
		nSwitch.consumeAll();
		te.l2.click(19);
		nSwitch.consumeAll();
		assertEquals(4, te.l1.size());
		
		te.dismiss();
		
		h.clickFaction("Randoms");
		
		assertEquals(prevSize + 1, ac.rolesLV.size());
		
		h.clickListing(ac.rolesLV, "Bro Random");
		nSwitch.consumeAll();
		
		assertEquals(1, ac.rolesListLV.size());
		
	}
	
	public void testMembersShowing(){
		ArrayList<Interacter> interacters = init(2);
		Host h = (Host) interacters.get(0);
		
		h.addRole(BasicRoles.Mayor(), "Town");
		h.addRole(BasicRoles.Arsonist(), "Neutrals");
		h.addRole(BasicRoles.Citizen(), "Town");

		h.clickStart();
		
		GUIController gCon = h.getController();
		assertEquals(3, gCon.dScreen.membersLV.size());
	}
	
	public void testNightChat(){
		ArrayList<Interacter> interacters = init(2);
		Host h = (Host) interacters.get(0);
		
		h.addRole(BasicRoles.Mayor(), "Town");
		h.addRole(BasicRoles.Arsonist(), "Neutrals");
		h.addRole(BasicRoles.Citizen(), "Town");

		Instance curInstance = nSwitch.nSwitch.instances.get(0);
		curInstance.n.getRules().setBool(Rules.DAY_START, Narrator.DAY_START);
		
		h.clickStart();
		nSwitch.consumeAll();
		for(Player p: curInstance.n.getAllPlayers()){
			if(curInstance.n.isDay()){
				p.voteSkip();
			}
		}
		
		for(Player p: curInstance.n.getAllPlayers()){
			p.endNight();
		}
		
		for(Player p: curInstance.n.getAllPlayers()){
			if(curInstance.n.isDay()){
				p.voteSkip();
			}
		}
		
		ArrayList<ChatItem> list = h.getController().dScreen.chatAdapter.chatReference;
		boolean found1 = false, found2 = false;
		for(ChatItem s: list){
			if(!found1 && s.text.contains("Night 1"))
				found1 = true;
			if(!found2 && s.text.contains("Night 2"))
				found2 = true;
			if(found1 && found2)
				break;
		}
			
		assertTrue(found1);
		assertTrue(found2);
	}

	public void testBasicTargeting(){
		ArrayList<Interacter> interacters = init(2);
		Host h = (Host) interacters.get(0);
		
		h.addRole(BasicRoles.Doctor(), "Town");
		h.addRole(BasicRoles.SerialKiller(), "Neutrals");
		h.addRole(BasicRoles.Bodyguard(), "Town");
		
		setNightStart();
		
		Instance i = getInstance();
		h.clickStart();
		
		
		PlayerList pl = i.n._players.copy();
		Player p = i.n._players.getPlayerByName(interacters.get(2).getName());
		Player q = pl.remove(p).getFirst();
		
		GUIController gui = (GUIController) interacters.get(2).getController();
		assertEquals(Doctor.NIGHT_ACTION_DESCRIPTION, gui.dScreen.roleInfoTV.getText().toString());

		gui.setNightTarget(p, q);
		nSwitch.consumeAll();
		assertTrue(p.getActions().isTargeting(q, Role.MAIN_ABILITY));
	}
	
	public Instance getInstance(){
		return nSwitch.nSwitch.instances.get(0);
	}
	
	public void setNightStart(){
		Instance curInstance = getInstance();
		curInstance.n.getRules().setBool(Rules.DAY_START, Narrator.NIGHT_START);
		curInstance.n.setSeed(0);
	}
	
	public void testEndNightPersistance(){
		ArrayList<Interacter> interacters = init(2);
		Host h = (Host) interacters.get(0);
		
		h.addRole(BasicRoles.Mayor(), "Town");
		h.addRole(BasicRoles.Arsonist(), "Neutrals");
		h.addRole(BasicRoles.Citizen(), "Town");

		Instance curInstance = nSwitch.nSwitch.instances.get(0);
		curInstance.n.getRules().setBool(Rules.DAY_START, Narrator.DAY_START);
		curInstance.n.setSeed(0);
		
		h.clickStart();
	}
}