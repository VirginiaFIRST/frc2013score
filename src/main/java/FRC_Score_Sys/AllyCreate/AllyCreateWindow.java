package FRC_Score_Sys.AllyCreate;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import FRC_Score_Sys.MainMenu;
import FRC_Score_Sys.TeamRankObj;

public class AllyCreateWindow extends JFrame {
	
	final Logger logger = LoggerFactory.getLogger(AllyCreateWindow.class);
	private static final long	serialVersionUID	= 1L;
	private MainMenu myParent;
	
	private int TeamCount = 0;
	
	private int SelectedAlly = 0;
	private int SelectedPick = 1;
	
	private JPanel LPanel;
	private JPanel RPanel;
	
	public AllyCreateWindow(MainMenu parent){
		myParent = parent;
		this.setTitle("Create Alliances");
		setLayout(new GridLayout(0, 2, 0, 0));
		String s = "Select 'Yes' for 3v3.\nSelect 'No' for 2v2.";
		String tit = "How many teams per alliances?";
		int perform = JOptionPane.showConfirmDialog(null, s, tit, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		if(perform == JOptionPane.YES_OPTION) TeamCount = 3;
		if(perform == JOptionPane.NO_OPTION) TeamCount = 2;
		List<TeamRankObj> teams = myParent.CommHandle.SqlTalk.FetchTeamlist(true, null);
		
		LPanel = new JPanel();
		RPanel = new JPanel();
		LPanel.setLayout(new GridLayout(0, 1, 0, 0));
		RPanel.setLayout(new GridLayout(0, 3, 0, 0));
		getContentPane().add(LPanel);
		getContentPane().add(RPanel);
		
		int i = 0;
		for(TeamRankObj team : teams){
			i++;
			if(i <= 8){
				// Create top 8.
				AllyTopRow SRow = new AllyTopRow(this, i, team.ID, TeamCount);
				LPanel.add(SRow);
			} else {
				// Create others.
				AllyAvailRow SRow = new AllyAvailRow(this, i, team.ID);
				RPanel.add(SRow);
			}
		}
		this.pack();
		UpdateSelect();
	}
	
	private void UpdateSelect(){
		boolean Complete = false;
		if(SelectedAlly == 8 && SelectedPick == 1) {
			if(TeamCount == 3) {
				SelectedPick = 2;
				SelectedAlly = 9;
			} else {
				Complete = true;
			}
		} else if(!hasAvailableTeams()){
			Complete = true;
		} else if(SelectedAlly == 1 && SelectedPick == 2){
			Complete = true;
		}
		if(!Complete){
			int Moving = 1;
			if(SelectedPick == 2) Moving = -1;
			SelectedAlly = SelectedAlly + Moving;
			for (Component c : LPanel.getComponents()) {
				if (c instanceof AllyTopRow) { 
					AllyTopRow r = ((AllyTopRow)c);
					if(r.Rank() == SelectedAlly){
						r.DoSelect(SelectedPick);
					} else {
						r.DoSelect(0);
					}
				}
			}
		} else {
			String msg = "You are about to move into Quarterfinal Mode. Once this is done, you can not move back to Qualifications.\nWarning: If you select 'NO' you're changes will be lost.\n Continue?";
			String tit = "Save Alliances?";
			int perform = JOptionPane.showConfirmDialog(null, msg, tit, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if(perform == JOptionPane.YES_OPTION) PerformSave();
			pullThePlug();
		}
	}
	
	private void PerformSave(){
		logger.info("AllyCreate Adding Alliances..");
		for (Component c : LPanel.getComponents()) {
			if (c instanceof AllyTopRow) { 
				AllyTopRow r = ((AllyTopRow)c);
				myParent.CommHandle.SqlTalk.AddAllyToDB(r);
			}
		}
		myParent.SwitchElims("QF");
	}
	
	public void SetCapt(int Rank, int Team){
		for (Component c : LPanel.getComponents()) {
			if (c instanceof AllyTopRow) { 
				AllyTopRow r = ((AllyTopRow)c);
				if(r.Rank() == Rank) r.SetCapt(Team);
			}
		}
	}
	
	public int GetCapt(int Rank){
		for (Component c : LPanel.getComponents()) {
			if (c instanceof AllyTopRow) { 
				AllyTopRow r = ((AllyTopRow)c);
				if(r.Rank() == Rank) return r.TeamID();
			}
		}
		return 0;
	}
	public boolean SetValue(int NewVal, boolean isSeed){
		for (Component c : LPanel.getComponents()) {
			if (c instanceof AllyTopRow) { 
				AllyTopRow r = ((AllyTopRow)c);
				if(r.Rank() == SelectedAlly) {
					r.SetValue(SelectedPick, NewVal);
					if(isSeed) MoveTeamsUp(GetRank(NewVal));
					UpdateSelect();
					return true;
				}
			}
		}
		return false;
	}
	
	public void MoveTeamsUp(int FromRank){
		int LowTeam = GetNextTeam();
		int LastCapt = GetCapt(8); 
		SetCapt(8,LowTeam);
		for(int i=7;i!=FromRank-1;i--){
			int thisCapt = GetCapt(i); 
			SetCapt(i,LastCapt);
			LastCapt = thisCapt;
		}
	}
	
	private int GetRank(int Team){
		for (Component c : LPanel.getComponents()) {
			if (c instanceof AllyTopRow) { 
				AllyTopRow r = ((AllyTopRow)c);
				if(r.TeamID() == Team) return r.Rank(); 
			}
		}
		return 0;
	}
	private int GetNextTeam(){
		int LowRank = 99999;
		int LowTeam = 0;
		for (Component c : RPanel.getComponents()) {
			if (c instanceof AllyAvailRow) { 
				AllyAvailRow r = ((AllyAvailRow)c);
				if(r.Rank() < LowRank && r.CanBePicked){
					LowRank = r.Rank();
					LowTeam = r.TeamID();
				}
			}
		}
		for (Component c : RPanel.getComponents()) {
			if (c instanceof AllyAvailRow) { 
				AllyAvailRow r = ((AllyAvailRow)c);
				if(r.Rank() == LowRank){
					r.setCantPick();
				}
			}
		}
		return LowTeam;
	}
public boolean hasAvailableTeams(){
	int available = 0;
	for (Component c : RPanel.getComponents()) {
		if (c instanceof AllyAvailRow) { 
			AllyAvailRow r = ((AllyAvailRow)c);
			if(r.CanBePicked){
				available++;
			}
		}
	}
	logger.info("Found {} teams still available.",available);
	if(available > 1)
		return true;
	else
		return false;
}
	public void pullThePlug() {
		WindowEvent wev = new WindowEvent(this, WindowEvent.WINDOW_CLOSING);
		dispatchEvent(wev);
	}
}
