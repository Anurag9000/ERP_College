package main.java.gui.components;

import main.java.gui.style.PastelTheme;

import javax.swing.*;
import java.awt.*;

/**
 * A standard card component for the Pastel UI.
 */
public class JCard extends JPanel {

    public JCard() {
        super(new BorderLayout());
        setup();
    }

    public JCard(LayoutManager layout) {
        super(layout);
        setup();
    }

    private void setup() {
        PastelTheme.styleCard(this);
    }
}
