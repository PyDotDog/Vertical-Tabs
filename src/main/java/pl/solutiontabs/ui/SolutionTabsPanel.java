package pl.solutiontabs.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import pl.solutiontabs.rules.RuleEngine;
import pl.solutiontabs.settings.SolutionTabsSettings;
import pl.solutiontabs.settings.SolutionTabsState;
import pl.solutiontabs.settings.FileOrder;
import pl.solutiontabs.settings.GroupOrder;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class SolutionTabsPanel extends JPanel implements Disposable {
    private final Project project;
    private final Tree tree = new Tree();
    private final AtomicLong sequence = new AtomicLong();
    private final Map<String, Long> openedOrder = new HashMap<>();
    private final Map<String, Long> usedOrder = new HashMap<>();
    private final Map<String, String> groupByFileUrl = new HashMap<>();
    private final Map<String, Color> colorByGroup = new HashMap<>();
    private SolutionTabsState currentState;
    private boolean refreshScheduled;

    public SolutionTabsPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        this.currentState = SolutionTabsSettings.get(project).getState();
        setBorder(JBUI.Borders.empty());
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new Renderer());
        for (VirtualFile file : FileEditorManager.getInstance(project).getOpenFiles()) markOpened(file);
        tree.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event) && isCloseClick(event)) { closeSelected(); return; }
                int requiredClicks = currentState.singleClickOpensFile ? 1 : 2;
                if (event.getClickCount() == requiredClicks && SwingUtilities.isLeftMouseButton(event)) openSelected();
                if (SwingUtilities.isMiddleMouseButton(event)) closeSelected();
            }
            @Override public void mousePressed(MouseEvent event) { showPopup(event); }
            @Override public void mouseReleased(MouseEvent event) { showPopup(event); }
        });
        add(new JBScrollPane(tree), BorderLayout.CENTER);

        project.getMessageBus().connect(this).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
            @Override public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) { markOpened(file); scheduleRefresh(); }
            @Override public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) { scheduleRefresh(); }
            @Override public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                if (event.getNewFile() != null) usedOrder.put(event.getNewFile().getUrl(), sequence.incrementAndGet());
                if (currentState.fileOrder == FileOrder.RECENTLY_USED
                        || currentState.groupOrder == GroupOrder.RECENTLY_USED) scheduleRefresh();
                else refreshSelection();
            }
        });
        project.getMessageBus().connect(this).subscribe(SolutionTabsSettings.TOPIC, this::refresh);
        refresh();
    }

    private void refresh() {
        currentState = SolutionTabsSettings.get(project).getState();
        SolutionTabsState state = currentState;
        groupByFileUrl.clear();
        colorByGroup.clear();
        Map<String, List<VirtualFile>> groupedFiles = new HashMap<>();
        VirtualFile[] files = FileEditorManager.getInstance(project).getOpenFiles();
        for (VirtualFile file : files) markOpened(file);
        Comparator<VirtualFile> fileComparator = fileComparator(state);
        Arrays.sort(files, fileComparator);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        for (VirtualFile file : files) {
            String name = RuleEngine.group(project, file, state);
            groupByFileUrl.put(file.getUrl(), name);
            colorByGroup.computeIfAbsent(name, ignored -> RuleEngine.groupColor(name, state));
            groupedFiles.computeIfAbsent(name, ignored -> new ArrayList<>()).add(file);
        }
        List<String> groupNames = new ArrayList<>(groupedFiles.keySet());
        groupNames.sort(groupComparator(state, groupedFiles));
        for (String groupName : groupNames) {
            DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(groupName);
            groupedFiles.get(groupName).stream().sorted(fileComparator)
                    .forEach(file -> groupNode.add(new DefaultMutableTreeNode(file)));
            root.add(groupNode);
        }
        tree.setModel(new DefaultTreeModel(root));
        for (int row = 0; row < tree.getRowCount(); row++) tree.expandRow(row);
        refreshSelection();
    }

    private void scheduleRefresh() {
        if (refreshScheduled) return;
        refreshScheduled = true;
        SwingUtilities.invokeLater(() -> {
            refreshScheduled = false;
            if (!project.isDisposed()) refresh();
        });
    }

    private void refreshSelection() {
        VirtualFile selected = FileEditorManager.getInstance(project).getSelectedFiles().length == 0
                ? null : FileEditorManager.getInstance(project).getSelectedFiles()[0];
        if (selected == null) return;
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        var nodes = root.depthFirstEnumeration();
        while (nodes.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
            if (selected.equals(node.getUserObject())) {
                tree.setSelectionPath(new TreePath(node.getPath()));
                tree.scrollPathToVisible(new TreePath(node.getPath()));
                return;
            }
        }
    }

    private VirtualFile selectedFile() {
        TreePath path = tree.getSelectionPath();
        if (path == null) return null;
        Object value = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
        return value instanceof VirtualFile file ? file : null;
    }

    private void openSelected() {
        VirtualFile file = selectedFile();
        if (file != null) {
            usedOrder.put(file.getUrl(), sequence.incrementAndGet());
            FileEditorManager.getInstance(project).openFile(file, true);
        }
    }

    private void markOpened(VirtualFile file) {
        openedOrder.computeIfAbsent(file.getUrl(), ignored -> sequence.incrementAndGet());
        usedOrder.putIfAbsent(file.getUrl(), openedOrder.get(file.getUrl()));
    }

    private Comparator<VirtualFile> fileComparator(SolutionTabsState state) {
        Comparator<VirtualFile> alphabetical = Comparator.comparing(VirtualFile::getName, String.CASE_INSENSITIVE_ORDER);
        return switch (state.fileOrder) {
            case ALPHABETICAL -> alphabetical;
            case RECENTLY_USED -> Comparator.<VirtualFile, Long>comparing(f -> usedOrder.getOrDefault(f.getUrl(), 0L)).reversed().thenComparing(alphabetical);
            case OPENED_NEWEST -> Comparator.<VirtualFile, Long>comparing(f -> openedOrder.getOrDefault(f.getUrl(), 0L)).reversed().thenComparing(alphabetical);
            case OPENED_OLDEST -> Comparator.<VirtualFile, Long>comparing(f -> openedOrder.getOrDefault(f.getUrl(), 0L)).thenComparing(alphabetical);
        };
    }

    private Comparator<String> groupComparator(SolutionTabsState state, Map<String, List<VirtualFile>> groups) {
        Comparator<String> alphabetical = String.CASE_INSENSITIVE_ORDER;
        return switch (state.groupOrder) {
            case ALPHABETICAL -> alphabetical;
            case RECENTLY_USED -> Comparator.<String, Long>comparing(name -> groups.get(name).stream()
                    .mapToLong(file -> usedOrder.getOrDefault(file.getUrl(), 0L)).max().orElse(0L)).reversed().thenComparing(alphabetical);
            case RULE_PRIORITY -> Comparator.<String, Integer>comparing(name -> rulePriority(name, state)).thenComparing(alphabetical);
        };
    }

    private int rulePriority(String groupName, SolutionTabsState state) {
        for (int i = 0; i < state.rules.size(); i++) {
            if (groupName.equalsIgnoreCase(state.rules.get(i).group)) return i;
        }
        return Integer.MAX_VALUE;
    }

    private void closeSelected() {
        VirtualFile file = selectedFile();
        if (file != null) FileEditorManager.getInstance(project).closeFile(file);
    }

    private boolean isCloseClick(MouseEvent event) {
        TreePath path = tree.getPathForLocation(event.getX(), event.getY());
        if (path == null) return false;
        tree.setSelectionPath(path);
        Object value = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
        Rectangle bounds = tree.getPathBounds(path);
        return value instanceof VirtualFile && currentState.showCloseButtons
                && bounds != null && event.getX() >= bounds.x + bounds.width - 22;
    }

    private void showPopup(MouseEvent event) {
        if (!event.isPopupTrigger()) return;
        TreePath path = tree.getPathForLocation(event.getX(), event.getY());
        if (path == null) return;
        tree.setSelectionPath(path);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        JPopupMenu menu = new JPopupMenu();
        if (node.getUserObject() instanceof VirtualFile) {
            JMenuItem close = new JMenuItem("Close");
            close.addActionListener(ignored -> closeSelected());
            menu.add(close);
            JMenuItem closeOthers = new JMenuItem("Close Others");
            closeOthers.addActionListener(ignored -> closeOthers());
            menu.add(closeOthers);
        } else if (node.getParent() != null) {
            JMenuItem closeGroup = new JMenuItem("Close Group");
            closeGroup.addActionListener(ignored -> closeNodeFiles(node));
            menu.add(closeGroup);
        }
        JMenuItem closeAll = new JMenuItem("Close All");
        closeAll.addActionListener(ignored -> closeAllFiles());
        menu.add(closeAll);
        menu.show(tree, event.getX(), event.getY());
    }

    private void closeOthers() {
        VirtualFile keep = selectedFile();
        if (keep == null) return;
        for (VirtualFile file : FileEditorManager.getInstance(project).getOpenFiles()) {
            if (!file.equals(keep)) FileEditorManager.getInstance(project).closeFile(file);
        }
    }

    private void closeAllFiles() {
        for (VirtualFile file : FileEditorManager.getInstance(project).getOpenFiles()) {
            FileEditorManager.getInstance(project).closeFile(file);
        }
    }

    private void closeNodeFiles(DefaultMutableTreeNode node) {
        var children = node.depthFirstEnumeration();
        while (children.hasMoreElements()) {
            Object value = ((DefaultMutableTreeNode) children.nextElement()).getUserObject();
            if (value instanceof VirtualFile file) FileEditorManager.getInstance(project).closeFile(file);
        }
    }

    @Override public void dispose() {}

    private final class Renderer extends ColoredTreeCellRenderer {
        @Override public void customizeCellRenderer(@NotNull JTree ignored, Object value, boolean selected,
                                                     boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Object object = ((DefaultMutableTreeNode) value).getUserObject();
            if (object instanceof VirtualFile file) {
                String groupName = groupByFileUrl.get(file.getUrl());
                Color color = groupName == null ? null : colorByGroup.get(groupName);
                if (currentState.showFileIcons) setIcon(file.getFileType().getIcon());
                SimpleTextAttributes fileAttributes = color == null
                        ? SimpleTextAttributes.REGULAR_ATTRIBUTES
                        : new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, color);
                String displayName = currentState.showFileExtensions
                        ? file.getName() : file.getNameWithoutExtension();
                append(displayName, fileAttributes);
                if (currentState.showCloseButtons) {
                    append("   ×", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                }
                if (currentState.showFullPathInTooltip) setToolTipText(file.getPath());
            } else if (object != null) {
                Color color = colorByGroup.computeIfAbsent(object.toString(), group -> RuleEngine.groupColor(group, currentState));
                append(object.toString(), new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, color));
            }
        }

    }

}
