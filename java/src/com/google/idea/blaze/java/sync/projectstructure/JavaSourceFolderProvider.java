/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.blaze.java.sync.projectstructure;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.idea.blaze.base.sync.SourceFolderProvider;
import com.google.idea.blaze.base.util.UrlUtil;
import com.google.idea.blaze.java.sync.model.BlazeContentEntry;
import com.google.idea.blaze.java.sync.model.BlazeJavaSyncData;
import com.google.idea.blaze.java.sync.model.BlazeSourceDirectory;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFileManager;
import java.io.File;
import javax.annotation.Nullable;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.java.JavaResourceRootType;
import org.jetbrains.jps.model.java.JavaSourceRootProperties;
import org.jetbrains.jps.model.module.JpsModuleSourceRoot;

/** Edits source folders in IntelliJ content entries */
public class JavaSourceFolderProvider implements SourceFolderProvider {

  private final ImmutableMap<File, BlazeContentEntry> blazeContentEntries;

  public JavaSourceFolderProvider(@Nullable BlazeJavaSyncData syncData) {
    this.blazeContentEntries = blazeContentEntries(syncData);
  }

  private static ImmutableMap<File, BlazeContentEntry> blazeContentEntries(
      @Nullable BlazeJavaSyncData syncData) {
    if (syncData == null) {
      return ImmutableMap.of();
    }
    ImmutableMap.Builder<File, BlazeContentEntry> builder = ImmutableMap.builder();
    for (BlazeContentEntry blazeContentEntry : syncData.importResult.contentEntries) {
      builder.put(blazeContentEntry.contentRoot, blazeContentEntry);
    }
    return builder.build();
  }

  @Override
  public ImmutableMap<File, SourceFolder> initializeSourceFolders(ContentEntry contentEntry) {
    ImmutableMap.Builder<File, SourceFolder> output = ImmutableMap.builder();
    BlazeContentEntry javaContentEntry =
        blazeContentEntries.get(UrlUtil.urlToFile(contentEntry.getUrl()));
    if (javaContentEntry != null) {
      for (BlazeSourceDirectory sourceDirectory : javaContentEntry.sources) {
        SourceFolder sourceFolder = addSourceFolderToContentEntry(contentEntry, sourceDirectory);
        output.put(UrlUtil.urlToFile(sourceFolder.getUrl()), sourceFolder);
      }
    }
    return output.build();
  }

  @Override
  public SourceFolder setSourceFolderForLocation(
      ContentEntry contentEntry, SourceFolder parentFolder, File file, boolean isTestSource) {
    SourceFolder sourceFolder;
    if (isResource(parentFolder)) {
      JavaResourceRootType resourceRootType =
          isTestSource ? JavaResourceRootType.TEST_RESOURCE : JavaResourceRootType.RESOURCE;
      sourceFolder =
          contentEntry.addSourceFolder(UrlUtil.pathToUrl(file.getPath()), resourceRootType);
    } else {
      sourceFolder = contentEntry.addSourceFolder(UrlUtil.pathToUrl(file.getPath()), isTestSource);
    }
    sourceFolder.setPackagePrefix(derivePackagePrefix(file, parentFolder));
    JpsModuleSourceRoot sourceRoot = sourceFolder.getJpsElement();
    JpsElement properties = sourceRoot.getProperties();
    if (properties instanceof JavaSourceRootProperties) {
      ((JavaSourceRootProperties) properties).setForGeneratedSources(isGenerated(parentFolder));
    }
    return sourceFolder;
  }

  private static String derivePackagePrefix(File file, SourceFolder parentFolder) {
    String parentPackagePrefix = parentFolder.getPackagePrefix();
    String parentPath = VirtualFileManager.extractPath(parentFolder.getUrl());
    String relativePath =
        FileUtil.toCanonicalPath(
            FileUtil.getRelativePath(parentPath, file.getPath(), File.separatorChar));
    if (Strings.isNullOrEmpty(relativePath)) {
      return parentPackagePrefix;
    }

    return parentPackagePrefix + "." + relativePath.replaceAll(File.separator, ".");
  }

  @VisibleForTesting
  static boolean isResource(SourceFolder folder) {
    return folder.getRootType() instanceof JavaResourceRootType;
  }

  @VisibleForTesting
  static boolean isGenerated(SourceFolder folder) {
    JpsElement properties = folder.getJpsElement().getProperties();
    return properties instanceof JavaSourceRootProperties
        && ((JavaSourceRootProperties) properties).isForGeneratedSources();
  }

  private static SourceFolder addSourceFolderToContentEntry(
      ContentEntry contentEntry, BlazeSourceDirectory sourceDirectory) {
    File sourceDir = sourceDirectory.getDirectory();

    // Create the source folder
    SourceFolder sourceFolder;
    if (sourceDirectory.getIsResource()) {
      sourceFolder =
          contentEntry.addSourceFolder(
              UrlUtil.pathToUrl(sourceDir.getPath()), JavaResourceRootType.RESOURCE);
    } else {
      sourceFolder = contentEntry.addSourceFolder(UrlUtil.pathToUrl(sourceDir.getPath()), false);
    }
    JpsModuleSourceRoot sourceRoot = sourceFolder.getJpsElement();
    JpsElement properties = sourceRoot.getProperties();
    if (properties instanceof JavaSourceRootProperties) {
      JavaSourceRootProperties rootProperties = (JavaSourceRootProperties) properties;
      if (sourceDirectory.getIsGenerated()) {
        rootProperties.setForGeneratedSources(true);
      }
    }
    String packagePrefix = sourceDirectory.getPackagePrefix();
    if (!Strings.isNullOrEmpty(packagePrefix)) {
      sourceFolder.setPackagePrefix(packagePrefix);
    }
    return sourceFolder;
  }
}
