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

package shuffle.fwk.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import shuffle.fwk.config.ConfigManager;
import shuffle.fwk.config.manager.SpeciesManager;
import shuffle.fwk.data.Species;
import shuffle.fwk.data.SpeciesPaint;
import shuffle.fwk.data.Stage;
import shuffle.fwk.data.Team;
import shuffle.fwk.data.TeamImpl;
import shuffle.fwk.gui.user.PaintsIndicatorUser;
import shuffle.fwk.gui.user.ShuffleFrameUser;
import shuffle.fwk.i18n.I18nUser;

/**
 * @author Andrew Meyers
 *         
 */
@SuppressWarnings("serial")
public class PaintPalletPanel extends JPanel implements I18nUser {
   
   // i18n Keys
   private static final String KEY_WOOD = "text.wood";
   private static final String KEY_METAL = "text.metal";
   private static final String KEY_COIN = "text.coin";
   private static final String KEY_MEGA = "text.mega";
   private static final String KEY_FROZEN = "text.frozen";
   private static final String KEY_HEALTH = "text.health";
   private static final String KEY_SCORE = "text.score";
   private static final String KEY_MOVES = "text.moves";
   private static final String KEY_ATTACK_POWERUP = "text.attack.power.up";
   private static final String KEY_MEGACHECK_TOOLTIP = "tooltip.megacheck";
   private static final String KEY_MEGAPROGRESS_TOOLTIP = "tooltip.megaprogress";
   private static final String KEY_FROZEN_TOOLTIP = "tooltip.frozen";
   private static final String KEY_WOOD_TOOLTIP = "tooltip.wood";
   private static final String KEY_METAL_TOOLTIP = "tooltip.metal";
   private static final String KEY_COIN_TOOLTIP = "tooltip.coin";
   private static final String KEY_SCORE_TOOLTIP = "tooltip.score";
   private static final String KEY_HP_TOOLTIP = "tooltip.hp";
   private static final String KEY_MOVES_TOOLTIP = "tooltip.moves";
   private static final String KEY_ATTACK_POWERUP_TOOLTIP = "tooltip.attack.power.up";
   
   // config keys
   private static final String KEY_PAINT_SELECT_COLOR = "PAINT_SELECT_COLOR";
   private static final String KEY_SELECT_PAINT_THICK = "SELECT_PAINT_THICK";
   private static final String KEY_OUTLINE_PAINT_THICK = "OUTLINE_PAINT_THICK";
   
   // defaults
   private static final int DEFAULT_SELECT_THICK = 2;
   private static final int DEFAULT_OUTLINE_THICK = 1;
   
   // data & references
   private PaintsIndicatorUser user;
   private Set<Indicator<SpeciesPaint>> indicators = new HashSet<Indicator<SpeciesPaint>>();
   private JPanel contentPanel;
   private JScrollPane jsp;
   private JCheckBox megaActive;
   private JComboBox<Integer> megaProgress;
   private JCheckBox coinBox;
   private JCheckBox frozenBox;
   private JCheckBox woodBox;
   private JCheckBox metalBox;
   private JLabel movesLabel;
   private JComboBox<Integer> movesLeft;
   private JLabel scoreLabel;
   private JSpinner scoreField;
   private JLabel healthLabel;
   private JCheckBox enableAttackPowerUpBox;
   
   private ItemListener megaActiveListener;
   private ItemListener megaProgressListener;
   private ItemListener specialSpeciesListener;
   private ItemListener frozenStateListener;
   private ChangeListener scoreListener;
   private ItemListener movesListener;
   private ChangeListener attackPowerListener;
   private Supplier<Integer> minimumWidthGetter = () -> 0;
   
   private List<SpeciesPaint> prevPaints = Collections.emptyList();
   private Team prevTeam = null;
   private SpeciesPaint prevPaint = null;
   
   /**
    * Creates a Paint Pallet Panel for the given user with the width as adjusted by function.
    * 
    * @param user
    *           The User for this Paint Pallet
    * @param function
    *           A function that converts a dimension to one that represents the appropriate width
    *           for this panel
    */
   public PaintPalletPanel(ShuffleFrameUser user, Function<Dimension, Dimension> function) {
      super(new GridBagLayout());
      this.user = user;
      JPanel optionPanel = new JPanel(new WrapLayout()) {
         @Override
         public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            d.width = minimumWidthGetter.get();
            int height = d.height;
            d = function.apply(d); // allows us to grab the same width as the scroll pane, since we
                                   // can't have a circular reference
            d.height = height;
            return d;
         }
      };
      
      megaActive = new JCheckBox(getString(KEY_MEGA));
      megaActive.setToolTipText(getString(KEY_MEGACHECK_TOOLTIP));
      megaProgress = new JComboBox<Integer>();
      megaProgress.setToolTipText(getString(KEY_MEGAPROGRESS_TOOLTIP));
      JPanel megaPanel = new JPanel(new BorderLayout());
      megaPanel.add(megaActive, BorderLayout.WEST);
      megaPanel.add(megaProgress, BorderLayout.EAST);
      optionPanel.add(megaPanel);
      
      frozenBox = new JCheckBox(getString(KEY_FROZEN));
      frozenBox.setToolTipText(getString(KEY_FROZEN_TOOLTIP));
      optionPanel.add(frozenBox);
      woodBox = new JCheckBox(getString(KEY_WOOD));
      woodBox.setToolTipText(getString(KEY_WOOD_TOOLTIP));
      optionPanel.add(woodBox);
      metalBox = new JCheckBox(getString(KEY_METAL));
      metalBox.setToolTipText(getString(KEY_METAL_TOOLTIP));
      optionPanel.add(metalBox);
      coinBox = new JCheckBox(getString(KEY_COIN));
      coinBox.setToolTipText(getString(KEY_COIN_TOOLTIP));
      optionPanel.add(coinBox);
      
      scoreLabel = new JLabel(getString(KEY_SCORE));
      scoreLabel.setToolTipText(getString(KEY_SCORE_TOOLTIP));
      SpinnerNumberModel snm = new SpinnerNumberModel(0, 0, 99999, 50);
      scoreField = new JSpinner(snm);
      scoreField.setToolTipText(getString(KEY_SCORE_TOOLTIP));
      JPanel scorePanel = new JPanel(new BorderLayout());
      scorePanel.add(scoreLabel, BorderLayout.WEST);
      scorePanel.add(scoreField, BorderLayout.EAST);
      optionPanel.add(scorePanel);
      
      healthLabel = new JLabel(getString(KEY_HEALTH, getUser().getRemainingHealth()));
      healthLabel.setToolTipText(getString(KEY_HP_TOOLTIP));
      optionPanel.add(healthLabel);
      
      movesLabel = new JLabel(getString(KEY_MOVES));
      movesLabel.setToolTipText(getString(KEY_MOVES_TOOLTIP));
      movesLeft = new JComboBox<Integer>();
      movesLeft.setToolTipText(getString(KEY_MOVES_TOOLTIP));
      JPanel movesPanel = new JPanel(new BorderLayout());
      movesPanel.add(movesLabel, BorderLayout.WEST);
      movesPanel.add(movesLeft, BorderLayout.EAST);
      optionPanel.add(movesPanel);
      
      enableAttackPowerUpBox = new JCheckBox(getString(KEY_ATTACK_POWERUP));
      enableAttackPowerUpBox.setSelected(getUser().getAttackPowerUp());
      enableAttackPowerUpBox.setToolTipText(getString(KEY_ATTACK_POWERUP_TOOLTIP));
      optionPanel.add(enableAttackPowerUpBox);
      
      GridBagConstraints c = new GridBagConstraints();
      c.weightx = 0.0;
      c.weighty = 1.0;
      c.gridx = 1;
      c.gridy = 2;
      c.fill = GridBagConstraints.BOTH;
      add(optionPanel, c);
      minimumWidthGetter = new Supplier<Integer>() {
         @Override
         public Integer get() {
            int ret = 0;
            for (Component component : new Component[] { megaPanel, frozenBox, woodBox, metalBox, coinBox,
                  enableAttackPowerUpBox, scorePanel, healthLabel, movesPanel }) {
               ret = Math.max(ret, component.getPreferredSize().width);
            }
            return ret;
         }
      };
      jsp = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER) {
         @Override
         public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            d = function.apply(d);
            d.height -= optionPanel.getPreferredSize().height;
            return d;
         }
      };
      c.gridy = 1;
      c.weightx = 1.0;
      c.fill = GridBagConstraints.BOTH;
      add(jsp, c);
      contentPanel = new JPanel(new WrapLayout()) {
         // Fix to make it play nice with the scroll bar.
         @Override
         public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            d.width = function.apply(new Dimension()).width;
            d.width -= jsp.getVerticalScrollBar().getWidth();
            d.width -= jsp.getInsets().right + jsp.getInsets().left;
            return d;
         }
      };
      jsp.addComponentListener(new ComponentAdapter() {
         @Override
         public void componentResized(ComponentEvent e) {
            invalidate();
            validate();
         }
      });
      jsp.setViewportView(contentPanel);
      jsp.getVerticalScrollBar().setUnitIncrement(30);
      addOptionListeners();
      updateAll();
   }
   
   @Override
   public Dimension getPreferredSize() {
      Dimension d = super.getPreferredSize();
      d.width = minimumWidthGetter.get();
      return d;
   }
   
   private PaintsIndicatorUser getUser() {
      return user;
   }
   
   private void addOptionListeners() {
      if (megaActiveListener == null) {
         megaActiveListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
               Team curTeam = getUser().getTeamManager().getTeamForStage(getUser().getCurrentStage());
               int megaThreshold = curTeam.getMegaThreshold(getUser().getSpeciesManager(), getUser().getRosterManager(),
                     getUser().getEffectManager());
               getUser().setMegaProgress(megaActive.isSelected() ? megaThreshold : 0);
            }
         };
      }
      if (megaProgressListener == null) {
         megaProgressListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
               Integer progress = megaProgress.getItemAt(megaProgress.getSelectedIndex());
               getUser().setMegaProgress(progress == null ? 0 : progress.intValue());
            }
         };
      }
      if (specialSpeciesListener == null) {
         specialSpeciesListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
               Stage stage = getUser().getCurrentStage();
               TeamImpl curTeam = new TeamImpl(getUser().getTeamManager().getTeamForStage(stage));
               String woodName = Species.WOOD.getName();
               boolean hasWood = curTeam.getNames().contains(woodName);
               String metalName = Species.METAL.getName();
               boolean hasMetal = curTeam.getNames().contains(metalName);
               String coinName = Species.COIN.getName();
               boolean hasCoin = curTeam.getNames().contains(coinName);
               
               if (hasWood && !woodBox.isSelected()) {
                  curTeam.removeName(woodName);
               } else if (!hasWood && woodBox.isSelected()) {
                  curTeam.addName(woodName, getNextBindingFor(woodName, curTeam));
               }
               boolean metalSelected = metalBox.isSelected();
               if (hasMetal != metalSelected) {
                  boolean extendedMetalEnabled = getUser().isExtendedMetalEnabled();
                  getUser().getTeamManager().setMetalInTeam(curTeam, metalSelected, extendedMetalEnabled);
               }
               if (hasCoin && !coinBox.isSelected()) {
                  curTeam.removeName(coinName);
               } else if (!hasCoin && coinBox.isSelected()) {
                  curTeam.addName(coinName, getNextBindingFor(coinName, curTeam));
               }
               getUser().setTeamForStage(curTeam, stage);
            }
         };
      }
      if (frozenStateListener == null) {
         frozenStateListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
               getUser().setPaintsFrozen(frozenBox.isSelected());
            }
         };
      }
      if (scoreListener == null) {
         scoreListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
               getUser().setCurrentScore((int) scoreField.getModel().getValue());
            }
         };
      }
      if (movesListener == null) {
         movesListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
               getUser().setRemainingMoves(movesLeft.getItemAt(movesLeft.getSelectedIndex()));
            }
         };
      }
      if (attackPowerListener == null) {
         attackPowerListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
               getUser().setAttackPowerUp(enableAttackPowerUpBox.isSelected());
            }
         };
      }
      megaActive.addItemListener(megaActiveListener);
      megaProgress.addItemListener(megaProgressListener);
      woodBox.addItemListener(specialSpeciesListener);
      metalBox.addItemListener(specialSpeciesListener);
      coinBox.addItemListener(specialSpeciesListener);
      frozenBox.addItemListener(frozenStateListener);
      scoreField.addChangeListener(scoreListener);
      movesLeft.addItemListener(movesListener);
      enableAttackPowerUpBox.addChangeListener(attackPowerListener);
   }
   
   private char getNextBindingFor(String name, Team team) {
      return getUser().getTeamManager().getAllAvailableBindingsFor(name, team).iterator().next();
   }
   
   private void removeOptionListeners() {
      megaActive.removeItemListener(megaActiveListener);
      megaProgress.removeItemListener(megaProgressListener);
      woodBox.removeItemListener(specialSpeciesListener);
      metalBox.removeItemListener(specialSpeciesListener);
      coinBox.removeItemListener(specialSpeciesListener);
      frozenBox.removeItemListener(frozenStateListener);
      scoreField.removeChangeListener(scoreListener);
      movesLeft.removeItemListener(movesListener);
      enableAttackPowerUpBox.removeChangeListener(attackPowerListener);
   }
   
   /**
    * Updates all paints in the pallet. After this is run, it is guaranteed that all paints
    * displayed exist in the team, as valid species paints. Also, the selected paint should only be
    * the one recognized by the ShuffleView associated with this PaintPalletPanel.
    * 
    * @return True if and only if something changed which might require a pack of the parent.
    */
   public boolean updateAll() {
      updateIndicators();
      updateOptionsFromUser();
      return true;
   }
   
   /**
    * @param curPaint
    * @param curPaints
    */
   private void updateIndicators() {
      List<SpeciesPaint> curPaints = getUser().getCurrentPaints();
      SpeciesPaint curPaint = getUser().getSelectedSpeciesPaint();
      Team curTeam = getUser().getTeamManager().getTeamForStage(getUser().getCurrentStage());
      if (curPaints.equals(prevPaints) && (curTeam == prevTeam || curTeam != null && curTeam.equals(prevTeam))) {
         if (!(prevPaint == curPaint || prevPaint != null && prevPaint.equals(curPaint))) {
            updateIndicatorBorders();
            prevPaint = curPaint;
         }
         return;
      } else {
         prevPaints = curPaints;
         prevPaint = curPaint;
         prevTeam = curTeam;
      }
      removeAllIndicators();
      for (int i = 0; i < curPaints.size(); i++) {
         SpeciesPaint value = curPaints.get(i);
         String text = getUser().getTextFor(value);
         Indicator<SpeciesPaint> ind = new Indicator<SpeciesPaint>(getUser());
         ind.setVisualized(value, text);
         if (value.equals(curPaint)) { // border necessary
            setBorderFor(ind, true);
         } else { // buffer needed to keep a nice look
            setBorderFor(ind, false);
         }
         addIndicator(ind);
      }
      contentPanel.revalidate();
      addIndicatorListeners();
   }
   
   /**
    * 
    */
   private void updateIndicatorBorders() {
      SpeciesPaint curPaint = getUser().getSelectedSpeciesPaint();
      for (Indicator<SpeciesPaint> ind : indicators) {
         SpeciesPaint paint = ind.getValue();
         setBorderFor(ind, curPaint == paint || curPaint != null && curPaint.equals(paint));
      }
   }
   
   /**
    * 
    */
   private void updateOptionsFromUser() {
      removeOptionListeners();
      
      // Actual text
      String megaText = getString(KEY_MEGA);
      String frozenText = getString(KEY_FROZEN);
      String woodText = getString(KEY_WOOD);
      String metalText = getString(KEY_METAL);
      String coinText = getString(KEY_COIN);
      String healthText = getString(KEY_HEALTH, getUser().getRemainingHealth());
      String scoreText = getString(KEY_SCORE);
      String movesText = getString(KEY_MOVES);
      String attackText = getString(KEY_ATTACK_POWERUP);
      
      if (!megaText.equals(megaActive.getText())) {
         megaActive.setText(megaText);
      }
      if (!frozenText.equals(frozenBox.getText())) {
         frozenBox.setText(frozenText);
      }
      if (!woodText.equals(woodBox.getText())) {
         woodBox.setText(woodText);
      }
      if (!metalText.equals(metalBox.getText())) {
         metalBox.setText(metalText);
      }
      if (!coinText.equals(coinBox.getText())) {
         coinBox.setText(coinText);
      }
      if (!healthText.equals(healthLabel.getText())) {
         healthLabel.setText(healthText);
      }
      if (!scoreText.equals(scoreLabel.getText())) {
         scoreLabel.setText(scoreText);
      }
      if (!movesText.equals(movesLabel.getText())) {
         movesLabel.setText(movesText);
      }
      if (!attackText.equals(enableAttackPowerUpBox.getText())) {
         enableAttackPowerUpBox.setText(attackText);
      }
      
      // Tooltip text
      String megaCheckTT = getString(KEY_MEGACHECK_TOOLTIP);
      String megaProgressTT = getString(KEY_MEGAPROGRESS_TOOLTIP);
      String frozenTT = getString(KEY_FROZEN_TOOLTIP);
      String woodTT = getString(KEY_WOOD_TOOLTIP);
      String metalTT = getString(KEY_METAL_TOOLTIP);
      String coinTT = getString(KEY_COIN_TOOLTIP);
      String scoreTT = getString(KEY_SCORE_TOOLTIP);
      String hpTT = getString(KEY_HP_TOOLTIP);
      String movesTT = getString(KEY_MOVES_TOOLTIP);
      String atkTT = getString(KEY_ATTACK_POWERUP_TOOLTIP);
      
      if (!megaCheckTT.equals(megaActive.getToolTipText())) {
         megaActive.setToolTipText(megaCheckTT);
      }
      if (!megaProgressTT.equals(megaProgress.getToolTipText())) {
         megaProgress.setToolTipText(megaProgressTT);
      }
      if (!frozenTT.equals(frozenBox.getToolTipText())) {
         frozenBox.setToolTipText(frozenTT);
      }
      if (!woodTT.equals(woodBox.getToolTipText())) {
         woodBox.setToolTipText(woodTT);
      }
      if (!metalTT.equals(metalBox.getToolTipText())) {
         metalBox.setToolTipText(metalTT);
      }
      if (!coinTT.equals(coinBox.getToolTipText())) {
         coinBox.setToolTipText(coinTT);
      }
      if (!scoreTT.equals(scoreLabel.getToolTipText())) {
         scoreLabel.setToolTipText(scoreTT);
         scoreField.setToolTipText(scoreTT);
      }
      if (!hpTT.equals(healthLabel.getToolTipText())) {
         healthLabel.setToolTipText(hpTT);
      }
      if (!movesTT.equals(movesLabel.getToolTipText())) {
         movesLabel.setToolTipText(movesTT);
         movesLeft.setToolTipText(movesTT);
      }
      if (!atkTT.equals(enableAttackPowerUpBox.getToolTipText())) {
         enableAttackPowerUpBox.setToolTipText(atkTT);
      }
      
      Team curTeam = getUser().getTeamManager().getTeamForStage(getUser().getCurrentStage());
      SpeciesManager speciesManager = getUser().getSpeciesManager();
      String megaSlotName = curTeam.getMegaSlotName();
      Species megaSpecies = megaSlotName == null ? null : speciesManager.getSpeciesValue(megaSlotName);
      int megaThreshold = curTeam.getMegaThreshold(speciesManager, getUser().getRosterManager(),
            getUser().getEffectManager());
      if (megaSpecies == null || megaSpecies.getMegaName() == null || megaThreshold == Integer.MAX_VALUE) {
         // remove their states
         megaProgress.removeAllItems();
         megaActive.setSelected(false);
         // disable both the progress and the activate button
         megaProgress.setEnabled(false);
         megaActive.setEnabled(false);
      } else {
         // refresh the progress dropdown menu
         megaProgress.removeAllItems();
         int threshold = megaThreshold == Integer.MAX_VALUE ? 0 : megaThreshold;
         for (int i = 0; i <= threshold; i++) {
            megaProgress.addItem(i);
         }
         int progress = getUser().getMegaProgress();
         // update their states appropriately
         megaProgress.setSelectedItem(progress);
         megaActive.setSelected(megaThreshold == progress);
         // ensure they are editable
         megaProgress.setEnabled(true);
         megaActive.setEnabled(true);
      }
      
      boolean hasWood = curTeam.getNames().contains(Species.WOOD.getName());
      boolean hasMetal = curTeam.getNames().contains(Species.METAL.getName());
      boolean hasCoin = curTeam.getNames().contains(Species.COIN.getName());
      woodBox.setSelected(hasWood);
      metalBox.setSelected(hasMetal);
      coinBox.setSelected(hasCoin);
      
      frozenBox.setSelected(getUser().getFrozenState());
      
      scoreField.getModel().setValue(getUser().getCurrentScore());
      movesLeft.removeAllItems();
      for (int i = getUser().getCurrentStage().getMoves() + 5; i >= 1; i--) {
         movesLeft.addItem(i);
      }
      movesLeft.setSelectedItem(getUser().getRemainingMoves());
      
      enableAttackPowerUpBox.setSelected(getUser().getAttackPowerUp());
      
      addOptionListeners();
   }
   
   private void addIndicator(Indicator<SpeciesPaint> ind) {
      ind.setAlignmentX(LEFT_ALIGNMENT);
      ind.setAlignmentY(TOP_ALIGNMENT);
      contentPanel.add(ind);
      indicators.add(ind);
   }
   
   private void removeAllIndicators() {
      for (Indicator<SpeciesPaint> ind : indicators) {
         for (MouseListener ml : ind.getMouseListeners()) {
            ind.removeMouseListener(ml);
         }
         contentPanel.remove(ind);
      }
      indicators.clear();
   }
   
   private Color getPaintSelectColor() {
      return getUser().getPreferencesManager().getColorValue(KEY_PAINT_SELECT_COLOR, Color.BLACK);
   }
   
   private boolean setBorderFor(Indicator<SpeciesPaint> ind, boolean selected) {
      boolean changed = false;
      if (ind != null) {
         ConfigManager manager = getUser().getPreferencesManager();
         int thickness = manager.getIntegerValue(KEY_SELECT_PAINT_THICK, DEFAULT_SELECT_THICK);
         thickness = getUser().scaleBorderThickness(thickness);
         int outlineThick = manager.getIntegerValue(KEY_OUTLINE_PAINT_THICK, DEFAULT_OUTLINE_THICK);
         outlineThick = getUser().scaleBorderThickness(outlineThick);
         Border b;
         if (selected) {
            b = new LineBorder(getPaintSelectColor(), thickness);
         } else {
            b = new EmptyBorder(thickness, thickness, thickness, thickness);
         }
         Border greyOutline = new LineBorder(Color.gray, outlineThick);
         Border borderToSet = BorderFactory.createCompoundBorder(b, greyOutline);
         if (!borderToSet.equals(ind.getBorder())) {
            ind.setBorder(borderToSet);
            changed = true;
         }
      }
      return changed;
   }
   
   public void addIndicatorListeners() {
      for (Indicator<SpeciesPaint> ind : indicators) {
         addListenerForIndicator(ind);
      }
   }
   
   /**
    * @param ind
    */
   private void addListenerForIndicator(Indicator<SpeciesPaint> ind) {
      ind.addMouseListener(new PressOrClickMouseAdapter() {
         private void sendPress() {
            getUser().setSelectedSpecies(ind.getValue().getSpecies());
         }
         
         @Override
         protected void onLeft(MouseEvent e) {
            sendPress();
         }
         
         @Override
         protected void onRight(MouseEvent e) {
            sendPress();
         }
         
         @Override
         protected void onEnter() {
            // Do nothing
         }
      });
   }
   
}
