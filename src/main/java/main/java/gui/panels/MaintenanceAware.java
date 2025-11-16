package main.java.gui.panels;

/**
 * Panels that need to react to maintenance mode should implement this.
 */
public interface MaintenanceAware {
    /**
     * Invoked when maintenance mode is toggled.
     *
     * @param maintenance true if maintenance mode is enabled.
     */
    void onMaintenanceModeChanged(boolean maintenance);
}
