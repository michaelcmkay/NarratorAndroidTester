package iowrapper;

import java.util.ArrayList;

import android.GUIController;
import android.NActivity;
import android.alerts.PlayerPopUp;
import android.alerts.TeamBuilder;
import android.alerts.TeamEditor;
import android.day.ActivityDay;
import android.day.DayScreenController;
import android.day.TargetablesAdapter;
import android.graphics.Color;
import android.parse.Server;
import android.screens.ListingAdapter;
import android.setup.ActivityCreateGame;
import android.setup.SetupScreenController;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import junit.framework.TestCase;
import shared.ai.Computer;
import shared.logic.Narrator;
import shared.logic.Player;
import shared.logic.PlayerList;
import shared.logic.Team;
import shared.logic.support.Constants;
import shared.logic.support.FactionManager;
import shared.logic.support.Random;
import shared.logic.support.rules.Rules;
import shared.logic.templates.BasicRoles;
import shared.roles.Citizen;
import shared.roles.Driver;
import shared.roles.RandomMember;
import shared.roles.Role;
import shared.roles.Witch;
import voss.narrator.R;

public class PhoneGUITests extends TestCase{

	
	public PhoneGUITests(String name) {
		super(name);
	}
	
	public void runTest() throws Throwable{
		Server.testing = true;
		super.runTest();
	}
	
	public void tearDown() throws Exception{
		Server.testing = true;
		super.tearDown();
	}
	
	public void testNeutralBenign(){
		IOWrapper wrap = new IOWrapper();
		
		long seed = new Random().nextLong();
		//long seed = Long.parseLong("6278177073870124829");
		System.out.println("Testing NeutralBenign(Phone)\t" + seed);
		
		Host h = wrap.startHost(seed);
		h.addRole(RandomMember.NeutralBenignRandom(), "Randoms");
		
		assertEquals(1, h.getNarrator().getAllRoles().size());
	}
	
	public void testSingleHost(){
		for (int j = 0; j < 1; j++){
			IOWrapper wrap = new IOWrapper();
			
			//long seed = new Random().nextLong();
			long seed = Long.parseLong("3192112319501699872");
			System.out.println("Testing Single Host\t" + seed);
			Host h = wrap.startHost(seed);
			
			h.addRole(BasicRoles.MassMurderer(), "Neutrals");
			h.addRole(BasicRoles.CultLeader(),   "Neutrals");
			h.addRandomRole();
			h.addRandomRole();
			h.addRandomRole();
			h.addRandomRole();
			h.addRandomRole();
			h.addRandomRole();
			h.addRandomRole();
			h.addRandomRole();
			h.addRandomRole();
			h.addRandomRole();
			h.addRandomRole();
			
			for (int i = 1; i <= 12; i++){
				h.newPlayer(Computer.toLetter(i));
			}
			
			h.clickStart(seed);
			
			wrap.startBrain();
			while(h.getNarrator().isInProgress()){
				wrap.doActions();
			}
			
			wrap.close();
		}
	}
	
	public void testRemovePlayer(){
		IOWrapper wrap = new IOWrapper();
		
		Host h1 = wrap.startHost();  //no seed means no setting what seed we're using
		h1.newPlayer("C1");
		h1.newPlayer("C2");
		
		ActivityCreateGame ac = h1.getActivityCreateGame();
		h1.clickButton(R.id.roles_show_Players);
		PlayerPopUp pPop = ac.pPop;
		
		assertEquals(3, pPop.lv.size());
		pPop.lv.click(1);
		assertEquals(3, pPop.lv.size());
		pPop.lv.click(1);
		
		assertEquals(2, pPop.lv.size());
	}
	
	private static CheckBox getCheckbox(ActivityDay ad, String name, int column){
		TextView tv;
		for(View v: ad.actionLV.views){
			tv = (TextView) v.findViewById(R.id.target_name);
			if(tv.getText().toString().contains(name)){
				return (CheckBox) v.findViewById(TargetablesAdapter.TranslateColumnToViewId(column));
			}
		}
		return null;
	}
	
	public void testWitchAbility(){
		IOWrapper wrap = new IOWrapper();
		
		Host h1 = wrap.startHost();  //no seed means no setting what seed we're using
		h1.newPlayer("w1");
		h1.newPlayer("w2");
		
		h1.addRole(BasicRoles.Witch(), "Neutrals");
		h1.addRole(BasicRoles.Witch(), "Neutrals");
		h1.addRole(BasicRoles.BusDriver(), "Town");
		
		h1.nightStart();
		h1.clickStart(new Long(0));

		PlayerList pl = h1.getPlayers();
		Player p1 = pl.getFirst(), w1 = pl.get(1), w2 = pl.get(2);
		
		assertTrue(p1.is(Witch.class));
		GUIController c = (GUIController) h1.getController();

		c.selectSlave(p1);
		c.actionPanelClick();
		c.swipeAbilityPanel(Witch.Control);
		
		try{
			c.clickPlayer(p1, 1);
			fail();
		}catch(NullPointerException e){}
		
		TargetablesAdapter ta = c.dScreen.targetablesAdapter;
		assertFalse(getCheckbox(c.dScreen, w1.getName(), 0).isChecked());
		assertFalse(getCheckbox(c.dScreen, w1.getName(), 1).isChecked());
		assertEquals(View.VISIBLE, getCheckbox(c.dScreen, w1.getName(), 0).getVisibility());
		assertEquals(View.VISIBLE, getCheckbox(c.dScreen, w1.getName(), 1).getVisibility());
		
		// [] []
		c.clickPlayer(w1);
		// X []
		assertFalse(getCheckbox(c.dScreen, w1.getName(), 0).isChecked());
		assertTrue(getCheckbox(c.dScreen, w1.getName(), 1).isChecked());
		assertEquals(1, ta.clicked.size());
		TargetablesAdapter.ClickAction ca = new TargetablesAdapter.ClickAction(w1.getName(), getCheckbox(c.dScreen, w1.getName(), 1));
		assertTrue(ta.clicked.contains(ca));
		
		// X []
		c.clickPlayer(w1);
    	// [] X
		assertEquals(1, ta.clicked.size());
		assertTrue(getCheckbox(c.dScreen, w1.getName(), 0).isChecked());
		assertFalse(getCheckbox(c.dScreen, w1.getName(), 1).isChecked());
		assertEquals(1, ta.clicked.size());
		
		// [] X
		c.clickPlayer(w1);
		// X  X
		assertFalse(p1.getActions().isEmpty());
		assertTrue(getCheckbox(c.dScreen, w1.getName(), 0).isChecked());
		assertTrue(getCheckbox(c.dScreen, w1.getName(), 1).isChecked());
		assertEquals(2, ta.clicked.size());
		
		
		// X X
		c.clickPlayer(w1);
		// [] []
		assertTrue(p1.getActions().isEmpty());
		assertFalse(getCheckbox(c.dScreen, w1.getName(), 0).isChecked());
		assertFalse(getCheckbox(c.dScreen, w1.getName(), 1).isChecked());
		assertEquals(0, ta.clicked.size());
		
		// [] []
		c.clickPlayer(w1, 0);
		// [] [X]
		assertFalse(p1.getActions().isTargeting(Player.list(w1, w1), Witch.MAIN_ABILITY));
		
		// [] [X]
		c.clickPlayer(w1, 1);
		// X X
		assertTrue(p1.getActions().isTargeting(Player.list(w1, w1), Witch.MAIN_ABILITY));
		assertTrue(getCheckbox(c.dScreen, w1.getName(), 0).isChecked());
		assertTrue(getCheckbox(c.dScreen, w1.getName(), 1).isChecked());
		assertTrue(p1.getActions().isTargeting(Player.list(w1, w1), Witch.MAIN_ABILITY));
		assertFalse(p1.getActions().isEmpty());
		
		c.clickPlayer(w1);
		assertFalse(getCheckbox(c.dScreen, w1.getName(), 0).isChecked());
		assertFalse(getCheckbox(c.dScreen, w1.getName(), 1).isChecked());
		assertTrue(p1.getActions().isEmpty());

		c.clickPlayer(w2, 1);
		c.clickPlayer(w1, 0);
		
		assertFalse(p1.getActions().isEmpty());
		assertFalse(p1.getActions().isTargeting(Player.list(w2, w2), Role.MAIN_ABILITY));
		assertTrue(p1.getActions().isTargeting(Player.list(w2, w1), Role.MAIN_ABILITY));
		assertFalse(p1.getActions().isTargeting(Player.list(w1, w2), Role.MAIN_ABILITY));
		assertEquals(1, c.dScreen.getCheckedPlayers(0).size());
		assertEquals(1, c.dScreen.getCheckedPlayers(1).size());
		assertEquals(w2.getName(), c.dScreen.getCheckedPlayers(1).get(0));
		assertEquals(w1.getName(), c.dScreen.getCheckedPlayers(0).get(0));
		
		c.clickPlayer(w2, 0);
		assertFalse(p1.getActions().isEmpty());
		assertTrue(p1.getActions().isTargeting(Player.list(w2, w2), Role.MAIN_ABILITY));
		assertEquals(1, c.dScreen.getCheckedPlayers(0).size());
		assertEquals(1, c.dScreen.getCheckedPlayers(1).size());
		assertEquals(w2.getName(), c.dScreen.getCheckedPlayers(1).get(0));
		assertEquals(w2.getName(), c.dScreen.getCheckedPlayers(0).get(0));
		
	}
	
	public void testWitchDeselectTest2(){
		IOWrapper wrap = new IOWrapper();
		
		Host h1 = wrap.startHost();  //no seed means no setting what seed we're using
		h1.newPlayer("w1");
		h1.newPlayer("w2");
		
		h1.addRole(BasicRoles.Witch(), "Neutrals");
		h1.addRole(BasicRoles.Witch(), "Neutrals");
		h1.addRole(BasicRoles.BusDriver(), "Town");
		
		h1.nightStart();
		h1.clickStart(new Long(0));

		PlayerList pl = h1.getPlayers();
		Player p1 = pl.getFirst(), w1 = pl.get(1), w2 = pl.get(2);
		
		assertTrue(p1.is(Witch.class));
		GUIController c = (GUIController) h1.getController();

		c.selectSlave(p1);
		c.actionPanelClick();
		c.swipeAbilityPanel(Witch.Control);
		
		TargetablesAdapter ta = c.dScreen.targetablesAdapter;
		
		c.clickPlayer(w1, 0);
		assertEquals(1, ta.clicked.size());
		
		c.clickPlayer(w1, 1);
		assertEquals(2, ta.clicked.size());
		
		c.clickPlayer(w2, 1);
		assertEquals(2, ta.clicked.size());
		
		c.clickPlayer(w2, 1);
	}
	
	public void testStartScreen(){
		IOWrapper wrap = new IOWrapper();
		
		Host h1 = wrap.startHost();  //no seed means no setting what seed we're using
		h1.newPlayer("BD1");
		h1.newPlayer("BD2");
		
		h1.addRole(BasicRoles.BusDriver(), "Town");
		h1.addRole(BasicRoles.BusDriver(), "Town");
		h1.addRole(BasicRoles.Chauffeur(), "Mafia");
		
		h1.nightStart();
		h1.clickStart();
		
		GUIController c = (GUIController) h1.getController();
		c.rand = new Random();
		c.rand.setSeed(0);
		
		TargetablesAdapter ta = c.dScreen.targetablesAdapter;
		assertEquals(3, ta.targetables.size());
		for(String name: ta.targetables){
			for(int i = 0; i < TargetablesAdapter.MAX; i++){
				assertEquals(View.GONE, getCheckbox(c.dScreen, name, i).getVisibility());
			}
		}
	}
	
	public void testDriverAbilityTest(){
		IOWrapper wrap = new IOWrapper();
		
		Host h1 = wrap.startHost();  //no seed means no setting what seed we're using
		h1.newPlayer("BD1");
		h1.newPlayer("BD2");
		
		h1.addRole(BasicRoles.BusDriver(), "Town");
		h1.addRole(BasicRoles.BusDriver(), "Town");
		h1.addRole(BasicRoles.Chauffeur(), "Mafia");
		
		h1.nightStart();
		h1.clickStart();
		
		GUIController c = (GUIController) h1.getController();
		c.rand = new Random();
		c.rand.setSeed(0);
		
		PlayerList pl = h1.getPlayers();
		Player p1 = pl.getFirst(), p2 = pl.get(1), p3 = pl.get(2);
		
		c.selectSlave(p1);
		assertTrue(c.dScreen.manager.dScreenController.playerSelected());
		
		assertTrue(c.dScreen.playerLabelTV.getText().equals(p1.getName()));
		String commandText = c.dScreen.commandTV.getText().toString();
		assertFalse(commandText.equals(DayScreenController.HAVENT_ENDED_NIGHT_TEXT));
		
		c.swipeAbilityPanel(Driver.COMMAND);
		TargetablesAdapter ta = c.dScreen.targetablesAdapter;
		assertEquals(Driver.COMMAND, c.dScreen.manager.getCommand());
		assertEquals(View.GONE, getCheckbox(c.dScreen, p1.getName(), 1).getVisibility());
		
		c.clickPlayer(p1, 0);
		assertTrue(p1.getActions().isEmpty());
		
		
		try{
			c.clickPlayer(p1, 1);
			fail();
		}catch(NullPointerException e){}
		
		c.clickPlayer(p2, 0);
		assertFalse(p1.getActions().isEmpty());
		ta = c.dScreen.targetablesAdapter;
		assertEquals(2, ta.clicked.size());
		
		//deselecting 
		c.clickPlayer(p1, 0);
		ta = c.dScreen.targetablesAdapter;
		assertEquals(1, ta.clicked.size());
		assertTrue(p1.getActions().isEmpty());
		
		c.clickPlayer(p1, 0);
		assertFalse(p1.getActions().isEmpty());
		c.clickPlayer(p2, 0);
		assertTrue(p1.getActions().isEmpty());
		
		c.clickPlayer(p2, 0);
		c.clickPlayer(p3, 0);
		//test for proper 'popoff'
		
		assertTrue(p1.getActions().isTargeting(Player.list(p2,p3), Role.MAIN_ABILITY));
		assertFalse(getCheckbox(c.dScreen, p1.getName(), 0).isChecked());
		assertEquals(2, ta.clicked.size());

		c.clickPlayer(p3, 0);
		assertEquals(1, ta.clicked.size());

		c.clickPlayer(p3, 0);
	}
	
	public void testSkipDayText(){
		IOWrapper wrap = new IOWrapper();
		Host h = wrap.startHost();
		h.newPlayer("p1");
		h.newPlayer("p2");
		
		h.addRole(BasicRoles.MassMurderer(), "Neutrals");
		h.addRole(BasicRoles.Citizen(), "Town");
		h.addRole(BasicRoles.Citizen(), "Town");
		
		h.dayStart();
		h.clickStart();
		
		GUIController g = h.getController();
		
		Player first = h.getPlayers().getFirst();
		g.selectSlave(first);
		g.actionPanelClick();
		
		ArrayList<String> actionList = g.dScreen.actionList;
		assertEquals(3, actionList.size());
		assertFalse(actionList.contains(first.getName()));
		
		g.skipVote(first);
		assertEquals(1, h.getNarrator().Skipper.getVoteCount());
	}
	
	
	
	public void testSinglePlayer(){
		IOWrapper wrap = new IOWrapper();
		long seed = new Random().nextLong();
		//long seed = Long.parseLong("-5740312402763706335");
		System.out.println("Single Player :\t " + seed);
		
		Host h = wrap.startHost(seed);
		h.addRole(BasicRoles.MassMurderer(), "Neutrals");
		NActivity na = (NActivity) h.getEnvironment().getActive();
		assertEquals(1, na.ns.local.getAllRoles().size());
		
		h.newComputer();
		h.addRole(BasicRoles.Citizen(), "Town");
		assertEquals(2, na.ns.local.getAllRoles().size());
		
		
		for (int i = 0; i < 1; i++){
			h.newComputer();
			h.addRandomRole();
		}
		
		assertEquals(3, na.ns.local.getAllRoles().size());
		
		
		h.clickStart(seed);
	
		wrap.startBrain();
		
		
		while(h.getNarrator().isInProgress() ){
			//ad.onDoubleTap();
			wrap.doActions();
		}
		wrap.close();
	}
	
	public void testDoubleClick(){
		IOWrapper wrap = new IOWrapper();

		Long l = new Random().nextLong();
		//Long l = Long.parseLong("-6729159550149294020");
		System.out.println("Testing Double Click: \t " + l);
		
		Host h = wrap.startHost(l);
		
		int playerSize = 15;
		
		for(int i = 0; i < playerSize - 1; i++)
			h.newPlayer(Computer.toLetter(i+2));
		
		h.setAllComputers();
		for(Player comp : h.getPlayers()){
			assertTrue(comp.isComputer());
		}
		
		h.addRole(BasicRoles.Framer(), "Mafia");
		h.addRole(BasicRoles.BusDriver(), "Town");
		
		for(int i = 0; i < playerSize - 2; i++){
			h.addRandomRole();
		}
		
		
		h.clickStart(l);
		
		assertTrue(h.getNarrator().isInProgress());
		
		while(h.getNarrator().isInProgress())
			h.doubleClick();
	}
	
	public void testRuleText(){
		IOWrapper wrap = new IOWrapper();
		
		Host h = wrap.startHost();
		
		h.addRole(BasicRoles.Consort(), "Mafia");
		ActivityCreateGame ac = (ActivityCreateGame) h.getEnvironment().getActive();
		SetupScreenController sc = ac.getManager().screenController;
				
		TextView ruleLabel = (TextView) ac.findViewById(R.id.create_info_label);
		TextView ruleDescrip = (TextView) ac.findViewById(R.id.create_info_description);
		assertEquals(BasicRoles.CONSORT, ruleLabel.getText());
		assertEquals(ruleLabel.getCurrentTextColor(), Color.parseColor(Constants.A_MAFIA));
		
		CheckBox blockCB = sc.cBox[0];
		boolean prevVal = blockCB.isChecked();
		assertEquals(h.getNarrator().getRules().getBool(Rules.ROLE_BLOCK_IMMUNE), prevVal);
		blockCB.toggle();
		assertEquals(h.getNarrator().getRules().getBool(Rules.ROLE_BLOCK_IMMUNE), blockCB.isChecked());
		
		h.clickFaction("Mafia");
		assertEquals(ruleLabel.getText().toString(), ac.ns.fManager.getFaction(Constants.A_MAFIA).getName());
		assertEquals(ruleDescrip.getText().toString(), ac.ns.fManager.getFaction(Constants.A_MAFIA).getDescription());
		assertEquals(ruleDescrip.getVisibility(), View.VISIBLE);
		
		h.clickFaction("Randoms");
		assertEquals(ruleLabel.getVisibility(), View.GONE);
		assertEquals(ruleDescrip.getText().toString(), ac.ns.fManager.getFaction(Constants.A_RANDOM).getDescription());
		assertEquals(Rules.DAY_START[1], blockCB.getText().toString());
		blockCB.toggle();
		boolean dayStart = blockCB.isChecked();
		
		h.clickFaction("Neutrals");
		assertEquals(ruleLabel.getVisibility(), View.VISIBLE);
		
		h.newPlayer("J");
		h.newPlayer("R");
		h.addRole(BasicRoles.Agent(), "Mafia");
		h.addRole(BasicRoles.Citizen(), "Town");
		h.clickStart();
		
		assertEquals(dayStart, h.getNarrator().isDay());
	}
	
	public void testRemoveRole(){
		IOWrapper wrap = new IOWrapper();
		
		Host h = wrap.startHost();
		
		h.addRole(BasicRoles.Consort(), "Mafia");
		ActivityCreateGame ac = (ActivityCreateGame) h.getEnvironment().getActive();
		ac.rolesListLV.click(0);
		
		assertTrue(h.getNarrator().getAllRoles().isEmpty());
	}

	public void testNoFactionSelected(){
		IOWrapper wrap = new IOWrapper();
		Host h = wrap.startHost();
		
		ActivityCreateGame ac = (ActivityCreateGame) h.getEnvironment().getActive();
		assertEquals(ac.rolesLV.getVisibility(), View.GONE);
		assertEquals(ac.findViewById(R.id.create_info_wrapper).getVisibility(), View.GONE);
		h.clickFaction("Town");
		assertEquals(ac.findViewById(R.id.create_info_wrapper).getVisibility(), View.VISIBLE);
		assertEquals(ac.rolesLV.getVisibility(), View.VISIBLE);
	}
	
	public void testNewTeamButton(){
		IOWrapper wrap = new IOWrapper();
		Host h = wrap.startHost();
		
		ActivityCreateGame ac = (ActivityCreateGame) h.getEnvironment().getActive();
		h.clickButton(R.id.create_createTeamButton);
		TeamBuilder tb = (TeamBuilder) ac.getFragmentManager().getFragment(null, "newTeam");
		
		tb.nameInput.setText("Bro");
		tb.colorInput.setText("3g4");
		assertEquals(tb.preview.getText().toString(), "Bro");
		assertEquals(tb.preview.getCurrentTextColor(), Color.parseColor("#FFFFFF"));
		
		tb.colorInput.setText("3f4");
		assertEquals(tb.preview.getCurrentTextColor(), Color.parseColor("#33FF44"));
		
		tb.colorInput.setText("#44444");
		((Button) tb.mainView.findViewById(R.id.newTeam_submit)).click();
		assertEquals(tb.preview.getCurrentTextColor(), Color.parseColor("#FF0000"));
		assertEquals(tb.preview.getText().toString(), TeamBuilder.RGB_ERROR_CODE);
		
		tb.nameInput.setText("Town");
		tb.colorInput.setText("0FF");
		((Button) tb.mainView.findViewById(R.id.newTeam_submit)).click();
		assertEquals(tb.preview.getCurrentTextColor(), Color.parseColor("#FF0000"));
		assertEquals(tb.preview.getText().toString(), FactionManager.TEAM_TAKEN);
		
		tb.nameInput.setText("Bro");
		tb.colorInput.setText(Constants.A_ARSONIST);
		((Button) tb.mainView.findViewById(R.id.newTeam_submit)).click();
		assertEquals(tb.preview.getCurrentTextColor(), Color.parseColor("#FF0000"));
		assertEquals(tb.preview.getText().toString(), FactionManager.COLOR_TAKEN);
		
		tb.colorInput.setText("#FFE");
		((Button) tb.mainView.findViewById(R.id.newTeam_submit)).click();
		assertEquals(ac.ns.fManager.factions.size(), ac.cataLV.size());
		
		CheckBox cb = ac.getManager().screenController.cBox[0];
		assertEquals("Has Faction kill", cb.getText());
	}
	
	public void testButtonVisibility(){
		IOWrapper wrap = new IOWrapper();
		Host h = wrap.startHost();
		
		trioInvisible(h);
		h.clickFaction("Town");
		trioInvisible(h);
		h.clickFaction("Mafia");
		trioInvisible(h);
		h.clickFaction("Randoms");
		trioInvisible(h);
		h.addRole(BasicRoles.BusDriver(), "Town");
		trioInvisible(h);
		
		h.addTeam("Bro", "#3FA");
		
		trioVisible(h);
	}
	
	private void trioVisible(Host h){
		testVisibility(h, View.VISIBLE);
	}
	
	private void trioInvisible(Host h){
		testVisibility(h, View.GONE);
	}
	
	private void testVisibility(Host h, int visibility){
		ActivityCreateGame ac = (ActivityCreateGame) h.getEnvironment().getActive();
		Button editAlly = (Button) ac.findViewById(R.id.create_editAlliesButton);
		Button editRoles = (Button) ac.findViewById(R.id.create_editMembersButton);
		Button deleteTeam = (Button) ac.findViewById(R.id.create_deleteTeamButton);
		
		assertEquals(editAlly.getVisibility(), visibility);
		assertEquals(editRoles.getVisibility(), visibility);
		assertEquals(deleteTeam.getVisibility(), visibility);
	}
	
	public void testDeleteTeam(){
		IOWrapper wrap = new IOWrapper();
		Host h = wrap.startHost();
		ActivityCreateGame ac = (ActivityCreateGame) h.getEnvironment().getActive();

		int size = ac.ns.fManager.factions.size();
		
		h.addTeam("Bro", "#3FA");
		assertEquals(size + 1, ac.cataLV.size());
		
		h.clickButton(R.id.create_deleteTeamButton);
		assertEquals(size, ac.cataLV.size());
	}
	
	public void testEditAllies(){
		IOWrapper wrap = new IOWrapper();
		Host h = wrap.startHost();
		ActivityCreateGame ac = (ActivityCreateGame) h.getEnvironment().getActive();
		
		h.addTeam("Bro", "#3FA");
		Team broTeam = ac.ns.local.getTeam("#33FFAA");
		
		h.clickButton(R.id.create_editAlliesButton);

		TeamEditor te = (TeamEditor) ac.getFragmentManager().getFragment(null, "editTeam");
		assertEquals(te.getDialog().getTitle(), TeamEditor.EDITING_ALLIES_TITLE);
		
		//test headers
		assertEquals(((TextView) te.mainView.findViewById(R.id.editTeamTV1)).getText().toString(), TeamEditor.ALLIES_TITLE);
		assertEquals(((TextView) te.mainView.findViewById(R.id.editTeamTV2)).getText().toString(), TeamEditor.ENEMIES_TITLE);
		
		ListingAdapter la = (ListingAdapter) te.l1.adapter;
		String color = la.colors.get(0);
		Team newEnemyTeam = ac.ns.local.getTeam(color);
		
		assertFalse(newEnemyTeam.isEnemy(broTeam));
		te.l1.click(0);
		assertTrue(newEnemyTeam.isEnemy(broTeam));
		
		te.l2.click(0);
		assertFalse(newEnemyTeam.isEnemy(broTeam));
	}
	
	public void testEditRoles(){
		IOWrapper wrap = new IOWrapper();
		Host h = wrap.startHost();
		ActivityCreateGame ac = (ActivityCreateGame) h.getEnvironment().getActive();
		
		h.addTeam("Bro", "#3FA");
		h.clickButton(R.id.create_editMembersButton);

		TeamEditor te = (TeamEditor) ac.tEditor;
		assertEquals(te.getDialog().getTitle(), TeamEditor.EDITING_ROLES_TITLE);
		
		//test headers
		assertEquals(((TextView) te.mainView.findViewById(R.id.editTeamTV1)).getText().toString(), TeamEditor.AVAILABLE_ROLES_TITLE);
		assertEquals(((TextView) te.mainView.findViewById(R.id.editTeamTV2)).getText().toString(), TeamEditor.BLACKLISTED_ROLES_TITLE);
		
		ListingAdapter la2 = (ListingAdapter) te.l2.adapter;
		ListingAdapter la1 = (ListingAdapter) te.l1.adapter;
		int posOfBD = la2.data.indexOf(BasicRoles.BUS_DRIVER);
		assertFalse(la1.data.contains(BasicRoles.BUS_DRIVER));
		assertTrue(la2.data.contains(BasicRoles.BUS_DRIVER));
		
		te.l2.click(posOfBD);

		la2 = (ListingAdapter) te.l2.adapter;
		la1 = (ListingAdapter) te.l1.adapter;
		assertFalse(la2.data.contains(BasicRoles.BUS_DRIVER));
		assertTrue(la1.data.contains(BasicRoles.BUS_DRIVER));
	}
	
	public void testDeleteTeamPurgeRole(){
		IOWrapper wrap = new IOWrapper();
		Host h = wrap.startHost();
		ActivityCreateGame ac = (ActivityCreateGame) h.getEnvironment().getActive();
		
		h.addTeam("Bro", "#3FA");
		h.clickButton(R.id.create_editMembersButton);
		
		TeamEditor te = (TeamEditor) ac.getFragmentManager().getFragment(null, "editTeam");
		ListingAdapter la2 = (ListingAdapter) te.l2.adapter;
		int posOfCit = la2.data.indexOf(Citizen.class.getSimpleName());
		
		te.l2.click(posOfCit);//get it? piece of poop
		te.l2.click(0);
		
		h.clickButton(R.id.editTeamConfirm);
		h.addRole(BasicRoles.Citizen().setColor("#33FFAA"), "Bro");
		
		h.clickFaction("Randoms");
		ListingAdapter la = (ListingAdapter) ac.rolesLV.adapter;
		int broIndex = la.data.indexOf("Bro Random");
		ac.rolesLV.click(broIndex);
		
		assertEquals(2, ac.rolesListLV.size());
		
		h.clickFaction("Bro");
		h.clickButton(R.id.create_deleteTeamButton);
		
		assertEquals(0, ac.rolesListLV.size());
	}
	
	public void testDeselect(){
		IOWrapper wrap = new IOWrapper();
		Host h = wrap.startHost();
		h.newPlayer("p1");
		h.newPlayer("p2");
		
		h.addRole(BasicRoles.MassMurderer(), "Neutrals");
		h.addRole(BasicRoles.Sheriff(), "Town");
		h.addRole(BasicRoles.Doctor(),  "Town");
		
		h.dayStart();
		h.clickStart();
		
		Narrator n = h.getNarrator();
		assertTrue(n.isInProgress());
		
		Player p1 = n.getPlayerByName("p1");
		Player p2 = n.getPlayerByName("p2");
		
		
		
		
		h.getController().vote(p1, p2);
		assertEquals(1, p2.getVoteCount());
		
		boolean containsP2 = false;
		for(String s: h.getController().dScreen.getCheckedPlayers(0)){
			if(s.contains(p2.getName()))
				containsP2 = true;
		}
		assertTrue(containsP2);
		h.getController().unvote(p1);
		
		assertTrue(p2.getVoters().isEmpty());
		
		p1.voteSkip();
		p2.voteSkip();
		
		ActivityDay ad = h.getController().dScreen;
		ad.actionLV.click(0);
		ad.actionLV.click(1);
		
		assertTrue(getCheckbox(h.getController().dScreen, ad.actionList.get(1), 0).isChecked());
	}
	
}












