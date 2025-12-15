package main.java.gui.panels.faculty;

import main.java.gui.components.JCard;
import main.java.gui.style.PastelTheme;
import main.java.models.Appointment;
import main.java.models.Faculty;
import main.java.models.OfficeHour;
import main.java.models.User;
import main.java.service.FacultyService;
import main.java.utils.DatabaseUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class FacultyConnectPanel extends JPanel {
    private final User studentUser;
    private JList<Faculty> facultyList;
    private DefaultListModel<Faculty> facultyListModel;
    private JPanel detailPanel;
    private JButton bookButton;

    public FacultyConnectPanel(User studentUser) {
        this.studentUser = studentUser;
        setLayout(new BorderLayout(20, 20));
        setBackground(PastelTheme.PASTEL_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Left: List of Faculty
        JCard leftCard = new JCard(new BorderLayout());
        leftCard.setPreferredSize(new Dimension(250, 0));

        JLabel listHeader = new JLabel("Faculty Directory");
        listHeader.setFont(PastelTheme.CARD_TITLE_FONT);
        listHeader.setBorder(new EmptyBorder(0, 0, 10, 0));
        leftCard.add(listHeader, BorderLayout.NORTH);

        facultyListModel = new DefaultListModel<>();
        facultyList = new JList<>(facultyListModel);
        facultyList.setFont(PastelTheme.BODY_FONT);
        facultyList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Faculty) {
                    Faculty f = (Faculty) value;
                    lbl.setText(f.getFirstName() + " " + f.getLastName());
                    lbl.setBorder(new EmptyBorder(5, 5, 5, 5));
                }
                return lbl;
            }
        });
        facultyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadFacultyDetails(facultyList.getSelectedValue());
            }
        });

        leftCard.add(new JScrollPane(facultyList), BorderLayout.CENTER);
        add(leftCard, BorderLayout.WEST);

        // Right: Details
        detailPanel = new JPanel(new BorderLayout(20, 20));
        detailPanel.setBackground(PastelTheme.PASTEL_BG);
        add(detailPanel, BorderLayout.CENTER);

        // Initial Load
        loadFacultyList();
    }

    private void loadFacultyList() {
        List<Faculty> all = FacultyService.getAllFaculty();
        for (Faculty f : all) {
            facultyListModel.addElement(f);
        }
    }

    private void loadFacultyDetails(Faculty f) {
        detailPanel.removeAll();
        if (f == null) {
            detailPanel.revalidate();
            detailPanel.repaint();
            return;
        }

        // Header Card (Status)
        JCard statusCard = new JCard(new BorderLayout());
        JPanel headerInfo = new JPanel(new GridLayout(2, 1));
        headerInfo.setOpaque(false);
        JLabel nameLbl = new JLabel(f.getFirstName() + " " + f.getLastName());
        // Use generic HEADER_FONT derivative instead of undefined method
        nameLbl.setFont(PastelTheme.HEADER_FONT.deriveFont(20f));

        FacultyService.CurrentStatus status = FacultyService.FacultyCurrentStatus(f.getFacultyId());

        JLabel statusText = new JLabel("Status: " + status.type);
        statusText.setForeground(PastelTheme.TEXT_SECONDARY);
        if (status.type == FacultyService.StatusType.TEACHING) {
            statusText.setForeground(PastelTheme.PASTEL_RED_DARK);
            statusText.setIcon(PastelTheme.getDotIcon(PastelTheme.PASTEL_RED_DARK));
        } else if (status.type == FacultyService.StatusType.IN_OFFICE) {
            statusText.setForeground(PastelTheme.PASTEL_GREEN_DARK);
            statusText.setIcon(PastelTheme.getDotIcon(PastelTheme.PASTEL_GREEN_DARK));
        } else {
            statusText.setForeground(PastelTheme.TEXT_SECONDARY);
            statusText.setIcon(PastelTheme.getDotIcon(Color.GRAY));
        }

        headerInfo.add(nameLbl);
        headerInfo.add(statusText);

        JLabel locLbl = new JLabel("Location: " + status.location);
        locLbl.setFont(PastelTheme.BODY_FONT);

        statusCard.add(headerInfo, BorderLayout.CENTER);
        statusCard.add(locLbl, BorderLayout.SOUTH);

        detailPanel.add(statusCard, BorderLayout.NORTH);

        // Office Hours Card
        JCard ohCard = new JCard(new BorderLayout());
        JLabel ohHeader = new JLabel("Office Hours & Booking");
        ohHeader.setFont(PastelTheme.CARD_TITLE_FONT);
        ohHeader.setBorder(new EmptyBorder(0, 0, 10, 0));

        String[] cols = { "Day", "Time", "Location" };
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        JTable table = new JTable(model);
        List<OfficeHour> hours = FacultyService.getOfficeHours(f.getFacultyId());
        for (OfficeHour oh : hours) {
            model.addRow(
                    new Object[] { oh.getDayOfWeek(), oh.getStartTime() + " - " + oh.getEndTime(), oh.getLocation() });
        }

        bookButton = new JButton("Book Selected Slot");
        PastelTheme.styleButtonPrimary(bookButton);
        bookButton.addActionListener(e -> bookSlot(f, table));

        ohCard.add(ohHeader, BorderLayout.NORTH);
        ohCard.add(new JScrollPane(table), BorderLayout.CENTER);
        ohCard.add(bookButton, BorderLayout.SOUTH);

        detailPanel.add(ohCard, BorderLayout.CENTER);

        detailPanel.revalidate();
        detailPanel.repaint();
    }

    private void bookSlot(Faculty f, JTable table) {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an office hour slot.");
            return;
        }

        String purpose = JOptionPane.showInputDialog(this, "Purpose of meeting?");
        if (purpose == null)
            return;

        // Lookup student code
        main.java.models.Student student = DatabaseUtil.getAllStudents().stream()
                .filter(s -> s.getUsername() != null && s.getUsername().equals(studentUser.getUsername()))
                .findFirst()
                .orElse(null);

        if (student == null) {
            JOptionPane.showMessageDialog(this,
                    "Could not find linked student profile for user: " + studentUser.getUsername());
            return;
        }

        Appointment apt = new Appointment();
        apt.setStudentId(student.getStudentId());
        apt.setFacultyId(f.getFacultyId());
        apt.setAppointmentDate(LocalDate.now().plusDays(1)); // Placeholder: ideally pick from UI
        apt.setStartTime(java.time.LocalTime.of(10, 0));
        apt.setEndTime(java.time.LocalTime.of(10, 30));
        apt.setPurpose(purpose);

        FacultyService.bookAppointment(apt);
        JOptionPane.showMessageDialog(this, "Request sent!");

    }
}
