package com.cortex.viewer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;

public class CockpitPanel extends VBox {

    // --- stato interno
    private final Map<Integer, CheckBox> layerCheckboxes = new HashMap<>();

    private final CheckBox showAllLayersCheck = new CheckBox("Show all layers");
    private final CheckBox showInSynCheck = new CheckBox("Show input synapses");
    private final CheckBox showOutSynCheck = new CheckBox("Show output synapses");
    private final CheckBox highlightSensorsCheck = new CheckBox("Highlight sensor paths");

    private Consumer<Set<Integer>> onLayerChange;
    private Consumer<Boolean> onShowInSynChange;
    private Consumer<Boolean> onShowOutSynChange;
    private Consumer<Boolean> onHighlightSensorsChange;

    public CockpitPanel(int numLayers) {
        setSpacing(10);
        setPadding(new Insets(10));
        setPrefWidth(220);
        setStyle("-fx-background-color: #202020;");

        Label title = new Label("Cockpit");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");

        styleCheck(showAllLayersCheck);
        styleCheck(showInSynCheck);
        styleCheck(showOutSynCheck);
        styleCheck(highlightSensorsCheck);

        showAllLayersCheck.setSelected(true);
        showInSynCheck.setSelected(true);
        showOutSynCheck.setSelected(true);
        highlightSensorsCheck.setSelected(false);

        // --- layer section
        VBox layerBox = new VBox(5);

        for (int i = 0; i < numLayers; i++) {
            CheckBox cb = new CheckBox("Layer " + i);
            styleCheck(cb);
            cb.setSelected(true);
            layerCheckboxes.put(i, cb);

            cb.selectedProperty().addListener((obs, oldVal, newVal) -> notifyLayerChange());
            layerBox.getChildren().add(cb);
        }

        showAllLayersCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            for (CheckBox cb : layerCheckboxes.values()) {
                cb.setSelected(newVal);
            }
            notifyLayerChange();
        });

        // --- synapse toggles
        showInSynCheck.selectedProperty().addListener((obs, o, n) -> {
            if (onShowInSynChange != null) onShowInSynChange.accept(n);
        });

        showOutSynCheck.selectedProperty().addListener((obs, o, n) -> {
            if (onShowOutSynChange != null) onShowOutSynChange.accept(n);
        });

        // --- sensor highlight
        highlightSensorsCheck.selectedProperty().addListener((obs, o, n) -> {
            if (onHighlightSensorsChange != null) onHighlightSensorsChange.accept(n);
        });

        getChildren().addAll(
                title,
                new Separator(),
                showAllLayersCheck,
                layerBox,
                new Separator(),
                showInSynCheck,
                showOutSynCheck,
                new Separator(),
                highlightSensorsCheck
        );
    }

	private void notifyLayerChange() {
		Set<Integer> selectedLayers = new HashSet<>();
		for ( Entry<Integer, CheckBox> e : layerCheckboxes.entrySet() ) {
			if (e.getValue().isSelected()) {
				selectedLayers.add(e.getKey());
			}
		}		
		onLayerChange.accept(selectedLayers);
		return;
	}

	private void styleCheck(CheckBox chk) {
		chk.setStyle("-fx-text-fill: white;");
	}

	public void setOnLayerChange(Consumer<Set<Integer>>  c) {
		this.onLayerChange = c;
	}

	public void setOnShowInSynChange(Consumer<Boolean> c) {
		this.onShowInSynChange = c;
	}

	public void setOnShowOutSynChange(Consumer<Boolean> c) {
		this.onShowOutSynChange = c;
	}

	public void setOnHighlightSensorsChange(Consumer<Boolean> c) {
		this.onHighlightSensorsChange = c;
	}

}
