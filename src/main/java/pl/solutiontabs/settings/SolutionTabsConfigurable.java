package pl.solutiontabs.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class SolutionTabsConfigurable implements Configurable {
    private final Project project;
    private JPanel panel;
    private JComboBox<GroupMode> groupMode;
    private JComboBox<FileOrder> fileOrder;
    private JComboBox<GroupOrder> groupOrder;
    private JBCheckBox fullPath;
    private JBCheckBox hideNativeTabs;
    private JBCheckBox singleClick;
    private JBCheckBox showIcons;
    private JBCheckBox showExtensions;
    private JBCheckBox showCloseButtons;
    private RulesModel rulesModel;

    public SolutionTabsConfigurable(Project project) { this.project = project; }
    @Override public @Nls String getDisplayName() { return "Vertical Tabs"; }

    @Override public @Nullable JComponent createComponent() {
        groupMode = new JComboBox<>(GroupMode.values());
        fileOrder = new JComboBox<>(FileOrder.values());
        groupOrder = new JComboBox<>(GroupOrder.values());
        fullPath = new JBCheckBox("Show full path in tooltip");
        hideNativeTabs = new JBCheckBox("Hide native editor tabs");
        singleClick = new JBCheckBox("Open files with a single click");
        showIcons = new JBCheckBox("Show file-type icons");
        showExtensions = new JBCheckBox("Show file extensions");
        showCloseButtons = new JBCheckBox("Show close buttons (×)");
        rulesModel = new RulesModel();
        JBTable table = new JBTable(rulesModel);
        table.setShowGrid(true);
        table.setPreferredScrollableViewportSize(new Dimension(700, 260));
        JComponent decorated = ToolbarDecorator.createDecorator(table)
                .setAddAction(button -> rulesModel.add())
                .setRemoveAction(button -> rulesModel.remove(table.getSelectedRow()))
                .setMoveUpAction(button -> rulesModel.move(table.getSelectedRow(), -1, table))
                .setMoveDownAction(button -> rulesModel.move(table.getSelectedRow(), 1, table))
                .createPanel();
        decorated.setBorder(JBUI.Borders.emptyTop(4));
        panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Group open files by:"), groupMode)
                .addLabeledComponent(new JBLabel("Order files:"), fileOrder)
                .addLabeledComponent(new JBLabel("Order groups:"), groupOrder)
                .addComponent(singleClick)
                .addComponent(showIcons)
                .addComponent(showExtensions)
                .addComponent(showCloseButtons)
                .addComponent(fullPath)
                .addComponent(hideNativeTabs)
                .addSeparator()
                .addLabeledComponentFillVertically("Rules (semicolon-separated globs):", decorated)
                .addComponentFillVertically(new JBLabel("Rules are evaluated top-to-bottom. Move them to set priority. A group-name match also overrides that group's color."), 0)
                .getPanel();
        reset();
        return panel;
    }

    @Override public boolean isModified() {
        SolutionTabsState state = SolutionTabsSettings.get(project).getState();
        return state.groupMode != groupMode.getSelectedItem()
                || state.fileOrder != fileOrder.getSelectedItem()
                || state.groupOrder != groupOrder.getSelectedItem()
                || state.showFullPathInTooltip != fullPath.isSelected()
                || state.hideNativeEditorTabs != hideNativeTabs.isSelected()
                || state.singleClickOpensFile != singleClick.isSelected()
                || state.showFileIcons != showIcons.isSelected()
                || state.showFileExtensions != showExtensions.isSelected()
                || state.showCloseButtons != showCloseButtons.isSelected()
                || !sameRules(state.rules, rulesModel.rules);
    }

    @Override public void apply() {
        SolutionTabsState next = new SolutionTabsState();
        next.groupMode = (GroupMode) groupMode.getSelectedItem();
        next.fileOrder = (FileOrder) fileOrder.getSelectedItem();
        next.groupOrder = (GroupOrder) groupOrder.getSelectedItem();
        next.showFullPathInTooltip = fullPath.isSelected();
        next.hideNativeEditorTabs = hideNativeTabs.isSelected();
        next.singleClickOpensFile = singleClick.isSelected();
        next.showFileIcons = showIcons.isSelected();
        next.showFileExtensions = showExtensions.isSelected();
        next.showCloseButtons = showCloseButtons.isSelected();
        next.rules = copyRules(rulesModel.rules);
        SolutionTabsSettings.get(project).replace(next);
    }

    @Override public void reset() {
        if (groupMode == null) return;
        SolutionTabsState state = SolutionTabsSettings.get(project).getState();
        groupMode.setSelectedItem(state.groupMode);
        fileOrder.setSelectedItem(state.fileOrder);
        groupOrder.setSelectedItem(state.groupOrder);
        fullPath.setSelected(state.showFullPathInTooltip);
        hideNativeTabs.setSelected(state.hideNativeEditorTabs);
        singleClick.setSelected(state.singleClickOpensFile);
        showIcons.setSelected(state.showFileIcons);
        showExtensions.setSelected(state.showFileExtensions);
        showCloseButtons.setSelected(state.showCloseButtons);
        rulesModel.setRules(copyRules(state.rules));
    }

    @Override public void disposeUIResources() {
        panel = null; groupMode = null; fileOrder = null; groupOrder = null; fullPath = null;
        hideNativeTabs = null; singleClick = null; showIcons = null; showExtensions = null;
        showCloseButtons = null; rulesModel = null;
    }

    private static List<TabRule> copyRules(List<TabRule> rules) { return rules.stream().map(TabRule::copy).toList(); }
    private static boolean sameRules(List<TabRule> a, List<TabRule> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            TabRule x = a.get(i), y = b.get(i);
            if (!x.name.equals(y.name) || !x.pattern.equals(y.pattern) || !x.color.equals(y.color) || !x.group.equals(y.group)) return false;
        }
        return true;
    }

    private static final class RulesModel extends AbstractTableModel {
        private final String[] columns = {"Name", "Pattern", "Color (#RRGGBB)", "Group"};
        private List<TabRule> rules = new ArrayList<>();
        @Override public int getRowCount() { return rules.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }
        @Override public boolean isCellEditable(int row, int column) { return true; }
        @Override public Object getValueAt(int row, int column) {
            TabRule rule = rules.get(row);
            return switch (column) { case 0 -> rule.name; case 1 -> rule.pattern; case 2 -> rule.color; default -> rule.group; };
        }
        @Override public void setValueAt(Object value, int row, int column) {
            TabRule rule = rules.get(row); String text = String.valueOf(value);
            switch (column) { case 0 -> rule.name = text; case 1 -> rule.pattern = text; case 2 -> rule.color = text; default -> rule.group = text; }
            fireTableCellUpdated(row, column);
        }
        void add() { rules.add(new TabRule("New rule", "*.ext", "#808080", "Group")); fireTableRowsInserted(rules.size() - 1, rules.size() - 1); }
        void remove(int row) { if (row >= 0) { rules.remove(row); fireTableRowsDeleted(row, row); } }
        void move(int row, int delta, JTable table) {
            int target = row + delta;
            if (row < 0 || target < 0 || target >= rules.size()) return;
            TabRule moved = rules.remove(row);
            rules.add(target, moved);
            fireTableDataChanged();
            table.setRowSelectionInterval(target, target);
        }
        void setRules(List<TabRule> value) { rules = new ArrayList<>(value); fireTableDataChanged(); }
    }
}
