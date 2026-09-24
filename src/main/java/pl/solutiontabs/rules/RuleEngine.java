package pl.solutiontabs.rules;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import pl.solutiontabs.settings.GroupMode;
import pl.solutiontabs.settings.SolutionTabsState;
import pl.solutiontabs.settings.TabRule;

import java.awt.Color;
import java.util.Locale;
import java.util.Optional;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class RuleEngine {
    private static final ConcurrentHashMap<String, Pattern> GLOB_PATTERNS = new ConcurrentHashMap<>();

    private RuleEngine() {}

    public static Optional<TabRule> match(VirtualFile file, SolutionTabsState state) {
        String path = file.getPath().replace('\\', '/');
        for (TabRule rule : state.rules) {
            for (String raw : rule.pattern.split(";")) {
                String glob = raw.trim();
                if (!glob.isEmpty() && GLOB_PATTERNS.computeIfAbsent(glob,
                        value -> Pattern.compile(globToRegex(value), Pattern.CASE_INSENSITIVE)).matcher(path).matches()) {
                    return Optional.of(rule);
                }
            }
        }
        return Optional.empty();
    }

    public static Color color(VirtualFile file, SolutionTabsState state) {
        return match(file, state).map(rule -> parseColor(rule.color)).orElse(null);
    }

    public static Color groupColor(String groupName, SolutionTabsState state) {
        for (TabRule rule : state.rules) {
            if (rule.group != null && rule.group.equalsIgnoreCase(groupName)) {
                Color configured = parseColor(rule.color);
                if (configured != null) return configured;
            }
        }
        // Stable palette: a group keeps the same color between IDE sessions.
        Color[] palette = {
                new Color(0x4E9AEB), new Color(0xE06C75), new Color(0x64B587),
                new Color(0xD19A66), new Color(0xB07CC6), new Color(0x56B6C2),
                new Color(0xC7A84B), new Color(0xDB7093)
        };
        return palette[Math.floorMod(groupName.toLowerCase(Locale.ROOT).hashCode(), palette.length)];
    }

    public static String group(Project project, VirtualFile file, SolutionTabsState state) {
        if (state.groupMode == GroupMode.CUSTOM_RULES) {
            return match(file, state).map(r -> blankTo(r.group, "Other")).orElse("Other");
        }
        if (state.groupMode == GroupMode.EXTENSION) {
            String ext = file.getExtension();
            return ext == null ? "No extension" : "." + ext.toLowerCase(Locale.ROOT);
        }
        if (state.groupMode == GroupMode.DIRECTORY) {
            return file.getParent() == null ? "Other" : file.getParent().getName();
        }
        String dotNetProject = findDotNetProject(file);
        if (dotNetProject != null) return dotNetProject;
        Module module = ModuleUtilCore.findModuleForFile(file, project);
        if (module != null && !module.getName().equalsIgnoreCase("rider.module")) return module.getName();
        VirtualFile contentRoot = ProjectFileIndex.getInstance(project).getContentRootForFile(file);
        if (contentRoot != null) return contentRoot.getName();
        return solutionName(project, file);
    }

    /**
     * Rider exposes most .NET files through a technical IntelliJ module named
     * "rider.module". Visual Studio groups documents by the owning MSBuild
     * project instead, so walk towards the solution root and use the nearest
     * project file as the group identity.
     */
    static String findDotNetProject(VirtualFile file) {
        if (isProjectFile(file)) return file.getNameWithoutExtension();
        VirtualFile directory = file.isDirectory() ? file : file.getParent();
        while (directory != null) {
            VirtualFile[] projectFiles = Arrays.stream(directory.getChildren())
                    .filter(RuleEngine::isProjectFile)
                    .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                    .toArray(VirtualFile[]::new);
            if (projectFiles.length > 0) return projectFiles[0].getNameWithoutExtension();
            if (containsSolutionFile(directory)) break;
            directory = directory.getParent();
        }
        return null;
    }

    private static String solutionName(Project project, VirtualFile file) {
        VirtualFile directory = file.isDirectory() ? file : file.getParent();
        while (directory != null) {
            Optional<VirtualFile> solution = Arrays.stream(directory.getChildren())
                    .filter(RuleEngine::isSolutionFile)
                    .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                    .findFirst();
            if (solution.isPresent()) return "Solution: " + solution.get().getNameWithoutExtension();
            directory = directory.getParent();
        }
        return "Project: " + project.getName();
    }

    private static boolean containsSolutionFile(VirtualFile directory) {
        return Arrays.stream(directory.getChildren()).anyMatch(RuleEngine::isSolutionFile);
    }

    private static boolean isProjectFile(VirtualFile file) {
        String ext = file.getExtension();
        return ext != null && (ext.equalsIgnoreCase("csproj")
                || ext.equalsIgnoreCase("fsproj") || ext.equalsIgnoreCase("vbproj"));
    }

    private static boolean isSolutionFile(VirtualFile file) {
        String ext = file.getExtension();
        return ext != null && (ext.equalsIgnoreCase("sln") || ext.equalsIgnoreCase("slnx"));
    }

    public static Color parseColor(String text) {
        if (text == null) return null;
        try { return Color.decode(text.trim()); } catch (NumberFormatException ignored) { return null; }
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String globToRegex(String glob) {
        StringBuilder out = new StringBuilder(".*");
        for (char c : glob.toCharArray()) {
            switch (c) {
                case '*' -> out.append(".*");
                case '?' -> out.append('.');
                case '.', '(', ')', '+', '|', '^', '$', '@', '%' -> out.append('\\').append(c);
                case '\\' -> out.append('/');
                default -> out.append(c);
            }
        }
        return out.append('$').toString();
    }
}
