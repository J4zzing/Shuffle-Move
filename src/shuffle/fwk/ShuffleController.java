/*  ShuffleMove - A program for identifying and simulating ideal moves in the game
 *  called Pokemon Shuffle.
 *  
 *  Copyright (C) 2015  Andrew Meyers
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package shuffle.fwk;

import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Set;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.FontUIResource;

import org.apache.commons.lang3.StringUtils;

import shuffle.fwk.config.ConfigFactory;
import shuffle.fwk.config.ConfigManager;
import shuffle.fwk.config.EntryType;
import shuffle.fwk.config.manager.BoardManager;
import shuffle.fwk.config.manager.EffectManager;
import shuffle.fwk.config.manager.EntryModeManager;
import shuffle.fwk.config.manager.GradingModeManager;
import shuffle.fwk.config.manager.ImageManager;
import shuffle.fwk.config.manager.RosterManager;
import shuffle.fwk.config.manager.SpeciesManager;
import shuffle.fwk.config.manager.TeamManager;
import shuffle.fwk.data.Board.Status;
import shuffle.fwk.data.Effect;
import shuffle.fwk.data.Species;
import shuffle.fwk.data.SpeciesPaint;
import shuffle.fwk.data.Stage;
import shuffle.fwk.data.Team;
import shuffle.fwk.data.simulation.SimulationResult;
import shuffle.fwk.data.simulation.SimulationTask;
import shuffle.fwk.data.simulation.SimulationUser;
import shuffle.fwk.gui.GridPanel;
import shuffle.fwk.gui.ShuffleFrame;
import shuffle.fwk.gui.user.ShuffleFrameUser;
import shuffle.fwk.i18n.I18nUser;
import shuffle.fwk.service.BaseServiceManager;
import shuffle.fwk.service.movepreferences.MovePreferencesService;

/**
 * @author Andrew Meyers
 *
 */
public class ShuffleController extends Observable implements ShuffleViewUser, ShuffleModelUser, ShuffleFrameUser,
      SimulationUser, I18nUser {
   /** The log properties file path */
   private static final String LOG_CONFIG_FILE = "config/logger.properties";

   /** The logger for this controller. */
   private static final Logger LOG = Logger.getLogger(ShuffleController.class.getName());
   
   static { // Sets look and feel to a nicer version than the default
      try {
         UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
      } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
            | UnsupportedLookAndFeelException e) {
         LOG.warning("Cannot load NimbusLookAndFeel because: " + e.getMessage());
      }
   }
   
   // i18n keys
   private static final String KEY_LOAD_ALL = "log.all.load";
   private static final String KEY_SAVE_ALL = "log.all.save";
   private static final String KEY_SAVE_ALL_SUCCESS = "log.all.save.success";
   private static final String KEY_LOAD_ROSTER = "log.roster.load";
   private static final String KEY_SAVE_ROSTER = "log.roster.save";
   private static final String KEY_SAVE_ROSTER_SUCCESS = "log.roster.save.success";
   private static final String KEY_LOAD_TEAM = "log.team.load";
   private static final String KEY_SAVE_TEAM = "log.team.save";
   private static final String KEY_SAVE_TEAM_SUCCESS = "log.team.save.success";
   private static final String KEY_CLEAR_GRID = "log.grid.cleared";
   private static final String KEY_LOAD_GRID = "log.grid.load";
   private static final String KEY_LOAD_DEFAULT_GRID = "log.grid.load.default";
   private static final String KEY_SAVE_GRID = "log.grid.save";
   private static final String KEY_SAVE_GRID_SUCCESS = "log.grid.save.success";
   private static final String KEY_DO_MOVE = "log.move.do";
   private static final String KEY_REDO_MOVE = "log.move.redo";
   private static final String KEY_UNDO_MOVE = "log.move.undo";
   private static final String KEY_COMPUTE_NOW = "log.compute.now";
   private static final String KEY_COMPUTE_AUTO_TRUE = "log.compute.auto.on";
   private static final String KEY_COMPUTE_AUTO_FALSE = "log.compute.auto.off";
   private static final String KEY_ROSTER_CHANGED = "log.roster.changed";
   private static final String KEY_TEAM_CHANGED = "log.team.changed";
   private static final String KEY_IMAGES_CHANGED = "log.images.changed";
   private static final String KEY_SPECIES_CHANGED = "log.species.changed";
   private static final String KEY_GRADING_CHANGED = "log.grading.changed";
   
   // Scaling config keys
   private static final String KEY_FONT_SIZE_SCALING = "FONT_SIZE_SCALING";
   private static final String KEY_BORDER_SCALING = "BORDER_SCALING";
   
   /** The model for this controller. */
   private ShuffleModel model;
   /** The view for this controller. */
   private ShuffleView view;
   
   /** The ShuffleFrame which is the primary Window for the program. */
   private ShuffleFrame frame = null;
   /** The ConfigFactory used for manager creation */
   private ConfigFactory factory = null;
   
   /**
    * The main which starts the program.
    * 
    * @param args
    *           unused.
    */
   public static void main(String... args) {
      String userHomeArg = null;
      String levelToSetArg = null;

      if (args != null && args.length > 0) {
         userHomeArg = args[0];
         if (args.length > 1) {
            levelToSetArg = args[1];
         }
      }
      
      if (userHomeArg == null) {
         userHomeArg = System.getProperty("user.home") + File.separator + "Shuffle-Move";
      }
      setUserHome(userHomeArg);
      try {
         FileHandler handler = new FileHandler();
         Logger toRoot = LOG;
         while (toRoot.getParent() != null) {
            toRoot = toRoot.getParent();
         }
         toRoot.addHandler(handler);
      } catch (SecurityException e1) {
         e1.printStackTrace();
      } catch (IOException e1) {
         e1.printStackTrace();
      }

      if (levelToSetArg != null) {
         try {
            Level levelToSet = Level.parse(args[1]);
            Logger.getLogger(SimulationTask.class.getName()).setLevel(levelToSet);
            SimulationTask.setLogFiner(levelToSet.intValue() <= Level.FINER.intValue());
            Logger.getLogger(ShuffleModel.class.getName()).setLevel(levelToSet);
         } catch (Exception e) {
            LOG.fine("Cannot set simulation logging to that level: " + StringUtils.join(args));
         }
      }

      ShuffleController ctrl = new ShuffleController();
      SwingUtilities.invokeLater(new Runnable() {
         @Override
         public void run() {
            ctrl.getFrame().launch();
         }
      });
   }
   
   /**
    * Sets the user home to the given path.
    * 
    * @param userHome
    *           The absolute path to the new user home.
    */
   public static void setUserHome(String userHome) {
      try {
         File absoluteFile = new File(userHome).getCanonicalFile();
         absoluteFile.mkdir();
         System.setProperty("user.dir", absoluteFile.getCanonicalPath());
         System.setProperty("user.home", absoluteFile.getCanonicalPath());
         try (InputStream is = ClassLoader.getSystemResourceAsStream(LOG_CONFIG_FILE)) {
            File logDir = new File("log").getAbsoluteFile();
            if (!logDir.exists() && !logDir.mkdirs()) {
               throw new IOException("Cannot create log directory.");
            }
            LogManager.getLogManager().readConfiguration(is);
         } catch (IOException e) {
            e.printStackTrace();
         }
      } catch (SecurityException e) {
         e.printStackTrace();
      } catch (IOException e1) {
         e1.printStackTrace();
      }
   }
   
   /**
    * Creates a ShuffleController with the given configuration paths for the primary configuration
    * (which tells other managers where to get their configurations). If there are none passed, then
    * "config/main.txt" is assumed.
    * 
    * @param configPaths
    *           The paths as Strings
    */
   public ShuffleController(String... configPaths) {
      if (configPaths.length > 0 && configPaths[0] != null) {
         factory = new ConfigFactory(configPaths[0]);
      } else {
         factory = new ConfigFactory();
      }
      Integer menuFontOverride = getPreferencesManager().getIntegerValue(KEY_FONT_SIZE_SCALING);
      if (menuFontOverride != null && menuFontOverride != 100 && menuFontOverride >= 1 && menuFontOverride <= 10000) {
         float scale = menuFontOverride.floatValue() / 100.0f;
         try {
            // This is the cleanest and most bug-free way to do this hack.
            Set<Object> allKeys = new HashSet<Object>();
            allKeys.add("JMenu.font");
            // Yes we're not supposed to use this, but it is the only one that works with Nimbus LAF
            allKeys.addAll(UIManager.getLookAndFeelDefaults().keySet());
            Object value = UIManager.get("defaultFont");
            if (value != null && value instanceof FontUIResource) {
               FontUIResource fromFont = (javax.swing.plaf.FontUIResource) value;
               FontUIResource toFont = new FontUIResource(fromFont.deriveFont(fromFont.getSize() * scale));
               // This one is necessary
               UIManager.getLookAndFeel().getDefaults().put("defaultFont", toFont);
               // And this one allows other LAF to be used in the future
               UIManager.getDefaults().put("defaultFont", toFont);
            }
            
            // Needed for Nimbus's JTable row height adjustment
            Object tableFontValue = UIManager.getLookAndFeel().getDefaults().get("Table.font");
            Number bestRowHeight = null;
            if (tableFontValue != null && tableFontValue instanceof FontUIResource) {
               FontUIResource fromFont = (FontUIResource) tableFontValue;
               bestRowHeight = fromFont.getSize();
            }
            Object rowHeightValue = UIManager.getLookAndFeel().getDefaults().get("Table.rowHeight");
            if (rowHeightValue != null && rowHeightValue instanceof Number) {
               Number rowHeight = (Number) rowHeightValue;
               rowHeight = rowHeight.doubleValue() * scale;
               if (bestRowHeight == null || bestRowHeight.intValue() < rowHeight.intValue()) {
                  bestRowHeight = rowHeight;
               }
            }
            if (bestRowHeight != null) {
               bestRowHeight = bestRowHeight.doubleValue() * (4.0 / 3.0);
            }
            if (bestRowHeight != null && bestRowHeight.intValue() > 0) {
               UIManager.getLookAndFeel().getDefaults().put("Table.rowHeight", bestRowHeight.intValue());
            }
         } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            LOG.log(Level.SEVERE, "Cannot override menu font sizes!", e);
         }
      }
      try {
         setModel(new ShuffleModel(this));
         setView(new ShuffleView(this));
         getModel().checkLocaleConfig();
         loadFrame();
      } catch (Exception e) {
         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         e.printStackTrace(pw);
         LOG.log(Level.SEVERE, "Failure on start:", e);
      }
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.ModeIndicatorUser#scaleFont(java.awt.Font)
    */
   @Override
   public Font scaleFont(Font givenFont) {
      Font retFont = givenFont;
      Integer menuFontOverride = getPreferencesManager().getIntegerValue(KEY_FONT_SIZE_SCALING);
      if (menuFontOverride != null && menuFontOverride != 100 && menuFontOverride > 0 && menuFontOverride < 10000) {
         float scale = menuFontOverride.floatValue() / 100.0f;
         float adjustedSize = retFont.getSize2D() * scale;
         retFont = retFont.deriveFont(adjustedSize);
      }
      return retFont;
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.config.provider.ImageManagerProvider#scaleBorderThickness(int)
    */
   @Override
   public Integer scaleBorderThickness(int given) {
      Integer borderScale = getPreferencesManager().getIntegerValue(KEY_BORDER_SCALING);
      Integer ret = given;
      if (borderScale != null && borderScale != 100 && borderScale >= 1 && borderScale <= 10000) {
         float scale = borderScale.floatValue() * ret.floatValue() / 100.0f;
         ret = Math.round(scale);
      }
      return ret;
   }
   
   /**
    * Gets the ConfigFactory associated with this ShuffleController.
    * 
    * @return
    */
   @Override
   public ConfigFactory getConfigFactory() {
      return factory;
   }
   
   /**
    * Gets the ShuffleModel for this ShuffleController.
    * 
    * @return The ShuffleModel
    */
   public ShuffleModel getModel() {
      return model;
   }
   
   /**
    * Sets the ShuffleModel for this ShuffleController
    * 
    * @param model
    *           The ShuffleModel
    */
   public void setModel(ShuffleModel model) {
      this.model = model;
   }
   
   /**
    * Gets the ShuffleView for this ShuffleController.
    * 
    * @return The ShuffleView
    */
   public ShuffleView getView() {
      return view;
   }
   
   /**
    * Sets the ShuffleView for this ShuffleController.
    * 
    * @param view
    *           The ShuffleView.
    */
   public void setView(ShuffleView view) {
      this.view = view;
   }
   
   // Load/Save menu options
   /**
    * Loads all data from configurations.
    */
   @Override
   public void loadAll() {
      LOG.info(getString(KEY_LOAD_ALL));
      if (getModel().loadAllData()) {
         getModel().setDataChanged();
         repaint();
      }
   }
   
   @Override
   public void saveAll() {
      LOG.info(getString(KEY_SAVE_ALL));
      getModel().saveAllData();
      LOG.info(getString(KEY_SAVE_ALL_SUCCESS));
   }
   
   /**
    * Loads all Roster data from configuration.
    */
   @Override
   public void loadRoster() {
      RosterManager manager = getModel().getRosterManager();
      if (manager.loadFromConfig()) {
         LOG.info(getString(KEY_LOAD_ROSTER));
         getModel().setDataChanged();
         repaint();
      }
   }
   
   /**
    * Saves all Roster data to configuration.
    */
   @Override
   public void saveRoster() {
      RosterManager manager = getModel().getRosterManager();
      LOG.info(getString(KEY_SAVE_ROSTER));
      manager.saveDataToConfig();
      LOG.info(getString(KEY_SAVE_ROSTER_SUCCESS));
   }
   
   /**
    * Loads all Teams data from configuration.
    */
   @Override
   public void loadTeams() {
      TeamManager manager = getModel().getTeamManager();
      if (manager.loadFromConfig()) {
         LOG.info(getString(KEY_LOAD_TEAM));
         getModel().setDataChanged();
         repaint();
      }
   }
   
   /**
    * Saves all Teams data to configuration.
    */
   @Override
   public void saveTeams() {
      TeamManager manager = getModel().getTeamManager();
      LOG.info(getString(KEY_SAVE_TEAM));
      manager.saveDataToConfig();
      LOG.info(getString(KEY_SAVE_TEAM_SUCCESS));
   }
   
   /**
    * Clears the current board.
    */
   @Override
   public void clearGrid() {
      if (getModel().clearBoard()) {
         LOG.info(getString(KEY_CLEAR_GRID));
         getModel().setDataChanged();
         repaint();
      }
   }
   
   /**
    * Loads the board from configuration.
    */
   @Override
   public void loadGrid() {
      if (getModel().loadBoard()) {
         LOG.info(getString(KEY_LOAD_GRID));
         getModel().setDataChanged();
         repaint();
      }
   }
   
   /**
    * Loads the default board for the current stage, from configuration.
    */
   @Override
   public void loadDefaultGrid() {
      if (getModel().loadDefaultBoard()) {
         LOG.info(getString(KEY_LOAD_DEFAULT_GRID));
         getModel().setDataChanged();
         repaint();
      }
   }
   
   /**
    * Saves the board to configuration.
    */
   @Override
   public void saveGrid() {
      LOG.info(getString(KEY_SAVE_GRID));
      getModel().saveBoard();
      LOG.info(getString(KEY_SAVE_GRID_SUCCESS));
   }
   
   @Override
   public void doSelectedMove() {
      if (getModel().doSelectedMove()) {
         LOG.info(getString(KEY_DO_MOVE));
         if (getModel().isSwapToPaint()) {
            getModel().setCurrentEntryMode(EntryMode.PAINT);
         }
         getModel().setCursorTo(1, 1);
         repaint();
         getFrame().toFront();
      }
   }
   
   @Override
   public void undoMove() {
      if (getModel().undoMove()) {
         LOG.info(getString(KEY_UNDO_MOVE));
         repaint();
      }
   }
   
   @Override
   public void redoMove() {
      if (getModel().redoMove()) {
         LOG.info(getString(KEY_REDO_MOVE));
         repaint();
      }
   }
   
   /**
    * Returns the ShuffleFrame for this view.
    * 
    * @return The ShuffleFrame
    */
   public ShuffleFrame getFrame() {
      if (frame == null) {
         frame = new ShuffleFrame(this);
      }
      return frame;
   }
   
   public void loadFrame() {
      if (frame == null) {
         frame = new ShuffleFrame(this);
      }
   }
   
   @Override
   public void changeMode() {
      EntryMode next = getModel().getCurrentEntryMode().getNextMode();
      if (getModel().setCurrentEntryMode(next)) {
         repaint();
      }
   }
   
   @Override
   public ImageManager getImageManager() {
      return getConfigFactory().getImageManager();
   }
   
   @Override
   public void toggleFrozen() {
      if (getModel().toggleFrozenPaints()) {
         repaint();
      }
   }
   
   @Override
   public void setSelectedSpecies(Species toPaint) {
      if (getModel().setSelectedSpecies(toPaint)) {
         repaint();
      }
   }
   
   @Override
   public void setCursorTo(int row, int col) {
      if (getModel().setCursorTo(row, col)) {
         repaint();
      }
   }
   
   @Override
   public void paintAt(SpeciesPaint paint, int row, int col) {
      if (getModel().paintAt(row, col, paint)) {
         getModel().setDataChanged();
         repaint();
      }
   }
   
   @Override
   public void toggleFrozenAt(Integer row, Integer column) {
      boolean prevFrozen = getModel().getBoard().isFrozenAt(row, column);
      if (getModel().paintAt(row, column, null, !prevFrozen)) {
         getModel().setDataChanged();
         repaint();
      }
   }
   
   @Override
   public void advanceCursorBy(int i) {
      if (getModel().advanceCursorBy(i)) {
         repaint();
      }
   }
   
   @Override
   public void setCurrentEntryMode(EntryMode mode) {
      if (getModel().setCurrentEntryMode(mode)) {
         repaint();
      }
   }
   
   @Override
   public void setCurrentStage(Stage newStage) {
      if (getModel().setCurrentStage(newStage)) {
         getModel().setDataChanged();
         repaint();
      }
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.StageIndicatorUser#setEscalationLevel(java.lang.Integer)
    */
   @Override
   public void setEscalationLevel(Integer level) {
      if (getModel().setEscalationLevel(level)) {
         getModel().setDataChanged();
         repaint();
      }
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.StageIndicatorUser#canLevelEscalation()
    */
   @Override
   public boolean canLevelEscalation() {
      return true;
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.StageIndicatorUser#getEscalationLevel()
    */
   @Override
   public Integer getEscalationLevel() {
      return getModel().getEscalationLevel();
   }
   
   @Override
   public String getTitle() {
      return ShuffleVersion.VERSION_FULL;
   }
   
   @Override
   public String getTextFor(Object value) {
      return getView().getTextFor(value);
   }
   
   @Override
   public Stage getCurrentStage() {
      return getModel().getCurrentStage();
   }
   
   @Override
   public SpeciesPaint getSelectedSpeciesPaint() {
      return getModel().getCurrentSpeciesPaint();
   }
   
   @Override
   public List<SpeciesPaint> getCurrentPaints() {
      return getModel().getCurrentPaints();
   }
   
   // Simulation User methods
   
   @Override
   public int getMegaProgress() {
      return getModel().getMegaProgress();
   }
   
   @Override
   public void setMegaProgress(int progress) {
      if (getModel().setMegaProgress(progress)) {
         getModel().setDataChanged();
         repaint();
      }
   }
   
   @Override
   public boolean isMegaAllowed() {
      return getModel().isMegaAllowed();
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.data.simulation.util.SimulationAcceptor#acceptResults(java.util.Collection)
    */
   @Override
   public void acceptResults(Collection<SimulationResult> results) {
      if (getModel().setBestResults(results)) {
         repaint();
      }
   }
   
   @Override
   public UUID getAcceptedId() {
      return getModel().getAcceptedId();
   }
   
   @Override
   public int getPreferredNumFeeders() {
      return getModel().getNumFeeders();
   }
   
   @Override
   public int getPreferredFeederHeight() {
      return getModel().getFeederHeight();
   }
   
   @Override
   public void computeNow() {
      LOG.info(getString(KEY_COMPUTE_NOW));
      getModel().computeNow();
      repaint();
   }
   
   @Override
   public boolean isAutoCompute() {
      return getModel().getAutoCompute();
   }
   
   @Override
   public void setAutoCompute(boolean autoCompute) {
      if (getModel().setAutoCompute(autoCompute)) {
         if (autoCompute) {
            LOG.info(getString(KEY_COMPUTE_AUTO_TRUE));
         } else {
            LOG.info(getString(KEY_COMPUTE_AUTO_FALSE));
         }
         getModel().setDataChanged();
         repaint();
      }
   }
   
   @Override
   public void reportBug(final String givenMessage) {
      SwingUtilities.invokeLater(new Runnable() {
         @Override
         public void run() {
            StringBuilder sb = new StringBuilder();
            sb.append("MESSAGE READS:\r\n");
            sb.append(givenMessage);
            sb.append("\r\nEND OF MESSAGE");
            sb.append("\r\n\r\n");
            sb.append("Selected result:");
            SimulationResult curResult = getModel().getCurrentResult();
            if (curResult == null) {
               sb.append(" No result selected.");
            } else {
               sb.append("\r\n");
               sb.append(curResult.toString());
            }
            sb.append("\r\n\r\n");
            sb.append("Current results:");
            Collection<SimulationResult> results = getModel().getResults();
            if (results == null) {
               sb.append("No results.");
            } else {
               for (SimulationResult result : results) {
                  if (result != null) {
                     sb.append("\r\n");
                     sb.append(result.toString());
                  }
               }
            }
            sb.append("\r\n\r\n");
            sb.append("Current running windows:");
            List<String> info = new ArrayList<String>();
            info.add(getFrame().toString());
            Collection<JDialog> serviceDialogs = BaseServiceManager.getAllDialogs();
            for (JDialog d : serviceDialogs) {
               info.add(d.toString());
            }
            for (String s : info) {
               sb.append("\r\n\r\n");
               sb.append(s);
            }
            getModel().saveAllData();
            getModel().reportBug(sb.toString());
         }
      });
   }
   
   @Override
   public void repaint() {
      if (SwingUtilities.isEventDispatchThread()) {
         setChanged();
         notifyObservers();
         getFrame().repaint();
      } else {
         SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
               setChanged();
               notifyObservers();
               getFrame().repaint();
            }
         });
      }
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.service.update.UpdateServiceUser#getCurrentVersion()
    */
   @Override
   public String getCurrentVersion() {
      return ShuffleVersion.VERSION_FULL;
   }
   
   public void setMegaActive(boolean active) {
      if (getModel().setMegaActive(active)) {
         getModel().setDataChanged();
         repaint();
      }
   }
   
   /**
	 * 
	 */
   @Override
   public void toggleActiveMega() {
      Team team = getCurrentTeam();
      String megaSlotName = team.getMegaSlotName();
      if (megaSlotName != null) {
         boolean wasActive = getModel().isMegaSlotActive();
         setMegaActive(!wasActive);
      }
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.data.simulation.SimulationUser#getSpeciesManager()
    */
   @Override
   public SpeciesManager getSpeciesManager() {
      return getModel().getSpeciesManager();
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.StageIndicatorUser#getAllStages()
    */
   @Override
   public Collection<Stage> getAllStages() {
      return getModel().getStageManager().getAllStages();
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.config.provider.RosterManagerProvider#getRosterManager()
    */
   @Override
   public RosterManager getRosterManager() {
      return getModel().getRosterManager();
   }
   
   /*
    * (non-Javadoc)
    * @see
    * shuffle.fwk.service.roster.EditRosterServiceUser#loadFromRosterManager(shuffle.fwk.config.
    * manager.RosterManager)
    */
   @Override
   public void loadFromRosterManager(RosterManager manager) {
      RosterManager curManager = getModel().getRosterManager();
      if (curManager.copyFromManager(manager)) {
         LOG.info(getString(KEY_ROSTER_CHANGED));
         getModel().setDataChanged();
         repaint();
      }
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.config.provider.TeamManagerProvider#getTeamManager()
    */
   @Override
   public TeamManager getTeamManager() {
      return getModel().getTeamManager();
   }
   
   @Override
   public EffectManager getEffectManager() {
      return getModel().getEffectManager();
   }
   
   @Override
   public GradingModeManager getGradingModeManager() {
      return getModel().getGradingModeManager();
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.config.provider.BoardManagerProvider#getBoardManager()
    */
   @Override
   public BoardManager getBoardManager() {
      return getModel().getBoardManager();
   }
   
   @Override
   public EntryModeManager getEntryModeManager() {
      return getModel().getEntryModeManager();
   }
   
   @Override
   public EntryMode getCurrentEntryMode() {
      return getEntryModeManager().getCurrentEntryMode();
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.EntryModeUser#getCurrentCursor()
    */
   @Override
   public List<Integer> getCurrentCursor() {
      return getModel().getCurrentCursor();
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.EntryModeUser#getCurrentSpeciesPaint()
    */
   @Override
   public SpeciesPaint getCurrentSpeciesPaint() {
      return getModel().getCurrentSpeciesPaint();
   }
   
   @Override
   public Collection<Species> getCurrentSpecies() {
      return getModel().getCurrentSpecies();
   }

   /*
    * (non-Javadoc)
    * @see shuffle.fwk.EntryModeUser#getPreviousCursor()
    */
   @Override
   public List<Integer> getPreviousCursor() {
      return getModel().getPreviousCursor();
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.data.simulation.util.SimulationResultProvider#getResult()
    */
   @Override
   public SimulationResult getSelectedResult() {
      return getModel().getCurrentResult();
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.data.simulation.util.SimulationResultProvider#getResults()
    */
   @Override
   public Collection<SimulationResult> getResults() {
      return getModel().getResults();
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.GridPanelUser#getPaintAt(java.lang.Integer, java.lang.Integer)
    */
   @Override
   public SpeciesPaint getPaintAt(Integer row, Integer col) {
      return getModel().getPaintAt(row, col);
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.service.textdisplay.TextDisplayServiceUser#getPathManager()
    */
   @Override
   public ConfigManager getPathManager() {
      return getConfigFactory().getPathManager();
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.service.textdisplay.TextDisplayServiceUser#getPreferencesManager()
    */
   @Override
   public ConfigManager getPreferencesManager() {
      return getConfigFactory().getPreferencesManager();
   }
   
   /*
    * (non-Javadoc)
    * @see
    * shuffle.fwk.service.teams.EditTeamServiceUser#loadFromTeamManager(shuffle.fwk.config.manager
    * .TeamManager)
    */
   @Override
   public void loadFromTeamManager(TeamManager manager) {
      TeamManager curManager = getModel().getTeamManager();
      if (curManager.copyFromManager(manager)) {
         LOG.info(getString(KEY_TEAM_CHANGED));
         getModel().setDataChanged();
         repaint();
      }
   }
   
   /*
    * (non-Javadoc)
    * @see
    * shuffle.fwk.service.editspecies.EditSpeciesServiceUser#loadSpeciesManagerFrom(shuffle.fwk.
    * config.manager.SpeciesManager)
    */
   @Override
   public void loadSpeciesManagerFrom(SpeciesManager manager) {
      SpeciesManager curManager = getModel().getSpeciesManager();
      if (curManager.copyFromManager(manager)) {
         LOG.info(getString(KEY_SPECIES_CHANGED));
         getModel().setDataChanged();
         repaint();
      }
   }
   
   /*
    * (non-Javadoc)
    * @see
    * shuffle.fwk.service.editspecies.EditSpeciesServiceUser#loadImageManagerFrom(shuffle.fwk.config
    * .manager.ImageManager)
    */
   @Override
   public void loadImageManagerFrom(ImageManager manager) {
      ImageManager curManager = getView().getImageManager();
      if (curManager.copyFromManager(manager)) {
         LOG.info(getString(KEY_IMAGES_CHANGED));
         curManager.reloadIcons();
         repaint();
      }
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.GradingModeUser#getCurrentGradingMode()
    */
   @Override
   public GradingMode getCurrentGradingMode() {
      return getModel().getCurrentGradingMode();
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.GradingModeUser#setGradingMode(shuffle.fwk.GradingMode)
    */
   @Override
   public void setGradingMode(GradingMode mode) {
      if (getModel().setGradingMode(mode)) {
         LOG.info(getString(KEY_GRADING_CHANGED));
         repaint();
      }
   }
   
   /*
    * (non-Javadoc)
    * @see
    * shuffle.fwk.service.movechooser.MoveChooserServiceUser#setSelectedResult(shuffle.fwk.data.
    * simulation.SimulationResult)
    */
   @Override
   public void setSelectedResult(SimulationResult result) {
      if (getModel().setSelectedResult(result)) {
         repaint();
      }
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.ShuffleMenuUser#setLocaleTo(java.util.Locale)
    */
   @Override
   public void setLocaleTo(Locale loc) {
      if (getModel().setLocaleTo(loc)) {
         repaint();
         getFrame().updateMinimumSize();
         getFrame().pack();
      }
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.PaintsIndicatorUser#setTeamForStage(shuffle.fwk.data.Team,
    * shuffle.fwk.data.Stage)
    */
   @Override
   public void setTeamForStage(Team team, Stage stage) {
      if (getModel().getTeamManager().setTeamForStage(team, stage)) {
         getModel().setDataChanged();
         repaint();
      }
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.PaintsIndicatorUser#setFrozenState(boolean)
    */
   @Override
   public void setPaintsFrozen(boolean selected) {
      boolean prev = getModel().arePaintsFrozen();
      if (prev != selected && getModel().toggleFrozenPaints()) {
         repaint();
      }
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.PaintsIndicatorUser#getFrozenState()
    */
   @Override
   public boolean getFrozenState() {
      return getModel().arePaintsFrozen();
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.data.simulation.SimulationUser#getRemainingMoves()
    */
   @Override
   public int getRemainingMoves() {
      return getModel().getRemainingMoves();
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.data.simulation.SimulationUser#getRemainingHealth()
    */
   @Override
   public int getRemainingHealth() {
      return getModel().getRemainingHealth();
   }
   
   @Override
   public void setCurrentScore(int score) {
      if (getModel().setCurrentScore(score)) {
         getModel().setDataChanged();
         repaint();
      }
   }
   
   @Override
   public int getCurrentScore() {
      return getModel().getCurrentScore();
   }

   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.PaintsIndicatorUser#setRemainingMoves(int)
    */
   @Override
   public void setRemainingMoves(int moves) {
      if (getModel().setRemainingMoves(moves)) {
         getModel().setDataChanged();
         repaint();
      }
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.service.movepreferences.MovePreferencesServiceUser#getDisabledEffects()
    */
   @Override
   public Collection<Effect> getDisabledEffects() {
      return getModel().getDisabledEffects();
   }

   /*
    * (non-Javadoc)
    * @see shuffle.fwk.service.movepreferences.MovePreferencesServiceUser#getAttackPowerUp()
    */
   @Override
   public boolean getAttackPowerUp() {
      return getModel().getAttackPowerUp();
   }
   
   @Override
   public int getEffectThreshold() {
      return getModel().getEffectThreshold();
   }

   /*
    * (non-Javadoc)
    * @see
    * shuffle.fwk.service.movepreferences.MovePreferencesServiceUser#applyMovePreferences(shuffle
    * .fwk.service.movepreferences.MovePreferencesService)
    */
   @Override
   public void applyMovePreferences(MovePreferencesService service) {
      int numFeeders = service.getNumFeeders();
      int feederHeight = service.getFeederHeight();
      boolean autoCompute = service.isAutoCompute();
      boolean swapToPaint = service.isSwapToPaint();
      Collection<Effect> disabledEffects = service.getDisabledEffects();
      int threshold = service.getThreshold();
      boolean mobileMode = service.isMobileMode();
      boolean expressMetal = service.isExpressMetalAdvanceEnabled();
      boolean extendedMetal = service.isExtendedMetalEnabled();
      
      boolean changed = false;
      // These DO affect simulation results.
      changed |= getModel().setFeederPreferences(numFeeders, feederHeight, autoCompute);
      changed |= getModel().setDisabledEffects(disabledEffects);
      changed |= getModel().setEffectThreshold(threshold);
      changed |= getModel().setMobileMode(mobileMode);
      boolean teamChanged = getModel().setMetalExtended(extendedMetal);
      
      if (changed) {
         getModel().setDataChanged();
      }
      if (changed || teamChanged) {
         repaint();
      }
      // This doesn't affect simulation results.
      getModel().setSwapToPaint(swapToPaint);
      getModel().setExpressMetalAdvanceEnabled(expressMetal);
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.PaintsIndicatorUser#setAttackPowerUp(boolean)
    */
   @Override
   public void setAttackPowerUp(boolean enabled) {
      if (getModel().setAttackPowerUp(enabled)) {
         getModel().setDataChanged();
         repaint();
      }
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.ShuffleMenuUser#fillGrid()
    */
   @Override
   public void fillGrid() {
      if (getModel().fillGrid()) {
         getModel().setDataChanged();
         repaint();
      }
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.service.saveprompt.SavePromptServiceUser#shouldPromptSave()
    */
   @Override
   public boolean shouldPromptSave() {
      return factory.isDataChanged();
   }
   
   @Override
   public boolean isSwapToPaint() {
      return getModel().isSwapToPaint();
   }

   /*
    * (non-Javadoc)
    * @see shuffle.fwk.service.movepreferences.MovePreferencesServiceUser#isMobileMode()
    */
   @Override
   public boolean isMobileMode() {
      return factory.isMobileMode();
   }
   
   /*
    * (non-Javadoc)
    * @see
    * shuffle.fwk.service.movepreferences.MovePreferencesServiceUser#isExpressMetalAdvanceEnabled()
    */
   @Override
   public boolean isExpressMetalAdvanceEnabled() {
      return getModel().isExpressMetalAdvanceEnabled();
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.service.teams.EditTeamServiceUser#isMetalExtended()
    */
   @Override
   public boolean isExtendedMetalEnabled() {
      return getModel().isExtendedMetalEnabled();
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.service.teams.EditTeamServiceUser#isSurvival()
    */
   @Override
   public boolean isSurvival() {
      return getModel().isSurvivalMode();
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.service.teams.EditTeamServiceUser#setSurvival(boolean)
    */
   @Override
   public boolean setSurvival(boolean enabled) {
      boolean changed = getModel().setSurvivalMode(enabled);
      if (changed) {
         getModel().setDataChanged();
         repaint();
      }
      return changed;
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.ShuffleViewUser#getCurrentTeam()
    */
   @Override
   public Team getCurrentTeam() {
      return getModel().getCurrentTeam();
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.PaintsIndicatorUser#getStatus()
    */
   @Override
   public Status getStatus() {
      return getModel().getStatus();
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.PaintsIndicatorUser#getStatusDuration()
    */
   @Override
   public int getStatusDuration() {
      return getModel().getStatusDuration();
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.PaintsIndicatorUser#setStatus(shuffle.fwk.data.Board.Status)
    */
   @Override
   public boolean setStatus(Status status) {
      boolean changed = getModel().setStatus(status);
      if (changed) {
         getModel().setDataChanged();
         repaint();
      }
      return changed;
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.PaintsIndicatorUser#setStatusDuration(int)
    */
   @Override
   public boolean setStatusDuration(int duration) {
      boolean changed = getModel().setStatusDuration(duration);
      if (changed) {
         getModel().setDataChanged();
         repaint();
      }
      return changed;
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.ShuffleMenuUser#printGrid()
    */
   @Override
   public void printGrid() {
      GridPanel.printGrid(this);
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.ShuffleMenuUser#setGridPrintGridEnabled(boolean)
    */
   @Override
   public void setGridPrintGridEnabled(boolean enable) {
      getPreferencesManager().setEntry(EntryType.BOOLEAN, GridPanel.KEY_PRINT_INCLUDE_GRID, enable);
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.ShuffleMenuUser#setGridPrintMoveEnabled(boolean)
    */
   @Override
   public void setGridPrintMoveEnabled(boolean enable) {
      getPreferencesManager().setEntry(EntryType.BOOLEAN, GridPanel.KEY_PRINT_INCLUDE_MOVE, enable);
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.ShuffleMenuUser#setGridPrintCursorEnabled(boolean)
    */
   @Override
   public void setGridPrintCursorEnabled(boolean enable) {
      getPreferencesManager().setEntry(EntryType.BOOLEAN, GridPanel.KEY_PRINT_INCLUDE_CURSOR, enable);
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.GridPanelUser#isGridPrintMoveEnabled()
    */
   @Override
   public boolean isGridPrintMoveEnabled() {
      return getPreferencesManager().getBooleanValue(GridPanel.KEY_PRINT_INCLUDE_MOVE,
            GridPanel.DEFAULT_PRINT_INCLUDE_MOVE);
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.GridPanelUser#isGridPrintCursorEnabled()
    */
   @Override
   public boolean isGridPrintCursorEnabled() {
      return getPreferencesManager().getBooleanValue(GridPanel.KEY_PRINT_INCLUDE_CURSOR,
            GridPanel.DEFAULT_PRINT_INCLUDE_CURSOR);
   }
   
   /*
    * (non-Javadoc)
    * @see shuffle.fwk.gui.user.GridPanelUser#isGridPrintGridEnabled()
    */
   @Override
   public boolean isGridPrintGridEnabled() {
      return getPreferencesManager().getBooleanValue(GridPanel.KEY_PRINT_INCLUDE_GRID,
            GridPanel.DEFAULT_PRINT_INCLUDE_GRID);
   }
   
}
