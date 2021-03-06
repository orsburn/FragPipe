package umich.msfragger.params.ptmshepherd;

import com.github.chhh.utils.StringUtils;
import com.github.chhh.utils.swing.UiCheck;
import com.github.chhh.utils.swing.UiSpinnerDouble;
import com.github.chhh.utils.swing.UiSpinnerInt;
import com.github.chhh.utils.swing.UiText;
import com.github.chhh.utils.swing.UiUtils.UiTextBuilder;
import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import net.java.balloontip.BalloonTip;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import umich.msfragger.messages.MessageLoadShepherdDefaults;
import umich.msfragger.messages.MessageSearchType;
import umich.msfragger.util.PropertiesUtils;
import umich.msfragger.util.SwingUtils;
import umich.msfragger.util.swing.FormEntry;
import umich.msfragger.util.swing.JPanelWithEnablement;

public class PtmshepherdJPanel extends JPanelWithEnablement {
  private static final Logger log = LoggerFactory.getLogger(PtmshepherdJPanel.class);

  public static final String PROP_threads = "threads";
  public static final String PROP_histo_bindivs = "histo_bindivs";
  public static final String PROP_histo_smoothbins = "histo_smoothbins";
  public static final String PROP_peakpicking_promRatio = "peakpicking_promRatio";
  public static final String PROP_peakpicking_width = "peakpicking_width";
  public static final String PROP_peakpicking_background = "peakpicking_background";
  public static final String PROP_peakpicking_topN = "peakpicking_topN";
  public static final String PROP_precursor_tol = "precursor_tol";
  public static final String PROP_spectra_ppmtol = "spectra_ppmtol";
  public static final String PROP_spectra_condPeaks = "spectra_condPeaks";
  public static final String PROP_spectra_condRatio = "spectra_condRatio";
  public static final String PROP_localization_background = "localization_background";
  public static final String PROP_output_extended = "output_extended";
  private String PROP_varmod_masses = "varmod_masses";

  private final List<BalloonTip> balloonTips = new ArrayList<>();
  private JCheckBox checkRun;
  private JPanel pContent;
  private JScrollPane scroll;
  private JPanel pPeakPicking;
  private JPanel pPrecursorSpectrum;
  private JPanel pTop;
  private UiText uiTextVarMods;


  public PtmshepherdJPanel() {
    initMore();
    initPostCreation();
    // register on the bus only after all the components have been created to avoid NPEs
    EventBus.getDefault().register(this);
  }

  private void initPostCreation() {
    this.addPropertyChangeListener("enabled", evt -> {
      log.debug("Shepherd panel property '{}' changed from '{}' to '{}'", evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
      boolean isSwitchToEnabled = (Boolean)evt.getNewValue() && !(Boolean)evt.getOldValue();
      log.debug("Shepherd panel is switching to enabled? : {}, !checkRun.isSelected() : {}", isSwitchToEnabled, !checkRun.isSelected());
      if (isSwitchToEnabled && !checkRun.isSelected()) {
        enablementMapping.put(pContent, false);
        updateEnabledStatus(pContent, false);
      }
    });
  }

  @Subscribe
  public void onMessageLoadShepherdDefaults(MessageLoadShepherdDefaults m) {
    log.debug("Got MessageLoadShepherdDefaults");
    if (m.doAskUser) {
      int answer = SwingUtils.showConfirmDialog(this, new JLabel("<html>Load PTMShepherd defaults?"));
      if (JOptionPane.OK_OPTION != answer) {
        log.debug("User cancelled Loading Shepherd defaults");
        return;
      }
    }

    try {
      Properties props = PropertiesUtils
          .loadPropertiesLocal(PtmshepherdParams.class, PtmshepherdParams.DEFAULT_PROPERTIES_FN);
      SwingUtils.valuesFromMap(this, PropertiesUtils.to(props));
    } catch (Exception e) {
      log.error("Error loading shepherd defaults", e);
      SwingUtils.showErrorDialog(e, this);
    }

  }

  @Subscribe
  public void onMessageSearchTypePtms(MessageSearchType m) {
    switch (m.type) {
      case open:
        checkRun.setSelected(true);
        break;
      case closed:
      case nonspecific:
        checkRun.setSelected(false);
        break;
    }
  }

  private void initMore() {

    this.setLayout(new BorderLayout());
//    this.setBorder(new EmptyBorder(0,0,0,0));
    this.setBorder(new TitledBorder("PTM Analysis"));

    // Top panel with run checkbox
    {
      // setting the insets allows the top panel to be shifted left of the options panel
      pTop = new JPanel(new MigLayout(new LC().insetsAll("0px")));

      checkRun = new UiCheck("Run PTMShepherd", null, true);
      checkRun.setName("ui.name.report.run-shepherd");
      checkRun.addActionListener(e -> {
        final boolean isSelected = checkRun.isSelected();
        enablementMapping.put(pContent, isSelected);
        updateEnabledStatus(pContent, isSelected);
      });
      pTop.add(checkRun, new CC().alignX("left"));
      JButton btnLoadDefaults = new JButton("Load PTMShepherd defaults");
      btnLoadDefaults.addActionListener((e) -> EventBus.getDefault().post(new MessageLoadShepherdDefaults(true)));
      pTop.add(btnLoadDefaults, new CC().alignX("left"));

      pTop.setBorder(new EmptyBorder(0,0,0,0));
      this.add(pTop, BorderLayout.NORTH);
    }

    // Main content panel - container
    {
      pContent = new JPanel(new MigLayout(new LC().fillX()));
      pContent.setBorder(new EmptyBorder(0,0,0,0));

      // when "Run Report" checkbox is switched, this panel can decide not to turn on,
      // if "Run Shepherd" checkbox is off
      pContent.addPropertyChangeListener("enabled", evt -> {
        log.debug("Shepherd pContent panel property '{}' changed from '{}' to '{}'", evt.getPropertyName(),
            evt.getOldValue(), evt.getNewValue());
        boolean newValue = (Boolean)evt.getNewValue();
        boolean isSwitchToEnabled = (Boolean) evt.getNewValue() && !(Boolean) evt.getOldValue();
        boolean pContentIsEnabled = newValue && checkRun.isSelected();
        log.debug("Shepherd pContent panel is switching to enabled? : {}, !checkRun.isSelected() : {}, final state should be: {}",
            isSwitchToEnabled, !checkRun.isSelected(), pContentIsEnabled);
        enablementMapping.put(pContent, pContentIsEnabled);
        updateEnabledStatus(pContent, pContentIsEnabled);
      });

//      scroll = new JScrollPane(pContent);
//      scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
//      scroll.getVerticalScrollBar().setUnitIncrement(16);
    }

    {
      pPeakPicking = new JPanel(new MigLayout(new LC()));
      //pPeakPicking.setBorder(new TitledBorder("PTMShepherd options"));
      pPeakPicking.setBorder(new EmptyBorder(0, 0, 0, 0));

      FormEntry feHistoSmoothBins = new FormEntry(PROP_histo_smoothbins, "Smoothing factor",
          new UiSpinnerInt(2, 0, 5, 1, 5),
          "<html>Histogram smoothing. 0 = No smoothing, 1 = smooth using +/-1 bin, etc.");
      FormEntry feLocBackground = new FormEntry(PROP_localization_background, "Localization background",
          new UiSpinnerInt(4, 1, 4, 1, 5));

      pPeakPicking.add(feHistoSmoothBins.label(), new CC().alignX("right"));
      pPeakPicking.add(feHistoSmoothBins.comp, new CC().alignX("left"));
      pPeakPicking.add(feLocBackground.label(), new CC().alignX("right"));
      pPeakPicking.add(feLocBackground.comp, new CC().alignX("left").wrap());

      UiSpinnerDouble uiSpinnerPromRatio = UiSpinnerDouble.builder(0.3,0.0,1.0, 0.1)
          .setFormat(new DecimalFormat("0.#")).setNumCols(5).create();
      FormEntry fePromRatio = new FormEntry(PROP_peakpicking_promRatio, "Prominence ratio", uiSpinnerPromRatio,
          "Ratio of peak prominence to total peak height.");

      UiSpinnerDouble uiSpinnerWidth = UiSpinnerDouble.builder(0.002, 0.0, 0.5, 0.001)
          .setFormat(new DecimalFormat("0.####")).setNumCols(5).create();
      FormEntry feWidth = new FormEntry(PROP_peakpicking_width, "Peak picking width (Da)", uiSpinnerWidth);
      FormEntry feExtendedOut = new FormEntry(PROP_output_extended, "not-shown",
          new UiCheck("Extended output", null, false),
          "<html>Write additional files with more detailed information.");

      pPeakPicking.add(fePromRatio.label(), new CC().alignX("right"));
      pPeakPicking.add(fePromRatio.comp, new CC().alignX("left"));
      pPeakPicking.add(feWidth.label(), new CC().alignX("right"));
      pPeakPicking.add(feWidth.comp, new CC().alignX("left"));
      pPeakPicking.add(feExtendedOut.comp, new CC().alignX("left").pushX().wrap());


      uiTextVarMods = new UiTextBuilder().create();
      uiTextVarMods.setGhostText("Phospho:79.9663, Something-else:-20.123");
      uiTextVarMods.addFocusListener(new FocusAdapter() {
        @Override
        public void focusLost(FocusEvent e) {
          super.focusLost(e);
          PtmshepherdJPanel.this.validateForm();
        }

        @Override
        public void focusGained(FocusEvent e) {
          super.focusGained(e);
          PtmshepherdJPanel.this.clearBalloonTips();
        }
      });

      FormEntry feVarMods = new FormEntry(PROP_varmod_masses, "Custom mass shifts", uiTextVarMods,
          "<html>Variable modification masses.<br/>\n"
              + "Comma separated entries of form \"&lt;name&gt;:&lt;mass&gt;\"<br/>\n"
              + "Example:<br/>\n"
              + "&nbsp;&nbsp;&nbsp;&nbsp;Phospho:79.9663, Something-else:-20.123");
      pPeakPicking.add(feVarMods.label(), new CC().alignX("right"));
      pPeakPicking.add(feVarMods.comp, new CC().alignX("left").spanX().growX());

      // these are valid shepherd parameters, but not displayed in the UI anymore

//      FormEntry feHistoBinDivs = new FormEntry(PROP_histo_bindivs, "Histogram bins",
//          new UiSpinnerInt(5000, 10, 1000000, 100, 5));
//
//      pPeakPicking.add(feHistoBinDivs.label(), new CC().alignX("right"));
//      pPeakPicking.add(feHistoBinDivs.comp, new CC());
//
//      UiSpinnerDouble uiSpinnerBackground = UiSpinnerDouble.builder(0.005, 0.0, 1e6, 0.001)
//          .setFormat(new DecimalFormat("0.####")).setNumCols(5).create();
//      FormEntry feBackground = new FormEntry(PROP_peakpicking_background, "Peak-picking background", uiSpinnerBackground);
//
//      UiSpinnerInt uiSpinnerTopN = new UiSpinnerInt(500, 1, 1000000, 50);
//      uiSpinnerTopN.setColumns(5);
//      FormEntry feTopN = new FormEntry(PROP_peakpicking_topN, "Peak-picking Top-N", uiSpinnerTopN);

//      pPeakPicking.add(feBackground.label(), new CC().alignX("right"));
//      pPeakPicking.add(feBackground.comp, new CC());
//      pPeakPicking.add(feTopN.label(), new CC().alignX("right"));
//      pPeakPicking.add(feTopN.comp, new CC().wrap());

//      UiSpinnerDouble uiSpinnerPrecTol = UiSpinnerDouble.builder(0.01, 0.001, 1e6, 0.01)
//          .setFormat(new DecimalFormat("0.###")).setNumCols(5).create();
//      uiSpinnerPrecTol.setColumns(5);
//      FormEntry fePrecTol = new FormEntry(PROP_precursor_tol, "Precursor tolerance", uiSpinnerPrecTol);
//
//      UiSpinnerDouble uiSpinnerPrecTolPpm = UiSpinnerDouble.builder(20.0, 0.001, 1e6, 1.0)
//          .setFormat(new DecimalFormat("0.#")).setNumCols(5).create();
//      FormEntry fePrecTolPpm = new FormEntry(PROP_precursor_tol_ppm, "Precursor tolerance ppm", uiSpinnerPrecTolPpm);
//
//      pPeakPicking.add(fePrecTol.label(), new CC().alignX("right"));
//      pPeakPicking.add(fePrecTol.comp, new CC());
//      pPeakPicking.add(fePrecTolPpm.label(), new CC().alignX("right"));
//      pPeakPicking.add(fePrecTolPpm.comp, new CC().wrap());
//
//      UiSpinnerDouble uiSpinnerSpecPpmTol = UiSpinnerDouble.builder(20.0, 0.001, 1e6, 1.0)
//          .setFormat(new DecimalFormat("0.###")).setNumCols(5).create();
//      FormEntry feSpecPpmTol = new FormEntry(PROP_spectra_ppmtol, "Spectrum ppm tolerance", uiSpinnerSpecPpmTol);
//
//      UiSpinnerInt uiSpinnerSpecCondPeaks = new UiSpinnerInt(100, 0, 1000000, 20);
//      uiSpinnerSpecCondPeaks.setColumns(5);
//      FormEntry feSpecCondPeaks = new FormEntry(PROP_spectra_condPeaks, "spectra_condPeaks", uiSpinnerSpecCondPeaks);
//
//      UiSpinnerDouble uiSpinnerSpecCondRatio = UiSpinnerDouble.builder(0.01, 0.001, 1e6, 0.01)
//          .setFormat(new DecimalFormat("0.###")).setNumCols(5).create();
//      FormEntry feSpecCondRatio = new FormEntry(PROP_spectra_condRatio, "spectra_condRatio", uiSpinnerSpecCondRatio);
//
//      pPeakPicking.add(feSpecPpmTol.label(), new CC().alignX("right"));
//      pPeakPicking.add(feSpecPpmTol.comp, new CC());
//      pPeakPicking.add(feSpecCondPeaks.label(), new CC().alignX("right"));
//      pPeakPicking.add(feSpecCondPeaks.comp, new CC().wrap());
//      pPeakPicking.add(feSpecCondRatio.label(), new CC().alignX("right"));
//      pPeakPicking.add(feSpecCondRatio.comp, new CC().wrap());


      pContent.add(pPeakPicking, new CC().wrap().growX());
    }

    this.add(pContent, BorderLayout.CENTER);

//    {
//      pPrecursorSpectrum = new JPanel(new MigLayout(new LC()));
//      pPrecursorSpectrum.setBorder(new TitledBorder("Peak Matching"));
//    }
  }

  public boolean isRunShepherd() {
    return checkRun.isEnabled() && checkRun.isSelected();
  }

  private void clearBalloonTips() {
    for (BalloonTip balloonTip : balloonTips) {
      if (balloonTip != null) {
        try {
          balloonTip.closeBalloon();
        } catch (Exception ignore) {
        }
      }
    }
    balloonTips.clear();
  }

  public boolean validateForm() {

    Pattern reVarMods = Pattern.compile("[^\\s]+:-?\\d+(?:\\.\\d+)?(?:\\s*,\\s*[^\\s]+:-?\\d+(?:\\.\\d+)?)*");
    String text = uiTextVarMods.getNonGhostText().trim();
    boolean ok = true;
    if (!StringUtils.isNullOrWhitespace(text) && !reVarMods.matcher(text).matches()) {
      BalloonTip tip = new BalloonTip(uiTextVarMods,
          "<html>Does not match allowed format \"&lt;name&gt;:&lt;mass&gt;\"");
      tip.setVisible(true);
      balloonTips.add(tip);
      ok = false;
    }
    return ok;
  }
}
