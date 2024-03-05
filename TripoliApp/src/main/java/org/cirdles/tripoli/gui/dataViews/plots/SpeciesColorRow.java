package org.cirdles.tripoli.gui.dataViews.plots;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import static org.cirdles.tripoli.constants.TripoliConstants.DetectorPlotFlavor;

public class SpeciesColorRow extends HBox {

    private DetectorPlotFlavor plotFlavor;
    private Color color;
    private ColorSplotch colorSplotch;


    public SpeciesColorRow(DetectorPlotFlavor plotFlavor, Color color, int index) {
        this.plotFlavor = plotFlavor;
        this.color = color;
        this.colorSplotch = new ColorSplotch(" ", plotFlavor, color, index);
        this.colorSplotch.prefWidthProperty().bind(widthProperty().divide(2));
        Label plotFlavorLabel = new Label(String.format("%s Color",getPlotFlavor().getName()));
        plotFlavorLabel.prefWidthProperty().bind(widthProperty().divide(2));
        plotFlavorLabel.setFont(new Font("Consolas", 14));
        getChildren().add(plotFlavorLabel);
        getChildren().add(this.colorSplotch);
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
        colorSplotch.getBackground().getFills().clear();
        colorSplotch.getBackground().getFills().add(new BackgroundFill(this.color,CornerRadii.EMPTY,Insets.EMPTY));
    }

    public ColorSplotch getColorSplotch() {
        return colorSplotch;
    }

    public DetectorPlotFlavor getPlotFlavor() {
        return plotFlavor;
    }

    public void setPlotFlavor(DetectorPlotFlavor plotFlavor) {
        this.plotFlavor = plotFlavor;
    }
}
