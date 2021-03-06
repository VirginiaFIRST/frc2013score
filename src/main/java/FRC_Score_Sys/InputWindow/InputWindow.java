package FRC_Score_Sys.InputWindow;

import FRC_Score_Sys.MainMenu;
import FRC_Score_Sys.SingleMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class InputWindow extends JFrame {
	private static final long serialVersionUID = 1;
	private MainMenu myParent;

	ScorePanel RedPanel;
	ScorePanel BluePanel;

	String MatchNumber = "Unk";

	JTextField WinnerDisplay;

	Color color_red = new Color(255, 106, 0);
	Color color_blue = new Color(30, 144, 255);
	Color color_yellow = new Color(242, 255, 0);
	boolean loaded = false;
	boolean did_save = false;
	private Logger logger = LoggerFactory.getLogger(InputWindow.class);

	private List<Integer> TeamNumbers;
	
	public InputWindow(MainMenu parent, String MatchNumber) {
		myParent = parent;
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				String msg = "im_closing";
				if (did_save) {
					msg = "im_closing_modified";
				}
				TellParent(msg, TeamNumbers);
			}
		});
		logger.info("Input Window for Match #{} Starting", MatchNumber);
		setResizable(false);
		setAlwaysOnTop(true);
		setTitle("Input Match Results - Match " + MatchNumber);

		GridBagLayout gridBagLayout = new GridBagLayout();
		getContentPane().setLayout(gridBagLayout);

		SingleMatch BlueMatch = new SingleMatch();
		SingleMatch RedMatch = new SingleMatch();
		
		TeamNumbers = new ArrayList<Integer>();
		try {
			List<SingleMatch> DBScores = myParent.CommHandle.SqlTalk.FetchMatch(MatchNumber);
			if (DBScores.size() == 2) {
				for (SingleMatch Match : DBScores) {
					TeamNumbers.add(Match.Robot1);
					// in the elims all we care about is the captain
					if(MatchNumber.startsWith("QQ")){
						TeamNumbers.add(Match.Robot2);
						TeamNumbers.add(Match.Robot3);
					}
					if (Match.aColor() == "R") {
						RedMatch = Match;
					}
					if (Match.aColor() == "B") {
						BlueMatch = Match;
					}
				}
			} else {
				// I'll disable the window here because apparently I can not
				// trigger a close event from the constructor. XD
				logger.error("Malformed score data received. Likely the match doesn't exist. Disabling Window.");
				setEnabled(false);
				setTitle("Defunct Input Window. Match did not Exist. Please Close Me.");
			}
		} catch (Exception e) {
			logger.info("Unable to fetch match for DB");
			pullThePlug();
		}
		// MAIN PANEL WHERE SCORES TALLY
		JPanel MainPanel = new JPanel();
		BluePanel = new ScorePanel(this, color_blue, BlueMatch);
		MainPanel.add(BluePanel);
		RedPanel = new ScorePanel(this, color_red, RedMatch);
		MainPanel.add(RedPanel);

		GridBagConstraints gbc_MainPanel = new GridBagConstraints();
		gbc_MainPanel.fill = GridBagConstraints.BOTH;
		gbc_MainPanel.insets = new Insets(0, 0, 5, 0);
		gbc_MainPanel.gridx = 0;
		gbc_MainPanel.gridy = 0;
		getContentPane().add(MainPanel, gbc_MainPanel);

		// BUTTONS PANEL
		JPanel BtnsPanel = new JPanel();
		BtnsPanel.setLayout(new GridLayout(0, 3, 0, 0));
		JButton SaveBtn = new JButton("Save & Close");
		SaveBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				// BUTTON PRESSED
				// Redo Calc to be sure we're up to date.
				logger.debug("Pending an Input Window Close let's do one more Calculate, in case something changed.");
				DoCalc();
				// Save
				logger.info("Requesting Save to DB...");
				List<SingleMatch> DataToSave = new ArrayList<SingleMatch>();
				
				SingleMatch BlueData = BluePanel.GetRawData(); 
				SingleMatch RedData  = RedPanel.GetRawData();
				
				// Add the other ally's penalty points to the TP score per Scoring manual
				BlueData.TP = BlueData.TP +  RedPanel.PenRow.GetPenalties();
				RedData.TP  =  RedData.TP + BluePanel.PenRow.GetPenalties();
				
				DataToSave.add(BlueData);
				DataToSave.add(RedData);
				boolean Saved = myParent.CommHandle.SqlTalk.SaveMatchChanges(DataToSave);
				if (Saved) {
					// Close Window
					did_save = true;
					logger.debug("Input Window Close request pending..");
					pullThePlug();
				} else {
					// TODO: Add dialog.
					logger.error("Woah. SQL Save failed?");
				}

			}
		});
		WinnerDisplay = new JTextField();
		WinnerDisplay.setHorizontalAlignment(SwingConstants.CENTER);

		JButton RefBtn = new JButton("Refresh Calculations");
		RefBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				// BUTTON PRESSED
				DoCalc();
			}
		});
		JButton ResetBtn = new JButton("Reset");
		ResetBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				// BUTTON PRESSED
				RedPanel.RequestReset();
				BluePanel.RequestReset();
				DoCalc();
			}
		});
		String DispMatchType = "??";
		switch (MatchNumber.substring(0, 2)) {
		case "QQ":
			DispMatchType = "Qualification #" + MatchNumber.substring(2);
			break;
		case "QF":
			DispMatchType = "Quarterfinal #" + MatchNumber.substring(2);
			break;
		case "SF":
			DispMatchType = "Semifinal #" + MatchNumber.substring(2);
			break;
		case "FF":
			DispMatchType = "Final #" + MatchNumber.substring(2);
			break;
		}
		JLabel Filler1 = new JLabel(DispMatchType);
		Filler1.setHorizontalAlignment(SwingConstants.CENTER);
		JLabel Filler2 = new JLabel(myParent.ei.EventName);
		Filler2.setHorizontalAlignment(SwingConstants.CENTER);

		BtnsPanel.add(RefBtn);
		BtnsPanel.add(WinnerDisplay);
		BtnsPanel.add(ResetBtn);
		BtnsPanel.add(Filler1);
		BtnsPanel.add(SaveBtn);
		BtnsPanel.add(Filler2);
		CheckWinner(0, 0);

		GridBagConstraints gbc_BtnsPanel = new GridBagConstraints();
		gbc_BtnsPanel.fill = GridBagConstraints.CENTER;
		gbc_BtnsPanel.gridx = 0;
		gbc_BtnsPanel.gridy = 1;
		getContentPane().add(BtnsPanel, gbc_BtnsPanel);
		loaded = true;
		DoCalc();
	}

	private void CheckWinner(int Red, int Blue) {
		if (Red == Blue) {
			WinnerDisplay.setText("DRAW");
			WinnerDisplay.setBackground(color_yellow);
			BluePanel.my_QS = 1;
			RedPanel.my_QS  = 1;
		}
		if (Red > Blue) {
			WinnerDisplay.setText("RED WINS");
			WinnerDisplay.setBackground(color_red);
			BluePanel.my_QS = 0;
			RedPanel.my_QS  = 2;
		}
		if (Blue > Red) {
			WinnerDisplay.setText("BLUE WINS");
			WinnerDisplay.setBackground(color_blue);
			BluePanel.my_QS = 2;
			RedPanel.my_QS  = 0;
		}
	}

	public void DoCalc() {
		if (loaded) {
			int rPen = RedPanel.GetPenalties();
			int bPen = BluePanel.GetPenalties();
			int rFinal = RedPanel.DoRefresh(bPen);
			int bFinal = BluePanel.DoRefresh(rPen);
			CheckWinner(rFinal, bFinal);
		}
	}

	public void pullThePlug() {
		this.setVisible(false);
		WindowEvent wev = new WindowEvent(this, WindowEvent.WINDOW_CLOSING);
		dispatchEvent(wev);
	}

	private void TellParent(String Msg, Object Datagram) {
		myParent.RecvChildWindowMsg(this, Msg, Datagram);
	}
}