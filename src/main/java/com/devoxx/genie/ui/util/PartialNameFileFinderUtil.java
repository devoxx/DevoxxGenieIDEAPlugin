package com.devoxx.genie.ui.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PartialNameFileFinderUtil {

    public List<VirtualFile> findAndProcessFilesByPartName(Project project, String filterText) {
        List<VirtualFile> filteredItems = new ArrayList<>();

        // Retrieve all filenames in the project
        // Use FilenameIndex to search for project files
        Collection<VirtualFile> projectFiles =
            FilenameIndex.getAllFilesByExt(project, "java", GlobalSearchScope.projectScope(project));

        // Include other project files in the list
        for (VirtualFile file : projectFiles) {
            if (file.getName().toLowerCase().contains(filterText) && !filteredItems.contains(file)) {
                filteredItems.add(file);
            }
        }
        return filteredItems;
    }
}
